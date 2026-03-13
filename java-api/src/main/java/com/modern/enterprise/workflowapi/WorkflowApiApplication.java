package com.modern.enterprise.workflowapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WorkflowApiApplication {
  public static void main(String[] args) {
    // Entrypoint for local run, tests, and container startup.
    SpringApplication.run(WorkflowApiApplication.class, args);
  }
}
