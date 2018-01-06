package org.adrianwalker.uploadserver.record.serialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import org.adrianwalker.uploadserver.record.FtpEvent;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

public final class FtpEventDeserializer implements Deserializer<FtpEvent> {

  private final DatumReader<FtpEvent> reader;

  public FtpEventDeserializer() {

    reader = new SpecificDatumReader<>(FtpEvent.class);
  }

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
  }

  @Override
  public FtpEvent deserialize(final String topic, final byte[] objectData) {

    try (ByteArrayInputStream in = new ByteArrayInputStream(objectData)) {

      Decoder decoder = DecoderFactory.get().jsonDecoder(FtpEvent.getClassSchema(), in);
      FtpEvent ftpEvent = reader.read(null, decoder);

      return ftpEvent;

    } catch (final IOException ioe) {

      throw new SerializationException(ioe);
    }
  }

  @Override
  public void close() {
  }
}
