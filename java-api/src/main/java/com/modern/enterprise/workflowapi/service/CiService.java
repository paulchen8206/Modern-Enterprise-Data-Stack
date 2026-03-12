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
public class CiService {
  private final AppConfigProperties.Github cfg;
  private final HttpClient httpClient;
  private final ObjectMapper mapper = new ObjectMapper();

  public CiService(AppConfigProperties props, HttpClient httpClient) {
    this.cfg = props.getGithub();
    this.httpClient = httpClient;
  }

  public String triggerWorkflow(String wf, String branch) throws Exception {
    String base = cfg.getActionsApi().endsWith("/") ? cfg.getActionsApi() : cfg.getActionsApi() + "/";
    String body = mapper.writeValueAsString(Map.of("ref", branch));
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(base + wf + "/dispatches"))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + cfg.getToken())
        .header("User-Agent", cfg.getUserAgent())
        .timeout(Duration.ofSeconds(30))
        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
        .build();
    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() >= 300) {
      throw new IllegalStateException("GitHub workflow trigger failed: " + resp.statusCode() + " " + resp.body());
    }
    return resp.body();
  }
}
