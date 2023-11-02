package work.myfavs.framework.orm;

import java.sql.*;
import java.util.Collection;
import java.util.List;
import work.myfavs.framework.orm.util.func.ThrowingConsumer;
import work.myfavs.framework.orm.util.func.ThrowingFunction;
import work.myfavs.framework.orm.util.func.ThrowingRunnable;
import work.myfavs.framework.orm.util.func.ThrowingSupplier;

/** 数据库操作接口 */
public interface IDatabase {
/*  *//**
   * 打开数据库连接
   *
   * @return 数据库连接
   *//*
  Connection open();

  *//** 关闭数据库连接 *//*
  void close();*/

  /** 提交事务 */
  void commit();

  /**
   * 设置Savepoint
   *
   * @return Savepoint
   */
  Savepoint setSavepoint();

  /**
   * 设置Savepoint
   *
   * @param name Savepoint名字
   * @return Savepoint
   */
  Savepoint setSavepoint(String name);

  /** 回滚全部事务 */
  void rollback();

  /**
   * 回滚事务到指定Savepoint
   *
   * @param savepoint Savepoint
   */
  void rollback(Savepoint savepoint);

  /**
   * 执行事务(带返回值)
   *
   * @param func ThrowingFunction
   * @return TResult
   * @param <TResult> 返回值类型
   */
  <TResult> TResult tx(ThrowingFunction<Connection, TResult, SQLException> func);

  /**
   * 执行事务(带返回值)
   *
   * @param supplier ThrowingSupplier
   * @return TResult
   * @param <TResult> 返回值类型
   */
  <TResult> TResult tx(ThrowingSupplier<TResult, SQLException> supplier);

  /**
   * 执行事务(无返回值)
   *
   * @param consumer ThrowingConsumer
   */
  void tx(ThrowingConsumer<Connection, SQLException> consumer);

  /**
   * 执行事务(无返回值)
   *
   * @param runnable ThrowingRunnable
   */
  void tx(ThrowingRunnable<SQLException> runnable);

  /**
   * 执行SQL，返回多行记录
   *
   * @param viewClass 结果集类型
   * @param sql SQL语句
   * @param params 参数
   * @param <TView> 结果集类型泛型
   * @return 结果集
   */
  <TView> List<TView> find(Class<TView> viewClass, String sql, Collection<?> params);

  /**
   * 执行一个SQL语句
   *
   * @param sql SQL语句
   * @param params 参数
   * @param queryTimeOut 超时时间
   * @return 影响行数
   */
  int execute(String sql, Collection<?> params, int queryTimeOut);

  int create(
      String sql,
      Collection<?> params,
      boolean autoGeneratedPK,
      ThrowingConsumer<ResultSet, SQLException> pkConsumer);

  int createBatch(
      String sql,
      Collection<Collection<?>> paramsList,
      boolean autoGeneratedPK,
      ThrowingConsumer<ResultSet, SQLException> consumer);

  int updateBatch(String sql, Collection<Collection<?>> paramsList);
}
