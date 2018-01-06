package org.adrianwalker.uploadserver.cassandra.entity;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import java.util.Set;
import java.util.UUID;

@Table(keyspace = "uploadserver", name = "metadata")
public final class Metadata {

  private UUID id;
  private String mimeType;
  private String title;
  private Set<String> tags;

  public Metadata() {
  }

  @PartitionKey
  @Column(name = "id")
  public UUID getId() {

    return id;
  }

  public void setId(final UUID id) {

    this.id = id;
  }

  @Column(name = "mime_type")
  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(final String mimeType) {
    this.mimeType = mimeType;
  }

  @Column(name = "title")
  public String getTitle() {

    return title;
  }

  public void setTitle(final String title) {

    this.title = title;
  }

  @Column(name = "tags")
  public Set<String> getTags() {

    return tags;
  }

  public void setTags(final Set<String> tags) {

    this.tags = tags;
  }
}
