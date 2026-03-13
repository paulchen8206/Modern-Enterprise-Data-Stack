package com.modern.enterprise.workflowapi.config;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientsConfig {
  @Bean
  public HttpClient httpClient() {
    // Shared JDK client keeps transport setup centralized for all outbound services.
    return HttpClient.newBuilder().build();
  }
}
