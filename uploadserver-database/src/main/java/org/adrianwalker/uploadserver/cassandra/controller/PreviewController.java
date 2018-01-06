package org.adrianwalker.uploadserver.cassandra.controller;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import java.util.UUID;
import org.adrianwalker.uploadserver.cassandra.entity.Preview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PreviewController {

  private static final Logger LOGGER = LoggerFactory.getLogger(PreviewController.class);

  private final Mapper<Preview> previewMapper;

  public PreviewController(final Session session) {

    LOGGER.debug("session = {}", session);

    if (null == session) {
      throw new IllegalArgumentException("session is null");
    }

    MappingManager manager = new MappingManager(session);

    previewMapper = manager.mapper(Preview.class);
  }

  public Preview getPreview(final UUID id) {

    LOGGER.debug("id = {}", id);

    if (null == id) {
      throw new IllegalArgumentException("id is null");
    }

    return previewMapper.get(id);
  }

  public void savePreview(final Preview preview) {

    LOGGER.debug("preview = {}", preview);

    if (null == preview) {
      throw new IllegalArgumentException("preview is null");
    }

    previewMapper.save(preview);
  }

  public void deletePreview(final UUID id) {

    LOGGER.debug("id = {}", id);

    if (null == id) {
      throw new IllegalArgumentException("id is null");
    }

    previewMapper.delete(id);
  }
}
