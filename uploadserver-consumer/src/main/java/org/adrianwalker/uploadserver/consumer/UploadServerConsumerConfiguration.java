package org.adrianwalker.uploadserver.consumer;

import java.util.Properties;

public final class UploadServerConsumerConfiguration extends Properties {

  private static final String KAFKA_HOST = "kafka.host";
  private static final String KAFKA_PORT = "kafka.port";
  private static final String KAFKA_TOPIC = "kafka.topic";

  private static final String CASSANDRA_HOST = "cassandra.host";
  private static final String CASSANDRA_PORT = "cassandra.port";
  private static final String CASSANDRA_KEYSPACE = "cassandra.keyspace";

  private static final String CONSUMER_POLL_INTERVAL = "consumer.poll.interval";
  private static final String CONSUMER_THREADS = "consumer.threads";

  public String getKafkaHost() {

    return getProperty(KAFKA_HOST);
  }

  public int getKafkaPort() {

    return Integer.parseInt(getProperty(KAFKA_PORT));
  }

  public String getKafkaTopic() {

    return getProperty(KAFKA_TOPIC);
  }

  public String getCassandraHost() {

    return getProperty(CASSANDRA_HOST);
  }

  public int getCassandraPort() {

    return Integer.parseInt(getProperty(CASSANDRA_PORT));
  }

  public String getCassandraKeyspace() {

    return getProperty(CASSANDRA_KEYSPACE);
  }

  public int getConsumerPollInterval() {

    return Integer.parseInt(getProperty(CONSUMER_POLL_INTERVAL));
  }

  public int getConsumerThreads() {

    return Integer.parseInt(getProperty(CONSUMER_THREADS));
  }
}
