package com.modern.enterprise.workflowapi.service;

import com.modern.enterprise.workflowapi.config.AppConfigProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class AtlasService {
  private final AppConfigProperties.Atlas cfg;
  private final HttpClient httpClient;

  public AtlasService(AppConfigProperties props, HttpClient httpClient) {
    this.cfg = props.getAtlas();
    this.httpClient = httpClient;
  }

  public String registerLineage(String payload) throws Exception {
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(cfg.getEndpoint() + "/lineage"))
        .header("Authorization", basicAuth())
        .header("Content-Type", "application/json")
        .timeout(Duration.ofSeconds(30))
        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
        .build();
    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() >= 300) {
      throw new IllegalStateException("Atlas request failed: " + resp.statusCode() + " " + resp.body());
    }
    return resp.body();
  }

  private String basicAuth() {
    String token = Base64.getEncoder().encodeToString((cfg.getUsername() + ":" + cfg.getPassword()).getBytes(StandardCharsets.UTF_8));
    return "Basic " + token;
  }
}
