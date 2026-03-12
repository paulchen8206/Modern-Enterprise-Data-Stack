package com.modern.enterprise.workflowapi.model;

public class StreamingResponse {
  private String runId;
  private String status;

  public StreamingResponse() {}

  public StreamingResponse(String runId, String status) {
    this.runId = runId;
    this.status = status;
  }

  public String getRunId() { return runId; }
  public void setRunId(String runId) { this.runId = runId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
