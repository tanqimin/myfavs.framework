package work.myfavs.framework.orm.meta.dialect.impl;

import work.myfavs.framework.orm.DBConfig;
import work.myfavs.framework.orm.meta.DbType;

/**
 * @author tanqimin
 */
public class PostgreSQLDialect extends MySqlDialect {

  public PostgreSQLDialect(DBConfig dbConfig) {
    super(dbConfig);
  }

  @Override
  public String dbType() {

    return DbType.POSTGRE_SQL;
  }
}
