package com.modern.enterprise.workflowapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modern.enterprise.workflowapi.model.StreamingRequest;
import com.modern.enterprise.workflowapi.model.StreamingResponse;
import com.modern.enterprise.workflowapi.service.AirflowService;
import com.modern.enterprise.workflowapi.service.KafkaService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stream")
public class StreamingController {
  private final KafkaService kafkaService;
  private final AirflowService airflowService;
  private final ObjectMapper mapper = new ObjectMapper();

  public StreamingController(KafkaService kafkaService, AirflowService airflowService) {
    this.kafkaService = kafkaService;
    this.airflowService = airflowService;
  }

  @PostMapping("/produce")
  public ResponseEntity<Map<String, String>> produce(@Valid @RequestBody StreamingRequest req) throws Exception {
    // Wrap payload with metadata so downstream consumers can debug provenance.
    Map<String, Object> msg = new LinkedHashMap<>();
    msg.put("ts", Instant.now().toString());
    msg.put("partition", req.getPartition());
    msg.put("payload", req.getPayload());
    kafkaService.produce(mapper.writeValueAsString(msg), req.getPartition());
    return ResponseEntity.ok(Map.of("status", "sent"));
  }

  @PostMapping("/run")
  public ResponseEntity<StreamingResponse> run() throws Exception {
    // Delegate long-running processing to Airflow rather than blocking the API.
    String runId = airflowService.triggerStreaming();
    return ResponseEntity.ok(new StreamingResponse(runId, "scheduled"));
  }
}
