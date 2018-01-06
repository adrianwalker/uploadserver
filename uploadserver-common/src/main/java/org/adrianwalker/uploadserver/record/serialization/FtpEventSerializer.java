package org.adrianwalker.uploadserver.record.serialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.adrianwalker.uploadserver.record.FtpEvent;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

public final class FtpEventSerializer implements Serializer<FtpEvent> {

  private final DatumWriter<FtpEvent> writer;

  public FtpEventSerializer() {

    writer = new SpecificDatumWriter<>(FtpEvent.class);
  }

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
  }

  @Override
  public byte[] serialize(final String topic, final FtpEvent ftpEvent) {

    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

      Encoder encoder = EncoderFactory.get().jsonEncoder(FtpEvent.getClassSchema(), os, true);
      writer.write(ftpEvent, encoder);
      encoder.flush();

      return os.toByteArray();

    } catch (final IOException ioe) {

      throw new SerializationException(ioe);
    }
  }

  @Override
  public void close() {
  }
}
