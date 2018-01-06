package org.adrianwalker.uploadserver.rest;

import java.util.Properties;

public final class UploadServerConfiguration extends Properties {

  private static final String CASSANDRA_HOST = "cassandra.host";
  private static final String CASSANDRA_PORT = "cassandra.port";
  private static final String CASSANDRA_KEYSPACE = "cassandra.keyspace";

  public String getCassandraHost() {

    return getProperty(CASSANDRA_HOST);
  }

  public int getCassandraPort() {

    return Integer.parseInt(getProperty(CASSANDRA_PORT));
  }

  public String getCassandraKeyspace() {

    return getProperty(CASSANDRA_KEYSPACE);
  }
}
