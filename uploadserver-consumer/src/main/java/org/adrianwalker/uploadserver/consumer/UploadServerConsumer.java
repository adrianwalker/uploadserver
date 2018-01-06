package org.adrianwalker.uploadserver.consumer;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.adrianwalker.uploadserver.cassandra.controller.FileSystemController;
import org.adrianwalker.uploadserver.cassandra.controller.MetadataController;
import org.adrianwalker.uploadserver.cassandra.controller.PreviewController;
import org.adrianwalker.uploadserver.processor.MetadataProcessor;
import org.adrianwalker.uploadserver.processor.PreviewProcessor;
import org.adrianwalker.uploadserver.record.FtpEvent;
import org.adrianwalker.uploadserver.record.serialization.FtpEventDeserializer;
import org.adrianwalker.uploadserver.record.serialization.GenericDeserializer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UploadServerConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(UploadServerConsumer.class);

  public static void main(final String[] args) throws Exception {

    LOGGER.debug("args = {}", (Object[]) args);

    UploadServerConsumerConfiguration configuration;
    if (args.length == 0) {
      throw new IllegalArgumentException("configuration file argument required");
    } else {
      configuration = readConfiguration(args[0]);
    }

    new UploadServerConsumer().run(configuration);
  }

  private void run(final UploadServerConsumerConfiguration configuration) throws IOException {

    LOGGER.debug("configuration = {}", configuration);

    ExecutorService executor = Executors.newFixedThreadPool(configuration.getConsumerThreads());

    Session session = createSession(configuration);
    FileSystemController fileSystemController = new FileSystemController(session);
    MetadataController metadataController = new MetadataController(session);
    PreviewController previewController = new PreviewController(session);

    Consumer<UUID, FtpEvent> consumer = createConsumer(configuration);
    consumer.subscribe(Collections.singletonList(configuration.getKafkaTopic()));

    while (true) {

      ConsumerRecords<UUID, FtpEvent> records
              = consumer.poll(configuration.getConsumerPollInterval());

      for (ConsumerRecord<UUID, FtpEvent> record : records) {

        executor.submit(new MetadataProcessor(record, fileSystemController, metadataController));
        executor.submit(new PreviewProcessor(record, fileSystemController, previewController));
      }

      consumer.commitSync();
    }
  }

  private static UploadServerConsumerConfiguration readConfiguration(final String path)
          throws IOException {

    LOGGER.debug("path = {}", path);

    UploadServerConsumerConfiguration configuration = new UploadServerConsumerConfiguration();
    configuration.load(Files.newBufferedReader(Paths.get(path)));

    LOGGER.debug(configuration.toString());

    return configuration;
  }

  private Session createSession(final UploadServerConsumerConfiguration configuration) {

    Cluster cluster = new Cluster.Builder()
            .addContactPoints(configuration.getCassandraHost())
            .withPort(configuration.getCassandraPort())
            .build();

    return cluster.connect(configuration.getCassandraKeyspace());
  }

  private Consumer<UUID, FtpEvent> createConsumer(
          final UploadServerConsumerConfiguration configuration) {

    LOGGER.debug("configuration = {}", configuration);

    Properties properties = new Properties();
    properties.put(BOOTSTRAP_SERVERS_CONFIG,
            configuration.getKafkaHost() + ":" + configuration.getKafkaPort());
    properties.put(GROUP_ID_CONFIG, configuration.getKafkaTopic());

    GenericDeserializer<UUID> keyDeserializer = new GenericDeserializer<>();
    FtpEventDeserializer valueDeserializer = new FtpEventDeserializer();

    Consumer<UUID, FtpEvent> consumer = new KafkaConsumer<>(
            properties,
            keyDeserializer,
            valueDeserializer);

    return consumer;
  }
}
