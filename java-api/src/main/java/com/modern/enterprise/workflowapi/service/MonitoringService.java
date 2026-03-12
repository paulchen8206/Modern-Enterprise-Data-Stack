package com.modern.enterprise.workflowapi.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MonitoringService {
  private final DbService dbService;
  private final StorageService storageService;
  private final KafkaService kafkaService;
  private final AirflowService airflowService;
  private final MlflowService mlflowService;

  public MonitoringService(
      DbService dbService,
      StorageService storageService,
      KafkaService kafkaService,
      AirflowService airflowService,
      MlflowService mlflowService) {
    this.dbService = dbService;
    this.storageService = storageService;
    this.kafkaService = kafkaService;
    this.airflowService = airflowService;
    this.mlflowService = mlflowService;
  }

  public Map<String, String> health() {
    Map<String, String> res = new LinkedHashMap<>();
    res.put("mysql", dbService.canConnectMySql() ? "UP" : "DOWN");
    res.put("postgres", dbService.canConnectPostgres() ? "UP" : "DOWN");
    res.put("minio", storageService.canReachMinio() ? "UP" : "DOWN");
    res.put("kafka", kafkaService.canReachKafka() ? "UP" : "DOWN");
    res.put("airflow", airflowService.canReachAirflow() ? "UP" : "DOWN");
    res.put("mlflow", mlflowService.canReachMlflow() ? "UP" : "DOWN");
    boolean overall = res.values().stream().allMatch("UP"::equals);
    res.put("overall", overall ? "UP" : "DEGRADED");
    return res;
  }
}
