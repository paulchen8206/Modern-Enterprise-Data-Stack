package com.modern.enterprise.workflowapi.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;

public class StreamingRequest {
  @Min(0)
  // Caller-selected Kafka partition for deterministic message routing.
  private int partition;
  // Arbitrary JSON payload forwarded as-is to the event bus.
  private JsonNode payload;

  public int getPartition() { return partition; }
  public void setPartition(int partition) { this.partition = partition; }
  public JsonNode getPayload() { return payload; }
  public void setPayload(JsonNode payload) { this.payload = payload; }
}
