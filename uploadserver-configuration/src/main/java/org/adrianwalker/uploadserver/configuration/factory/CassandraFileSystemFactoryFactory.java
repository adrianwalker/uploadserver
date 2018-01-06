package org.adrianwalker.uploadserver.configuration.factory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.adrianwalker.uploadserver.cassandra.controller.FileSystemController;
import org.adrianwalker.ftpserver.filesystem.CassandraFileSystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CassandraFileSystemFactoryFactory {

  private static final Logger LOGGER
          = LoggerFactory.getLogger(CassandraFileSystemFactoryFactory.class);

  private final String host;
  private final int port;
  private final String keyspace;

  public CassandraFileSystemFactoryFactory(
          final String host, final int port, final String keyspace) {

    LOGGER.debug("host = {}, port = {}, keyspace = {}", host, port, keyspace);

    if (null == host) {
      throw new IllegalArgumentException("host is null");
    }

    if (0 >= port) {
      throw new IllegalArgumentException("invalid port");
    }

    if (null == keyspace) {
      throw new IllegalArgumentException("keyspace is null");
    }

    this.host = host;
    this.port = port;
    this.keyspace = keyspace;
  }

  public CassandraFileSystemFactory newInstance() {

    LOGGER.debug("creating instance");

    Cluster cluster = new Cluster.Builder()
            .addContactPoints(host)
            .withPort(port)
            .build();
    Session session = cluster.connect(keyspace);
    FileSystemController controller = new FileSystemController(session);
    CassandraFileSystemFactory factory = new CassandraFileSystemFactory(controller);

    return factory;
  }
}
