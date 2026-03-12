package com.modern.enterprise.workflowapi.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BatchRequest {
  @Pattern(regexp = "^[A-Za-z0-9_]+$")
  private String sourceTable;

  @Size(max = 200)
  private String destinationPrefix;

  @Min(1)
  @Max(100000)
  private Integer limit;

  private boolean triggerAirflow = true;
  private boolean runGreatExpectations = true;

  public String getSourceTable() { return sourceTable; }
  public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
  public String getDestinationPrefix() { return destinationPrefix; }
  public void setDestinationPrefix(String destinationPrefix) { this.destinationPrefix = destinationPrefix; }
  public Integer getLimit() { return limit; }
  public void setLimit(Integer limit) { this.limit = limit; }
  public boolean isTriggerAirflow() { return triggerAirflow; }
  public void setTriggerAirflow(boolean triggerAirflow) { this.triggerAirflow = triggerAirflow; }
  public boolean isRunGreatExpectations() { return runGreatExpectations; }
  public void setRunGreatExpectations(boolean runGreatExpectations) { this.runGreatExpectations = runGreatExpectations; }
}
