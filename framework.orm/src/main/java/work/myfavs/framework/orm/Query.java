package work.myfavs.framework.orm;

import work.myfavs.framework.orm.meta.BatchParameters;
import work.myfavs.framework.orm.util.convert.DBConvert;
import work.myfavs.framework.orm.util.exception.DBException;
import work.myfavs.framework.orm.util.func.ThrowingConsumer;

import java.io.Closeable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * 查询封装
 */
public class Query implements Closeable {
  private final Database database;
  private final int      batchSize;
  private final int      fetchSize;

  private PreparedStatement preparedStatement;
  private String            sql;
  private boolean           autoGeneratedPK = false;

  //批量查询参数
  private final BatchParameters batchParameters = new BatchParameters();

  /**
   * 构造方法，推荐使用 {@link Database#createQuery(String, boolean)} 创建示例
   *
   * @param database        {@link Database}
   * @param sql             SQL语句
   * @param autoGeneratedPK 是否自动生成主键
   */
  public Query(Database database, String sql, boolean autoGeneratedPK) {
    this.database = database;
    this.batchSize = database.getDbConfig().getBatchSize();
    this.fetchSize = database.getDbConfig().getFetchSize();
    initQuery(sql, autoGeneratedPK);
  }

  private void initQuery(String sql, boolean autoGeneratedPK) {
    this.sql = sql;
    this.autoGeneratedPK = autoGeneratedPK;
    this.batchParameters.clear();
    this.closePreparedStatement();
  }

  /**
   * 创建一个新的 Query 对象(默认不自动生成主键)
   *
   * @param sql SQL语句
   * @return {@link Query}
   */
  public Query newQuery(String sql) {
    return newQuery(sql, false);
  }

  /**
   * 创建一个新的 Query 对象
   *
   * @param sql             SQL语句
   * @param autoGeneratedPK 是否自动生成主键
   * @return {@link Query}
   */
  public Query newQuery(String sql, boolean autoGeneratedPK) {
    initQuery(sql, autoGeneratedPK);
    return this;
  }

  /**
   * 创建 PreparedStatement
   *
   * @return {@link PreparedStatement}
   */
  private PreparedStatement createPreparedStatement() {
    try {
      if (this.preparedStatement == null) {
        if (this.autoGeneratedPK)
          this.preparedStatement = this.database.getConnection().prepareStatement(this.sql, Statement.RETURN_GENERATED_KEYS);
        else
          this.preparedStatement = this.database.getConnection().prepareStatement(this.sql);
      }
    } catch (SQLException e) {
      throw new DBException(e, "Error preparing statement: {}", e.getMessage());
    }
    return this.preparedStatement;
  }

  /**
   * 批量增加参数
   *
   * @param params 参数集合
   * @return {@link Database}
   */
  public Query addParameters(Collection<?> params) {
    this.batchParameters.getCurrentBatchParameters().addParameters(params);
    return this;
  }

  /**
   * 增加参数
   *
   * @param paramIndex 参数序号，从 1 开始
   * @param param      参数
   * @return {@link Database}
   */
  public Query addParameter(int paramIndex, Object param) {
    this.batchParameters.getCurrentBatchParameters().addParameter(paramIndex, param);
    return this;
  }

  public <TModel> List<TModel> find(Class<TModel> modelClass) {
    PreparedStatement preparedStatement = createPreparedStatement();
    setFetchSize(preparedStatement);
    this.batchParameters.applyParameters(preparedStatement);
    try (ResultSet resultSet = preparedStatement.executeQuery()) {
      return DBConvert.toList(modelClass, resultSet);
    } catch (SQLException ex) {
      throw new DBException(ex, "Error execute query: {}", ex.getMessage());
    } finally {
      this.batchParameters.clear();
    }
  }

  public <TModel> TModel get(Class<TModel> modelClass) {
    Iterator<TModel> iterator = find(modelClass).iterator();
    return iterator.hasNext() ? iterator.next() : null;
  }

  private void setFetchSize(PreparedStatement preparedStatement) {
    try {
      if (this.fetchSize > 0)
        preparedStatement.setFetchSize(this.fetchSize);
    } catch (SQLException ex) {
      throw new DBException(ex, "Error set fetch size: {}", ex.getMessage());
    }
  }

  public int execute(ThrowingConsumer<PreparedStatement, SQLException> configConsumer,
                     ThrowingConsumer<ResultSet, SQLException> keyConsumer) {
    PreparedStatement preparedStatement = createPreparedStatement();
    try {
      if (Objects.nonNull(configConsumer))
        configConsumer.accept(preparedStatement);
      this.batchParameters.applyParameters(preparedStatement);
      int result = preparedStatement.executeUpdate();
      if (Objects.nonNull(keyConsumer)) {
        if (this.autoGeneratedPK) {
          try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
            keyConsumer.accept(resultSet);
          }
        }
      }
      return result;
    } catch (SQLException e) {
      throw new DBException(e, "Error execute update: {}", e.getMessage());
    } finally {
      this.batchParameters.clear();
    }
  }

  public int execute() {
    return this.execute(null, null);
  }

  /**
   * 调用 {@link PreparedStatement#addBatch()} 并提交执行
   */
  public void addBatch() {
    this.batchParameters.addBatch();
  }

  public int[] executeBatch(ThrowingConsumer<ResultSet, SQLException> consumer) {
    PreparedStatement preparedStatement = createPreparedStatement();
    try {
      this.batchParameters.applyBatchParameters(preparedStatement, this.batchSize);
      int[] result = preparedStatement.executeBatch();
      if (Objects.nonNull(consumer)) {
        if (this.autoGeneratedPK) {
          try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
            consumer.accept(resultSet);
          }
        }
      }
      return result;
    } catch (SQLException e) {
      throw new DBException(e, "Error execute batch: {}", e.getMessage());
    } finally {
      this.batchParameters.clear();
    }
  }

  public int[] executeBatch() {
    return executeBatch(null);
  }

  @Override
  public void close() {
    closePreparedStatement();
  }

  private void closePreparedStatement() {
    if (this.preparedStatement != null) {
      try {
        if (!this.preparedStatement.isClosed())
          this.preparedStatement.close();
      } catch (SQLException e) {
        throw new DBException(e, "Error close preparedStatement: {}", e.getMessage());
      } finally {
        this.preparedStatement = null;
      }
    }
  }
}
