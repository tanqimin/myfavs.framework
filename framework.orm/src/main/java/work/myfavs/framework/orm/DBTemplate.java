package work.myfavs.framework.orm;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.sql.DataSource;
import work.myfavs.framework.orm.meta.handler.PropertyHandler;
import work.myfavs.framework.orm.meta.handler.PropertyHandlerFactory;
import work.myfavs.framework.orm.util.SqlLog;
import work.myfavs.framework.orm.util.exception.DBException;


public class DBTemplate {

  private final static Map<String, DBTemplate> POOL = new ConcurrentHashMap<>();

  public static DBTemplate get(String dsName) {
    if (POOL.containsKey(dsName)) {
      return POOL.get(dsName);
    }

    throw new DBException("The DataSource named {} not exists.", dsName);
  }

  //region Attributes
  /**
   * 数据源
   */
  private DataSource dataSource;
  /**
   * 数据库配置
   */
  private DBConfig dbConfig;
  /**
   * 数据库连接工厂
   */
  private ConnFactory connectionFactory;
  /**
   * SQL语句日志配置
   */
  private SqlLog sqlLog;
  //endregion

  //region Constructor

  /**
   * 构造方法
   *
   * @param builder Builder
   */
  private DBTemplate(Builder builder) {

    this.dataSource = builder.dataSource;
    this.dbConfig = builder.config;
    this.connectionFactory = createConnFactory(builder.connectionFactory, builder.dataSource);
    this.sqlLog = new SqlLog(this.dbConfig.getShowSql(), this.dbConfig.getShowResult());
    //注册 PropertyHandler
    registerMapper(builder.mapper);
  }

  /**
   * 注册 PropertyHandler
   *
   * @param mapper
   */
  private void registerMapper(Mapper mapper) {
    if (mapper == null || mapper.map.isEmpty()) {
      PropertyHandlerFactory.registerDefault();
      return;
    }
    for (Entry<Class<?>, PropertyHandler> entry : mapper.map.entrySet()) {
      PropertyHandlerFactory.register(entry.getKey(), entry.getValue());
    }
  }
  //endregion

  /**
   * 获取数据源
   *
   * @return 数据源
   */
  public DataSource getDataSource() {

    return dataSource;
  }

  /**
   * 获取连接工厂类
   *
   * @return 连接工厂类
   */
  public ConnFactory getConnectionFactory() {

    return connectionFactory;
  }

  /**
   * 获取配置
   *
   * @return 配置
   */
  public DBConfig getDbConfig() {

    return dbConfig;
  }

  public SqlLog getSqlLog() {
    return sqlLog;
  }

  /**
   * 获取数据库连接工厂
   *
   * @param cls        数据库连接工厂类型
   * @param dataSource 数据源
   * @return 数据库连接工厂
   */
  private ConnFactory createConnFactory(Class<? extends ConnFactory> cls,
      DataSource dataSource) {

    try {
      final Constructor<? extends ConnFactory> constructor = cls.getConstructor(DataSource.class);
      return constructor.newInstance(dataSource);
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new DBException(e, "Fail to create ConnectionFactory instance, error message:");
    }
  }

  public static class Builder {

    private String dsName;
    private DataSource dataSource;
    private DBConfig config;
    public Mapper mapper = new Mapper();

    public Builder() {
      this(DBConfig.DEFAULT_DATASOURCE_NAME);
    }

    public Builder(String dsName) {
      this.dsName = dsName;
    }

    private Class<? extends ConnFactory> connectionFactory = JdbcConnFactory.class;

    public Builder dataSource(DataSource dataSource) {

      this.dataSource = dataSource;
      return this;
    }

    public Builder config(Consumer<DBConfig> consumer) {

      config = new DBConfig();
      consumer.accept(config);
      return this;
    }

    public Builder connectionFactory(Class<? extends ConnFactory> connectionFactory) {

      this.connectionFactory = connectionFactory;
      return this;
    }

    public Builder mapping(Consumer<Mapper> consumer) {

      consumer.accept(mapper);
      return this;
    }

    public DBTemplate build() {

      if (this.dataSource == null) {
        throw new DBException("Please set a dataSource.");
      }

      if (this.config == null) {
        this.config = new DBConfig();
      }

      return DBTemplate.POOL.computeIfAbsent(dsName, ds -> {
        return new DBTemplate(this);
      });
    }

  }

  public static class Mapper {

    private Map<Class<?>, PropertyHandler> map;

    private Mapper() {

      map = new HashMap<>();
    }

    public Mapper register(Class<?> clazz,
        PropertyHandler propertyHandler) {

      map.put(clazz, propertyHandler);
      return this;
    }

  }

}
