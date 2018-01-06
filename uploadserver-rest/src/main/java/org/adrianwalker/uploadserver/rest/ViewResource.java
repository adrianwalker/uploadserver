package org.adrianwalker.uploadserver.rest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.stream.Collectors;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;


import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.ws.rs.core.StreamingOutput;
import org.adrianwalker.uploadserver.cassandra.controller.FileSystemController;
import org.adrianwalker.uploadserver.cassandra.entity.File;
import org.adrianwalker.uploadserver.cassandra.controller.MetadataController;
import org.adrianwalker.uploadserver.cassandra.entity.Metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/filesystem")
public final class ViewResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ViewResource.class);
  private static final String SEPERATOR = "/";
  private static final int BUFFER_SIZE = 8 * 1024;
  private static final String CONTENT_LENGTH = "Content-Length";
  private static final String CACHE_CONTROL = "Cache-Control";
  private static final String CACHE_CONTROL_VALUE = "public, max-age=31536000";

  private final FileSystemController fileSystemController;
  private final MetadataController metadataController;

  public ViewResource(
          final FileSystemController fileSystemController,
          final MetadataController metadataController) {

    LOGGER.debug("fileSystemController = {}, metadataController = {}",
            fileSystemController, metadataController);

    this.fileSystemController = fileSystemController;
    this.metadataController = metadataController;
  }

  @GET
  @Path("/view{path : .+}")
  public Response view(
          @PathParam("path")
          final String path,
          @QueryParam("height") final int height,
          @QueryParam("width") final int width) {

    LOGGER.debug("path = {}, height = {}, width = {}", path, height, width);

    if (null == path) {
      return Response.status(BAD_REQUEST).build();
    }

    File file = fileSystemController.getFile(toPath(path));

    if (null == file) {
      return Response.status(NOT_FOUND).build();
    }

    Metadata metadata = metadataController.getMetadata(file.getId());

    if (null == metadata) {
      return Response.status(NOT_FOUND).build();
    }

    String mimeType = metadata.getMimeType();

    if (null == mimeType) {
      return Response.status(NOT_FOUND).build();
    }

    StreamingOutput so = os -> {
      try (InputStream is = new BufferedInputStream(fileSystemController.createInputStream(file))) {
        byte[] bytes = new byte[BUFFER_SIZE];
        copyStream(bytes, is, os);
      }
    };

    return Response.ok(so, mimeType)
            .header(CONTENT_LENGTH, file.getSize())
            .header(CACHE_CONTROL, CACHE_CONTROL_VALUE)
            .build();
  }

  private String toPath(final String path) {

    return Arrays.asList(path.split(SEPERATOR))
            .stream()
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(SEPERATOR, SEPERATOR, ""));
  }

  private void copyStream(
          final byte[] bytes,
          final InputStream inputStream, final OutputStream outputStream)
          throws IOException {

    while ((inputStream.read(bytes)) != -1) {
      outputStream.write(bytes);
      outputStream.flush();
    }
  }
}
