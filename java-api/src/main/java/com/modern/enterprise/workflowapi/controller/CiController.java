package com.modern.enterprise.workflowapi.controller;

import com.modern.enterprise.workflowapi.service.CiService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ci")
public class CiController {
  private final CiService ciService;

  public CiController(CiService ciService) {
    this.ciService = ciService;
  }

  @PostMapping("/trigger")
  public ResponseEntity<Map<String, String>> trigger(@RequestParam String wf, @RequestParam String branch) throws Exception {
    // `wf` maps to workflow file name (or workflow id) under GitHub Actions.
    String res = ciService.triggerWorkflow(wf, branch);
    return ResponseEntity.ok(Map.of("result", res));
  }
}
