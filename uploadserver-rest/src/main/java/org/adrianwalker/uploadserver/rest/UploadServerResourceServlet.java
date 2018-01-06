package org.adrianwalker.uploadserver.rest;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.ApplicationPath;
import org.adrianwalker.uploadserver.cassandra.controller.FileSystemController;
import org.adrianwalker.uploadserver.cassandra.controller.MetadataController;
import org.adrianwalker.uploadserver.cassandra.controller.PreviewController;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(loadOnStartup = 1)
@ApplicationPath("uploadserver")
public final class UploadServerResourceServlet extends ResourceConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(UploadServerResourceServlet.class);
  private static final String PROPERTIES = "uploadserver-rest.properties";

  public UploadServerResourceServlet() throws IOException {

    super();

    UploadServerConfiguration configuration = readConfiguration();

    Cluster cluster = new Cluster.Builder()
            .addContactPoints(configuration.getCassandraHost())
            .withPort(configuration.getCassandraPort())
            .build();

    Session session = cluster.connect(configuration.getCassandraKeyspace());

    FileSystemController fileSystemController = new FileSystemController(session);
    MetadataController metadataController = new MetadataController(session);
    PreviewController previewController = new PreviewController(session);

    ListResource listResource = new ListResource(fileSystemController);
    PreviewResource previewResource = new PreviewResource(fileSystemController, previewController);
    ViewResource viewResource = new ViewResource(fileSystemController, metadataController);

    registerInstances(listResource, previewResource, viewResource);
    register(GzipWriterInterceptor.class);
  }

  private UploadServerConfiguration readConfiguration() throws IOException {

    UploadServerConfiguration configuration = new UploadServerConfiguration();

    try (InputStream is = getClassLoader().getResourceAsStream(PROPERTIES)) {
      configuration.load(is);
    } catch (final IOException ioe) {
      LOGGER.error(ioe.getMessage(), ioe);
      throw ioe;
    }

    return configuration;
  }
}
