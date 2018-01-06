package org.adrianwalker.uploadserver.rest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import org.adrianwalker.uploadserver.cassandra.controller.FileSystemController;
import org.adrianwalker.uploadserver.rest.element.FileElement;
import org.adrianwalker.uploadserver.rest.element.FolderElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/filesystem")
public final class ListResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ListResource.class);
  private static final String SEPERATOR = "/";

  private final FileSystemController fileSystemController;

  public ListResource(final FileSystemController fileSystemController) {

    LOGGER.debug("fileSystemController = {}", fileSystemController);

    this.fileSystemController = fileSystemController;
  }

  @GET
  @Gzip
  @Path("/list/{path : .+}")
  public Response list(
          @PathParam("path")
          final String path) {

    LOGGER.debug("path = {}", path);

    if (null == path) {
      return Response.status(BAD_REQUEST).build();
    }

    List<FileElement> files = fileSystemController.listFiles(toPath(path))
            .stream()
            .map(file -> {

              FileElement fileElement = new FileElement();
              fileElement.setPath(path + SEPERATOR + file.getName());
              fileElement.setName(file.getName());
              fileElement.setSize(file.getSize());
              fileElement.setModified(file.getModified());
              fileElement.setDirectory(file.isDirectory());

              return fileElement;
            })
            .collect(toList());
    FolderElement folder = new FolderElement();
    folder.setFiles(files);

    return Response.ok(folder, APPLICATION_JSON).build();
  }

  private String toPath(final String path) {

    return Arrays.asList(path.split(SEPERATOR))
            .stream()
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(SEPERATOR, SEPERATOR, ""));
  }
}
