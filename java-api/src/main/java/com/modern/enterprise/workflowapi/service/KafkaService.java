package com.modern.enterprise.workflowapi.service;

import com.modern.enterprise.workflowapi.config.AppConfigProperties;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;

@Service
public class KafkaService {
  private final AppConfigProperties.Kafka cfg;
  private final KafkaProducer<String, String> producer;

  public KafkaService(AppConfigProperties props) {
    this.cfg = props.getKafka();
    Properties p = new Properties();
    p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cfg.getBootstrapServers());
    p.put(ProducerConfig.CLIENT_ID_CONFIG, cfg.getClientId());
    p.put(ProducerConfig.ACKS_CONFIG, "all");
    p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    p.put(ProducerConfig.RETRIES_CONFIG, 3);
    p.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, cfg.getMessageTimeoutMs());
    p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    this.producer = new KafkaProducer<>(p);
  }

  public void produce(String message, int partition) throws Exception {
    ProducerRecord<String, String> rec = new ProducerRecord<>(cfg.getTopic(), partition, null, message);
    producer.send(rec).get();
  }

  public boolean canReachKafka() {
    try {
      producer.partitionsFor(cfg.getTopic());
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  @PreDestroy
  public void close() {
    producer.flush();
    producer.close(Duration.ofSeconds(cfg.getProducerFlushSeconds()));
  }
}
