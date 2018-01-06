package org.adrianwalker.uploadserver.cassandra.controller;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import java.util.UUID;
import org.adrianwalker.uploadserver.cassandra.entity.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MetadataController {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetadataController.class);

  private final Mapper<Metadata> metadataMapper;

  public MetadataController(final Session session) {

    LOGGER.debug("session = {}", session);

    if (null == session) {
      throw new IllegalArgumentException("session is null");
    }

    MappingManager manager = new MappingManager(session);

    metadataMapper = manager.mapper(Metadata.class);
  }

  public Metadata getMetadata(final UUID id) {

    LOGGER.debug("id = {}", id);

    if (null == id) {
      throw new IllegalArgumentException("id is null");
    }

    return metadataMapper.get(id);
  }

  public void saveMetadata(final Metadata metadata) {

    LOGGER.debug("metadata = {}", metadata);

    if (null == metadata) {
      throw new IllegalArgumentException("metadata is null");
    }

    metadataMapper.save(metadata);
  }

  public void deleteMetadata(final UUID id) {

    LOGGER.debug("id = {}", id);

    if (null == id) {
      throw new IllegalArgumentException("id is null");
    }

    metadataMapper.delete(id);
  }
}
