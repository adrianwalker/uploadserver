package org.adrianwalker.uploadserver.processor;

import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.adrianwalker.uploadserver.cassandra.controller.FileSystemController;
import org.adrianwalker.uploadserver.cassandra.entity.File;
import org.adrianwalker.uploadserver.cassandra.controller.PreviewController;
import org.adrianwalker.uploadserver.cassandra.entity.Preview;
import org.adrianwalker.uploadserver.record.FtpEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.tika.Tika;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PreviewProcessor extends RecordProcessor<FtpEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PreviewProcessor.class);
  private static final Tika TIKA = new Tika();
  private static final int THUMBNAIL_SIZE = 150;
  private static final String THUMBNAIL_FORMAT = "jpg";
  private static final String THUMBNAIL_MIME_TYPE = "image/jpeg";
  private static final String DELE_COMMAND = "DELE";
  private static final String STOR_COMMAND = "STOR";
  private static final List<String> SUPPORTED_IMAGE_MIME_TYPES = Arrays.asList(
          "image/jpeg",
          "image/png",
          "image/gif");

  private final FileSystemController fileController;
  private final PreviewController previewController;

  public PreviewProcessor(
          final ConsumerRecord<UUID, FtpEvent> record,
          final FileSystemController fileController,
          final PreviewController previewController) {

    super(record);

    LOGGER.debug("fileController = {}, previewController = {}",
            fileController, previewController);

    if (null == fileController) {
      throw new IllegalArgumentException("fileController is null");
    }

    if (null == previewController) {
      throw new IllegalArgumentException("previewController is null");
    }

    this.fileController = fileController;
    this.previewController = previewController;
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

    BufferedImage thumbnail;
    if (SUPPORTED_IMAGE_MIME_TYPES.contains(mimeType)) {
      thumbnail = createThumbnail(is);
    } else {
      thumbnail = null;
    }

    try {
      is.close();
    } catch (final IOException ioe) {
      LOGGER.error(ioe.getMessage(), ioe);
    }

    ByteBuffer content;
    if (null == thumbnail) {
      content = ByteBuffer.allocate(0);
    } else {
      content = createPreview(thumbnail);
    }

    Preview preview = new Preview();
    preview.setId(file.getId());
    preview.setSize(content.capacity());
    preview.setMimeType(THUMBNAIL_MIME_TYPE);
    preview.setContent(content);

    previewController.savePreview(preview);
  }

  private void dele(final String path) {

    LOGGER.debug("path = {}", path);

    File file = fileController.getFile(path);

    if (null == file) {
      return;
    }

    previewController.deletePreview(file.getId());
  }

  private ByteBuffer createPreview(final BufferedImage thumbnail) {

    ByteBuffer preview;

    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      ImageIO.write(thumbnail, THUMBNAIL_FORMAT, out);
      preview = ByteBuffer.wrap(out.toByteArray());

    } catch (final IOException ioe) {

      LOGGER.error(ioe.getMessage(), ioe);
      preview = null;
    }

    return preview;
  }

  private BufferedImage createThumbnail(final InputStream is) {

    BufferedImage image;

    try {

      image = ImageIO.read(is);
      image = Scalr.resize(image,
              image.getHeight() > image.getWidth()
              ? Scalr.Mode.FIT_TO_WIDTH
              : Scalr.Mode.FIT_TO_HEIGHT,
              THUMBNAIL_SIZE);

      image = Scalr.crop(image,
              (image.getWidth() - THUMBNAIL_SIZE) / 2,
              (image.getHeight() - THUMBNAIL_SIZE) / 2,
              THUMBNAIL_SIZE,
              THUMBNAIL_SIZE);

    } catch (final ImagingOpException | IOException | IllegalArgumentException e) {

      LOGGER.error(e.getMessage(), e);
      image = null;
    }

    return image;
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
