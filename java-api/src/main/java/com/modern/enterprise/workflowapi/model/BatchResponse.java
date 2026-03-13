package com.modern.enterprise.workflowapi.model;

public class BatchResponse {
  // Object storage path where ingested snapshot was written.
  private String objectKey;
  // Optional Airflow run ID when orchestration trigger is enabled.
  private String runId;
  // Optional validation output when Great Expectations is enabled.
  private String geReport;

  public BatchResponse() {}

  public BatchResponse(String objectKey, String runId, String geReport) {
    this.objectKey = objectKey;
    this.runId = runId;
    this.geReport = geReport;
  }

  public String getObjectKey() { return objectKey; }
  public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
  public String getRunId() { return runId; }
  public void setRunId(String runId) { this.runId = runId; }
  public String getGeReport() { return geReport; }
  public void setGeReport(String geReport) { this.geReport = geReport; }
}
