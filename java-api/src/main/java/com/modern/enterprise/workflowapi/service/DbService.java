package com.modern.enterprise.workflowapi.service;

import com.modern.enterprise.workflowapi.config.AppConfigProperties;
import com.modern.enterprise.workflowapi.config.StringConverters;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DbService {
  // Restrict table names to a safe identifier subset to prevent SQL injection.
  private static final Pattern TABLE = Pattern.compile("^[A-Za-z0-9_]+$");
  private final AppConfigProperties.ConnectionStrings cfg;

  public DbService(AppConfigProperties props) {
    this.cfg = props.getConnectionStrings();
  }

  public List<Map<String, Object>> readMySqlTable(String table, Integer limit) throws Exception {
    if (!TABLE.matcher(table).matches()) {
      throw new IllegalArgumentException("Only alphanumeric and underscore table names are allowed");
    }
    String jdbc = StringConverters.adoMysqlToJdbc(cfg.getMySql());
    // Table name is validated above, so string composition is safe here.
    String sql = limit == null ? "SELECT * FROM `" + table + "`" : "SELECT * FROM `" + table + "` LIMIT " + limit;

    try (Connection conn = DriverManager.getConnection(jdbc);
         Statement st = conn.createStatement();
         ResultSet rs = st.executeQuery(sql)) {
      st.setQueryTimeout(cfg.getCommandTimeoutSeconds());
      ResultSetMetaData md = rs.getMetaData();
      int cols = md.getColumnCount();
      List<Map<String, Object>> rows = new ArrayList<>();
      while (rs.next()) {
        // Preserve column order for predictable API payloads.
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= cols; i++) {
          row.put(md.getColumnLabel(i), rs.getObject(i));
        }
        rows.add(row);
      }
      return rows;
    }
  }

  public boolean canConnectPostgres() {
    try (Connection ignored = DriverManager.getConnection(StringConverters.adoPostgresToJdbc(cfg.getPostgres()))) {
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  public boolean canConnectMySql() {
    try (Connection ignored = DriverManager.getConnection(StringConverters.adoMysqlToJdbc(cfg.getMySql()))) {
      return true;
    } catch (Exception ex) {
      return false;
    }
  }
}
