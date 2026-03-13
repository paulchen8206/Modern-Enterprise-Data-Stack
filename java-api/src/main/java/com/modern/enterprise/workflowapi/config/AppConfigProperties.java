package com.modern.enterprise.workflowapi.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppConfigProperties {
  // Group external dependency settings under `app.*` for type-safe injection.
  private ConnectionStrings connectionStrings = new ConnectionStrings();
  private Minio minio = new Minio();
  private Kafka kafka = new Kafka();
  private Airflow airflow = new Airflow();
  private GreatExpectations greatExpectations = new GreatExpectations();
  private Atlas atlas = new Atlas();
  private Mlflow mlflow = new Mlflow();
  private Github github = new Github();

  public ConnectionStrings getConnectionStrings() { return connectionStrings; }
  public void setConnectionStrings(ConnectionStrings connectionStrings) { this.connectionStrings = connectionStrings; }
  public Minio getMinio() { return minio; }
  public void setMinio(Minio minio) { this.minio = minio; }
  public Kafka getKafka() { return kafka; }
  public void setKafka(Kafka kafka) { this.kafka = kafka; }
  public Airflow getAirflow() { return airflow; }
  public void setAirflow(Airflow airflow) { this.airflow = airflow; }
  public GreatExpectations getGreatExpectations() { return greatExpectations; }
  public void setGreatExpectations(GreatExpectations greatExpectations) { this.greatExpectations = greatExpectations; }
  public Atlas getAtlas() { return atlas; }
  public void setAtlas(Atlas atlas) { this.atlas = atlas; }
  public Mlflow getMlflow() { return mlflow; }
  public void setMlflow(Mlflow mlflow) { this.mlflow = mlflow; }
  public Github getGithub() { return github; }
  public void setGithub(Github github) { this.github = github; }

  public static class ConnectionStrings {
    // Legacy ADO-style connection strings are converted to JDBC at runtime.
    @NotBlank private String mySql = "";
    @NotBlank private String postgres = "";
    @Min(5) private int commandTimeoutSeconds = 30;
    public String getMySql() { return mySql; }
    public void setMySql(String mySql) { this.mySql = mySql; }
    public String getPostgres() { return postgres; }
    public void setPostgres(String postgres) { this.postgres = postgres; }
    public int getCommandTimeoutSeconds() { return commandTimeoutSeconds; }
    public void setCommandTimeoutSeconds(int commandTimeoutSeconds) { this.commandTimeoutSeconds = commandTimeoutSeconds; }
  }

  public static class Minio {
    // Endpoint omits protocol in app config; service layer applies scheme.
    @NotBlank private String endpoint = "";
    @NotBlank private String accessKey = "";
    @NotBlank private String secretKey = "";
    @NotBlank private String bucketRaw = "raw-data";
    @NotBlank private String bucketProcessed = "processed-data";
    @Min(0) private int maxUploadRetries = 2;
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getBucketRaw() { return bucketRaw; }
    public void setBucketRaw(String bucketRaw) { this.bucketRaw = bucketRaw; }
    public String getBucketProcessed() { return bucketProcessed; }
    public void setBucketProcessed(String bucketProcessed) { this.bucketProcessed = bucketProcessed; }
    public int getMaxUploadRetries() { return maxUploadRetries; }
    public void setMaxUploadRetries(int maxUploadRetries) { this.maxUploadRetries = maxUploadRetries; }
  }

  public static class Kafka {
    @NotBlank private String bootstrapServers = "";
    @NotBlank private String topic = "events";
    @NotBlank private String clientId = "workflow-api";
    @Min(1000) private int messageTimeoutMs = 5000;
    @Min(1) private int producerFlushSeconds = 5;
    public String getBootstrapServers() { return bootstrapServers; }
    public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public int getMessageTimeoutMs() { return messageTimeoutMs; }
    public void setMessageTimeoutMs(int messageTimeoutMs) { this.messageTimeoutMs = messageTimeoutMs; }
    public int getProducerFlushSeconds() { return producerFlushSeconds; }
    public void setProducerFlushSeconds(int producerFlushSeconds) { this.producerFlushSeconds = producerFlushSeconds; }
  }

  public static class Airflow {
    @NotBlank private String baseUrl = "";
    @NotBlank private String username = "";
    @NotBlank private String password = "";
    @NotBlank private String batchDagId = "batch_ingestion_dag";
    @NotBlank private String streamingDagId = "streaming_monitoring_dag";
    @Min(5) private int requestTimeoutSeconds = 30;
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getBatchDagId() { return batchDagId; }
    public void setBatchDagId(String batchDagId) { this.batchDagId = batchDagId; }
    public String getStreamingDagId() { return streamingDagId; }
    public void setStreamingDagId(String streamingDagId) { this.streamingDagId = streamingDagId; }
    public int getRequestTimeoutSeconds() { return requestTimeoutSeconds; }
    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) { this.requestTimeoutSeconds = requestTimeoutSeconds; }
  }

  public static class GreatExpectations {
    @NotBlank private String cliPath = "";
    @Min(1) private int timeoutSeconds = 300;
    public String getCliPath() { return cliPath; }
    public void setCliPath(String cliPath) { this.cliPath = cliPath; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
  }

  public static class Atlas {
    @NotBlank private String endpoint = "";
    @NotBlank private String username = "";
    @NotBlank private String password = "";
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
  }

  public static class Mlflow {
    @NotBlank private String trackingUri = "";
    @Min(5) private int requestTimeoutSeconds = 30;
    public String getTrackingUri() { return trackingUri; }
    public void setTrackingUri(String trackingUri) { this.trackingUri = trackingUri; }
    public int getRequestTimeoutSeconds() { return requestTimeoutSeconds; }
    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) { this.requestTimeoutSeconds = requestTimeoutSeconds; }
  }

  public static class Github {
    @NotBlank private String actionsApi = "";
    private String token = "";
    @NotBlank private String userAgent = "workflow-api";
    public String getActionsApi() { return actionsApi; }
    public void setActionsApi(String actionsApi) { this.actionsApi = actionsApi; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
  }
}
