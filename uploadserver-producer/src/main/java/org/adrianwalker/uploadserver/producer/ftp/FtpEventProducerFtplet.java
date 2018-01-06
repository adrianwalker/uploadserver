package org.adrianwalker.uploadserver.producer.ftp;

import java.io.IOException;
import java.util.UUID;
import org.adrianwalker.uploadserver.record.FtpEvent;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FtpEventProducerFtplet extends DefaultFtplet {

  private static final Logger LOGGER = LoggerFactory.getLogger(FtpEventProducerFtplet.class);

  private final Producer<UUID, FtpEvent> producer;
  private final String topic;

  public FtpEventProducerFtplet(final Producer<UUID, FtpEvent> producer, final String topic) {

    LOGGER.debug("producer = {}, topic = {}", producer, topic);

    this.producer = producer;
    this.topic = topic;
  }

  @Override
  public void destroy() {

    producer.close();
  }

  @Override
  public FtpletResult onUploadEnd(final FtpSession session, final FtpRequest request)
          throws FtpException, IOException {

    LOGGER.debug("session = {}, request = {}", session, request);

    onRequest(session, request);

    return super.onUploadEnd(session, request);
  }

  @Override
  public FtpletResult onDeleteStart(final FtpSession session, final FtpRequest request)
          throws FtpException, IOException {

    LOGGER.debug("session = {}, request = {}", session, request);

    onRequest(session, request);

    return super.onDeleteStart(session, request);
  }

  private void onRequest(final FtpSession session, final FtpRequest request)
          throws FtpException {

    LOGGER.debug("session = {}, request = {}", session, request);

    FtpFile ftpFile = session
            .getFileSystemView()
            .getFile(request.getArgument());

    FtpEvent event = new FtpEvent();
    event.setUsername(session.getUser().getName());
    event.setCommand(request.getCommand());
    event.setPath(ftpFile.getAbsolutePath());

    ProducerRecord<UUID, FtpEvent> record = new ProducerRecord<>(
            topic,
            UUID.randomUUID(),
            event);

    producer.send(record);
  }
}
