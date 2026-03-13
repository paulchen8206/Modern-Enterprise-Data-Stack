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
  // Keep a single producer instance to reuse TCP connections and internal buffers.
  private KafkaProducer<String, String> producer;

  public KafkaService(AppConfigProperties props) {
    this.cfg = props.getKafka();
  }

  private synchronized KafkaProducer<String, String> getOrCreateProducer() {
    if (producer != null) {
      return producer;
    }

    Properties p = new Properties();
    p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cfg.getBootstrapServers());
    p.put(ProducerConfig.CLIENT_ID_CONFIG, cfg.getClientId());
    // Idempotence + all acks reduce duplicate/lost messages for API-triggered events.
    p.put(ProducerConfig.ACKS_CONFIG, "all");
    p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    p.put(ProducerConfig.RETRIES_CONFIG, 3);
    p.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, cfg.getMessageTimeoutMs());
    p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    producer = new KafkaProducer<>(p);
    return producer;
  }

  public void produce(String message, int partition) throws Exception {
    KafkaProducer<String, String> activeProducer = getOrCreateProducer();
    ProducerRecord<String, String> rec = new ProducerRecord<>(cfg.getTopic(), partition, null, message);
    // Use synchronous ack so callers only return success after broker confirmation.
    activeProducer.send(rec).get();
  }

  public boolean canReachKafka() {
    try {
      getOrCreateProducer().partitionsFor(cfg.getTopic());
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  @PreDestroy
  public void close() {
    if (producer != null) {
      producer.flush();
      producer.close(Duration.ofSeconds(cfg.getProducerFlushSeconds()));
    }
  }
}
