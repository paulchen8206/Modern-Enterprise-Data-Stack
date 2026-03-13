package com.modern.enterprise.workflowapi.service;

import com.modern.enterprise.workflowapi.config.AppConfigProperties;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class GeValidationService {
  private final AppConfigProperties.GreatExpectations cfg;

  public GeValidationService(AppConfigProperties props) {
    this.cfg = props.getGreatExpectations();
  }

  public String validate(String suite) throws Exception {
    File cli = new File(cfg.getCliPath());
    if (!cli.exists()) {
      // Local/dev environments may not include GE CLI; callers can decide how to handle skip.
      return "SKIPPED: Great Expectations CLI not found at " + cfg.getCliPath();
    }

    // Invoke GE as an external process to avoid embedding Python runtime in the JVM service.
    ProcessBuilder pb = new ProcessBuilder(cfg.getCliPath(), "checkpoint", "run", suite);
    Process p = pb.start();
    boolean ok = p.waitFor(Duration.ofSeconds(cfg.getTimeoutSeconds()).toMillis(), TimeUnit.MILLISECONDS);
    String stdout;
    try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
      stdout = r.lines().reduce("", (a, b) -> a + (a.isEmpty() ? "" : "\n") + b);
    }
    if (!ok || p.exitValue() != 0) {
      throw new IllegalStateException("Great Expectations validation failed");
    }
    return stdout;
  }
}
