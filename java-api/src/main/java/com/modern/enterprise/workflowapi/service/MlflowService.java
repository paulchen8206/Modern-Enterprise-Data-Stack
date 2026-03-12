package com.modern.enterprise.workflowapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modern.enterprise.workflowapi.config.AppConfigProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MlflowService {
  private final AppConfigProperties.Mlflow cfg;
  private final HttpClient httpClient;
  private final ObjectMapper mapper = new ObjectMapper();

  public MlflowService(AppConfigProperties props, HttpClient httpClient) {
    this.cfg = props.getMlflow();
    this.httpClient = httpClient;
  }

  public String createRun(String experimentId, String runName) throws Exception {
    String body = mapper.writeValueAsString(Map.of("experiment_id", experimentId, "run_name", runName));
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(cfg.getTrackingUri() + "/api/2.0/mlflow/runs/create"))
        .header("Content-Type", "application/json")
        .timeout(Duration.ofSeconds(cfg.getRequestTimeoutSeconds()))
        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
        .build();
    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() >= 300) {
      throw new IllegalStateException("MLflow request failed: " + resp.statusCode() + " " + resp.body());
    }
    return resp.body();
  }

  public boolean canReachMlflow() {
    try {
      HttpRequest req = HttpRequest.newBuilder().uri(URI.create(cfg.getTrackingUri()))
          .timeout(Duration.ofSeconds(cfg.getRequestTimeoutSeconds())).GET().build();
      HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
      return resp.statusCode() < 500;
    } catch (Exception ex) {
      return false;
    }
  }
}
