package org.adrianwalker.uploadserver.rest.element;

import java.io.Serializable;
import java.util.Collection;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "files", propOrder = {"files"})
@XmlRootElement(name = "folder")
public final class FolderElement implements Serializable {

  private Collection<FileElement> files;

  public FolderElement() {
  }

  @XmlElement(name = "files", required = true)
  public Collection<FileElement> getFiles() {

    return files;
  }

  public void setFiles(final Collection<FileElement> files) {

    this.files = files;
  }
}
