package org.adrianwalker.uploadserver.record.serialization;

import java.io.Serializable;
import java.util.Map;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.kafka.common.serialization.Deserializer;

public final class GenericDeserializer<T extends Serializable> implements Deserializer<T> {

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
  }

  @Override
  public T deserialize(final String topic, final byte[] objectData) {

    T t = (objectData == null)
            ? null
            : (T) SerializationUtils.deserialize(objectData);

    return t;
  }

  @Override
  public void close() {
  }
}
