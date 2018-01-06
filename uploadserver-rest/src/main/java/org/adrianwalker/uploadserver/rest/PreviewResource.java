package org.adrianwalker.uploadserver.rest;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import org.adrianwalker.uploadserver.cassandra.controller.FileSystemController;
import org.adrianwalker.uploadserver.cassandra.entity.File;
import org.adrianwalker.uploadserver.cassandra.controller.PreviewController;
import org.adrianwalker.uploadserver.cassandra.entity.Preview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/filesystem")
public final class PreviewResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(PreviewResource.class);
  private static final String SEPERATOR = "/";
  private static final String CONTENT_LENGTH = "Content-Length";
  private static final String CACHE_CONTROL = "Cache-Control";
  private static final String CACHE_CONTROL_VALUE = "public, max-age=31536000";

  private final FileSystemController fileSystemController;
  private final PreviewController previewController;

  public PreviewResource(
          final FileSystemController fileSystemController,
          final PreviewController previewController) {

    LOGGER.debug("fileSystemController = {}, previewController = {}",
            fileSystemController, previewController);

    this.fileSystemController = fileSystemController;
    this.previewController = previewController;
  }

  @GET
  @Path("/preview/{path : .+}")
  public Response preview(
          @PathParam("path")
          final String path) {

    LOGGER.debug("path = {}", path);

    if (null == path) {
      return Response.status(BAD_REQUEST).build();
    }

    File file = fileSystemController.getFile(toPath(path));

    if (null == file) {
      return Response.status(NOT_FOUND).build();
    }

    Preview preview = previewController.getPreview(file.getId());

    if (null == preview) {
      return Response.status(NOT_FOUND).build();
    }

    String mimeType = preview.getMimeType();

    if (null == mimeType) {
      return Response.status(NOT_FOUND).build();
    }

    ByteBuffer content = preview.getContent();

    if (null == content) {
      return Response.status(NOT_FOUND).build();
    }

    return Response.ok(content.array(), mimeType)
            .header(CONTENT_LENGTH, preview.getSize())
            .header(CACHE_CONTROL, CACHE_CONTROL_VALUE)
            .build();
  }

  private String toPath(final String path) {

    return Arrays.asList(path.split(SEPERATOR))
            .stream()
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(SEPERATOR, SEPERATOR, ""));
  }
}
