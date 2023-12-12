package work.myfavs.framework.orm.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.myfavs.framework.orm.DBTemplate;
import work.myfavs.framework.orm.meta.BatchParameters;
import work.myfavs.framework.orm.meta.Parameters;
import work.myfavs.framework.orm.meta.Record;
import work.myfavs.framework.orm.meta.schema.Attribute;
import work.myfavs.framework.orm.meta.schema.ClassMeta;
import work.myfavs.framework.orm.meta.schema.Metadata;
import work.myfavs.framework.orm.util.common.Constant;

import java.util.*;

public class SqlLog {

  private static final Logger log = LoggerFactory.getLogger(SqlLog.class);

  private static final int TITLE_LENGTH = 55;

  private final boolean showSql;
  private final boolean showResult;

  public SqlLog(boolean showSql, boolean showResult) {
    this.showSql = showSql;
    this.showResult = showResult;
  }

  public void showSql(String sql) {
    if (!this.showSql) return;
    if (!log.isDebugEnabled()) return;

    log.debug(title("SQL").concat(System.lineSeparator()).concat(sql));
  }

  private String title(String title) {
    return StrUtil.padAfter(StrUtil.format("----- {} ", title), TITLE_LENGTH, "-");
  }

  public void showParams(BatchParameters batchParameters) {
    if (!this.showSql) return;
    if (!log.isDebugEnabled()) return;
    if (batchParameters == null || batchParameters.isEmpty()) return;

    log.debug(title("PARAMETERS"));
    if (batchParameters.isBatch()) {
      for (Map.Entry<Integer, Parameters> entry : batchParameters.getBatchParameters().entrySet()) {
        Parameters parameters = entry.getValue();
        if (parameters.isEmpty()) continue;
        log.debug(CollUtil.join(parameters.getParameters().values(), ", ", this::format));
      }
    } else {
      Parameters parameters = batchParameters.getCurrentBatchParameters();
      if (parameters.isEmpty()) return;
      log.debug(CollUtil.join(parameters.getParameters().values(), ", ", this::format));
    }
  }

  public int showAffectedRows(int result) {

    if (showResult && log.isDebugEnabled()) {
      if (Math.abs(result) > 1)
        log.debug("Executed successfully, affected {} rows", result);
      else
        log.debug("Executed successfully.");
    }
    return result;
  }

  public <TView> List<TView> showResult(Class<TView> viewClass, List<TView> result) {
    if (showResult && log.isDebugEnabled()) {
      log.debug(title("RESULTS"));
      if (viewClass == Record.class) {
        showRecords(result);
      } else if (viewClass.isPrimitive() || Constant.PRIMITIVE_TYPES.contains(viewClass)) {
        showScalar(result);
      } else {
        showEntities(viewClass, result);
      }
      log.debug(StrUtil.format("Query results : {} rows", result.size()));
    }
    return result;
  }

  private <TView> void showEntities(Class<TView> viewClass, List<TView> result) {
    ClassMeta             classMeta  = Metadata.classMeta(viewClass);
    Collection<Attribute> attributes = classMeta.getQueryAttributes().values();
    log.debug(CollUtil.join(attributes, ", ", Attribute::getColumnName));
    for (TView tView : result) {
      log.debug(CollUtil.join(attributes, ", ", attribute -> getResultValue(tView, attribute)));
    }
  }

  private <TView> String getResultValue(TView entity, Attribute attribute) {
    return format(attribute.getFieldVisitor().getValue(entity));
  }

  private <TView> void showScalar(List<TView> result) {
    for (TView tView : result) {
      log.debug(format(tView));
    }
  }

  private <TView> void showRecords(List<TView> result) {
    for (int i = 0; i < result.size(); i++) {
      Record record = (Record) result.get(i);
      if (i == 0) {
        log.debug(CollUtil.join(record.keySet(), ", "));
      }
      log.debug(CollUtil.join(record.values(), ", ", this::format));
    }
  }

  private String format(Object param) {
    if (Objects.isNull(param)) return "null";
    if (param instanceof Number) return param.toString();
    if (param instanceof Date) return StrUtil.format("'{}'", Constant.DATE_FORMATTER.format(param));
    return StrUtil.format("'{}'", param);
  }
}
