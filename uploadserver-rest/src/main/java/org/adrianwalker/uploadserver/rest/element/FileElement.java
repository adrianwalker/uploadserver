package org.adrianwalker.uploadserver.rest.element;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "file", propOrder = {
  "path",
  "name",
  "size",
  "modified",
  "directory"
})
public final class FileElement implements Serializable {

  private String path;
  private String name;
  private long size;
  private long modified;
  private boolean directory;

  public FileElement() {
  }

  @XmlElement(name = "path", required = true)
  public String getPath() {

    return path;
  }

  public void setPath(final String path) {

    this.path = path;
  }

  @XmlElement(name = "name", required = true)
  public String getName() {

    return name;
  }

  public void setName(final String name) {

    this.name = name;
  }

  @XmlElement(name = "size", required = true)
  public long getSize() {

    return size;
  }

  public void setSize(final long size) {

    this.size = size;
  }

  @XmlElement(name = "modified", required = true)
  public long getModified() {

    return modified;
  }

  public void setModified(final long modified) {

    this.modified = modified;
  }

  @XmlElement(name = "directory", required = true)
  public boolean isDirectory() {

    return directory;
  }

  public void setDirectory(final boolean directory) {

    this.directory = directory;
  }
}
