package com.modern.enterprise.workflowapi.controller;

import com.modern.enterprise.workflowapi.service.MlflowService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ml")
public class MlController {
  private final MlflowService mlflowService;

  public MlController(MlflowService mlflowService) {
    this.mlflowService = mlflowService;
  }

  @PostMapping("/run")
  public ResponseEntity<Map<String, String>> run(@RequestParam String expId, @RequestParam String name) throws Exception {
    String res = mlflowService.createRun(expId, name);
    return ResponseEntity.ok(Map.of("result", res));
  }
}
