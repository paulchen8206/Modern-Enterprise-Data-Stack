package com.modern.enterprise.workflowapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modern.enterprise.workflowapi.config.AppConfigProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AirflowService {
  // UTC timestamp keeps run IDs stable across developer machines and CI agents.
  private static final DateTimeFormatter ID_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);
  private final AppConfigProperties.Airflow cfg;
  private final HttpClient httpClient;
  private final ObjectMapper mapper = new ObjectMapper();

  public AirflowService(AppConfigProperties props, HttpClient httpClient) {
    this.cfg = props.getAirflow();
    this.httpClient = httpClient;
  }

  public String triggerBatch() throws Exception {
    // Prefixes make it easy to identify DAG runs created by each API endpoint.
    String id = "batch_" + ID_FMT.format(Instant.now());
    postDagRun(cfg.getBatchDagId(), id);
    return id;
  }

  public String triggerStreaming() throws Exception {
    String id = "stream_" + ID_FMT.format(Instant.now());
    postDagRun(cfg.getStreamingDagId(), id);
    return id;
  }

  public boolean canReachAirflow() {
    try {
      // Health endpoint returns 200 in the normal case, but auth/network intermediaries
      // can still prove basic reachability with non-5xx status codes.
      HttpRequest req = HttpRequest.newBuilder()
          .uri(URI.create(cfg.getBaseUrl() + "/health"))
          .header("Authorization", basicAuth())
          .timeout(Duration.ofSeconds(cfg.getRequestTimeoutSeconds()))
          .GET()
          .build();
      HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
      return resp.statusCode() < 500;
    } catch (Exception ex) {
      return false;
    }
  }

  private void postDagRun(String dagId, String runId) throws Exception {
    // Airflow requires dag_run_id in the request body for explicit run tracking.
    String body = mapper.writeValueAsString(Map.of("dag_run_id", runId));
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(cfg.getBaseUrl() + "/dags/" + dagId + "/dagRuns"))
        .header("Authorization", basicAuth())
        .header("Content-Type", "application/json")
        .timeout(Duration.ofSeconds(cfg.getRequestTimeoutSeconds()))
        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
        .build();
    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() >= 300) {
      throw new IllegalStateException("Airflow request failed: " + resp.statusCode() + " " + resp.body());
    }
  }

  private String basicAuth() {
    String token = Base64.getEncoder().encodeToString((cfg.getUsername() + ":" + cfg.getPassword()).getBytes(StandardCharsets.UTF_8));
    return "Basic " + token;
  }
}
