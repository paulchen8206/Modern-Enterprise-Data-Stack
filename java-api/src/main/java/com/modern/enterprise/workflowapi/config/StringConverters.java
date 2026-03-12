package com.modern.enterprise.workflowapi.config;

public final class StringConverters {
  private StringConverters() {}

  public static String adoMysqlToJdbc(String source) {
    String host = findValue(source, "Server", "localhost");
    String db = findValue(source, "Database", "");
    String user = findValue(source, "User", "root");
    String pass = findValue(source, "Password", "");
    return "jdbc:mysql://" + host + ":3306/" + db + "?user=" + user + "&password=" + pass;
  }

  public static String adoPostgresToJdbc(String source) {
    String host = findValue(source, "Host", "localhost");
    String db = findValue(source, "Database", "");
    String user = findValue(source, "Username", "postgres");
    String pass = findValue(source, "Password", "");
    return "jdbc:postgresql://" + host + ":5432/" + db + "?user=" + user + "&password=" + pass;
  }

  private static String findValue(String source, String key, String fallback) {
    String[] parts = source.split(";");
    for (String p : parts) {
      String[] kv = p.split("=", 2);
      if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(key)) {
        return kv[1].trim();
      }
    }
    return fallback;
  }
}
