package org.adrianwalker.uploadserver.processor;

import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public abstract class RecordProcessor<R> implements Runnable {

  private final ConsumerRecord<UUID, R> record;

  public RecordProcessor(final ConsumerRecord<UUID, R> record) {
    this.record = record;
  }

  @Override
  public final void run() {

    process(record);
  }

  public abstract void process(final ConsumerRecord<UUID, R> record);
}
