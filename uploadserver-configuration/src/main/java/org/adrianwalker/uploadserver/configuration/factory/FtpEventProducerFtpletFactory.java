package org.adrianwalker.uploadserver.configuration.factory;

import java.util.Properties;
import java.util.UUID;
import org.adrianwalker.uploadserver.producer.ftp.FtpEventProducerFtplet;
import org.adrianwalker.uploadserver.record.FtpEvent;
import org.adrianwalker.uploadserver.record.serialization.FtpEventSerializer;
import org.adrianwalker.uploadserver.record.serialization.GenericSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FtpEventProducerFtpletFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(FtpEventProducerFtpletFactory.class);

  private final String host;
  private final int port;
  private final String topic;

  public FtpEventProducerFtpletFactory(final String host, final int port, final String topic) {

    LOGGER.debug("host = {}, port = {}, topic = {}", host, port, topic);

    if (null == host) {
      throw new IllegalArgumentException("host is null");
    }

    if (0 >= port) {
      throw new IllegalArgumentException("invalid port");
    }

    if (null == topic) {
      throw new IllegalArgumentException("keyspace is null");
    }

    this.host = host;
    this.port = port;
    this.topic = topic;
  }

  public FtpEventProducerFtplet newInstance() {

    LOGGER.debug("creating instance");

    Properties properties = new Properties();
    properties.put(BOOTSTRAP_SERVERS_CONFIG, host + ":" + port);

    GenericSerializer<UUID> keySerializer = new GenericSerializer<>();
    FtpEventSerializer valueSerializer = new FtpEventSerializer();

    Producer<UUID, FtpEvent> producer = new KafkaProducer<>(
            properties,
            keySerializer,
            valueSerializer);
    FtpEventProducerFtplet ftplet = new FtpEventProducerFtplet(producer, topic);

    return ftplet;
  }
}
