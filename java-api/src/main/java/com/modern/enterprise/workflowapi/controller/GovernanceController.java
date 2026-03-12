package com.modern.enterprise.workflowapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modern.enterprise.workflowapi.service.AtlasService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance")
public class GovernanceController {
  private final AtlasService atlasService;
  private final ObjectMapper mapper = new ObjectMapper();

  public GovernanceController(AtlasService atlasService) {
    this.atlasService = atlasService;
  }

  @PostMapping("/lineage")
  public ResponseEntity<Map<String, String>> lineage(@RequestBody Object payload) throws Exception {
    String res = atlasService.registerLineage(mapper.writeValueAsString(payload));
    return ResponseEntity.ok(Map.of("result", res));
  }
}
