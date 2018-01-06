package org.adrianwalker.uploadserver.cassandra.entity;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import java.nio.ByteBuffer;
import java.util.UUID;

@Table(keyspace = "uploadserver", name = "preview")
public final class Preview {

  private UUID id;
  private long size;
  private String mimeType;
  private ByteBuffer content;

  public Preview() {
  }

  @PartitionKey
  @Column(name = "id")
  public UUID getId() {

    return id;
  }

  public void setId(final UUID id) {

    this.id = id;
  }

  @Column(name = "size")
  public long getSize() {

    return size;
  }

  public void setSize(final long size) {

    this.size = size;
  }

  @Column(name = "mime_type")
  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(final String mimeType) {
    this.mimeType = mimeType;
  }

  public ByteBuffer getContent() {

    return content;
  }

  public void setContent(final ByteBuffer content) {

    this.content = content;
  }
}
