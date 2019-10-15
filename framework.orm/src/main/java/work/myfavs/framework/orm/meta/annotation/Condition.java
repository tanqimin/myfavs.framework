package work.myfavs.framework.orm.meta.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import work.myfavs.framework.orm.meta.enumeration.Operator;

/**
 * 用于构造查询条件
 */
@java.lang.annotation.Inherited
@java.lang.annotation.Target({ElementType.FIELD})
@java.lang.annotation.Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Documented
public @interface Condition {

  /**
   * 数据库条件参数名称
   *
   * @return 数据库条件参数名称
   */
  String value() default "";

  /**
   * 条件运算符
   *
   * @return 条件运算符，默认为EQUALS
   */
  Operator operator() default Operator.EQUALS;

  /**
   * 条件顺序
   *
   * @return 条件顺序
   */
  int order() default 1;

}
