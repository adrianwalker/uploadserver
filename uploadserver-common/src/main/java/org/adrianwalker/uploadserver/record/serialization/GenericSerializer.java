package org.adrianwalker.uploadserver.record.serialization;

import java.io.Serializable;
import java.util.Map;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.kafka.common.serialization.Serializer;

public final class GenericSerializer<T extends Serializable> implements Serializer<T> {

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
  }

  @Override
  public byte[] serialize(final String topic, final T t) {

    return SerializationUtils.serialize(t);
  }

  @Override
  public void close() {
  }
}
