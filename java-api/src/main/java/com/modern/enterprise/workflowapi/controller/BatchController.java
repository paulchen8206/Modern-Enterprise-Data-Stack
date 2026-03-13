package com.modern.enterprise.workflowapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modern.enterprise.workflowapi.model.BatchRequest;
import com.modern.enterprise.workflowapi.model.BatchResponse;
import com.modern.enterprise.workflowapi.service.AirflowService;
import com.modern.enterprise.workflowapi.service.DbService;
import com.modern.enterprise.workflowapi.service.GeValidationService;
import com.modern.enterprise.workflowapi.service.StorageService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
public class BatchController {
  // Timestamped object keys keep each ingestion request immutable and traceable.
  private static final DateTimeFormatter KEY_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

  private final DbService dbService;
  private final StorageService storageService;
  private final GeValidationService geValidationService;
  private final AirflowService airflowService;
  private final ObjectMapper mapper = new ObjectMapper();

  public BatchController(
      DbService dbService,
      StorageService storageService,
      GeValidationService geValidationService,
      AirflowService airflowService) {
    this.dbService = dbService;
    this.storageService = storageService;
    this.geValidationService = geValidationService;
    this.airflowService = airflowService;
  }

  @PostMapping("/ingest")
  public ResponseEntity<BatchResponse> ingest(@Valid @RequestBody BatchRequest req) throws Exception {
    // Pull a bounded source snapshot from MySQL for deterministic batch ingestion.
    List<Map<String, Object>> rows = dbService.readMySqlTable(req.getSourceTable(), req.getLimit());
    String prefix = (req.getDestinationPrefix() == null || req.getDestinationPrefix().isBlank())
        ? req.getSourceTable()
        : req.getDestinationPrefix().replaceAll("^/+|/+$", "");
    if (prefix.contains("..")) {
      // Prevent path traversal style keys in object storage.
      throw new IllegalArgumentException("destinationPrefix cannot contain path traversal sequences");
    }

    String objectKey = prefix + "/" + KEY_FMT.format(Instant.now()) + ".json";
    storageService.uploadRaw(objectKey, mapper.writeValueAsString(rows));

    String geReport = null;
    if (req.isRunGreatExpectations()) {
      // Validation is optional to support faster local test runs.
      geReport = geValidationService.validate("great_expectations/expectations");
    }

    String runId = null;
    if (req.isTriggerAirflow()) {
      runId = airflowService.triggerBatch();
    }

    return ResponseEntity.ok(new BatchResponse(objectKey, runId, geReport));
  }
}
