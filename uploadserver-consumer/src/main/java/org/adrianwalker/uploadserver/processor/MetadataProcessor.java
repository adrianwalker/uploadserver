package org.adrianwalker.uploadserver.processor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.UUID;
import org.adrianwalker.uploadserver.cassandra.controller.FileSystemController;
import org.adrianwalker.uploadserver.cassandra.entity.File;
import org.adrianwalker.uploadserver.cassandra.controller.MetadataController;
import org.adrianwalker.uploadserver.cassandra.entity.Metadata;
import org.adrianwalker.uploadserver.record.FtpEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MetadataProcessor extends RecordProcessor<FtpEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetadataProcessor.class);
  private static final Tika TIKA = new Tika();
  private static final String DELE_COMMAND = "DELE";
  private static final String STOR_COMMAND = "STOR";

  private final FileSystemController fileController;
  private final MetadataController metadataController;

  public MetadataProcessor(
          final ConsumerRecord<UUID, FtpEvent> record,
          final FileSystemController fileController,
          final MetadataController metadataController) {

    super(record);

    LOGGER.debug("fileController = {}, metadataController = {}",
            fileController, metadataController);

    if (null == fileController) {
      throw new IllegalArgumentException("fileController is null");
    }

    if (null == metadataController) {
      throw new IllegalArgumentException("metadataController is null");
    }

    this.fileController = fileController;
    this.metadataController = metadataController;
  }

  @Override
  public void process(final ConsumerRecord<UUID, FtpEvent> record) {

    LOGGER.debug("thread = {}, record = {}", Thread.currentThread().getId(), record);

    if (null == record) {
      throw new IllegalArgumentException("record is null");
    }

    FtpEvent event = record.value();
    String path = event.getPath();
    String command = event.getCommand();

    LOGGER.debug("path = {}, command={}", path, command);

    switch (command) {

      case STOR_COMMAND:
        stor(path);
        break;

      case DELE_COMMAND:
        dele(path);
        break;

      default:
        break;
    }
  }

  private void stor(final String path) {

    LOGGER.debug("path = {}", path);

    File file = fileController.getFile(path);

    if (null == file) {
      return;
    }

    InputStream is = new BufferedInputStream(fileController.createInputStream(file));
    String mimeType = detectMimeType(is);

    try {
      is.close();
    } catch (final IOException ioe) {
      LOGGER.error(ioe.getMessage(), ioe);
    }

    Metadata metadata = new Metadata();
    metadata.setId(file.getId());
    metadata.setMimeType(mimeType);
    metadata.setTitle(file.getName());
    metadata.setTags(Collections.EMPTY_SET);

    metadataController.saveMetadata(metadata);
  }

  private void dele(final String path) {

    LOGGER.debug("path = {}", path);

    File file = fileController.getFile(path);

    if (null == file) {
      return;
    }

    metadataController.deleteMetadata(file.getId());
  }

  private String detectMimeType(final InputStream is) {

    String mimeType;

    try {

      mimeType = TIKA.detect(is);

    } catch (final IOException ioe) {

      LOGGER.error(ioe.getMessage(), ioe);
      mimeType = null;
    }

    LOGGER.debug("mimeType = {}", mimeType);

    return mimeType;
  }
}
