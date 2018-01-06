package org.adrianwalker.uploadserver.rest;

import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

@Provider
@Gzip
public class GzipWriterInterceptor implements WriterInterceptor {

  private static final String CONTENT_ENCODING = "Content-Encoding";
  private static final String CONTENT_ENCODING_GZIP = "gzip";

  @Override
  public void aroundWriteTo(final WriterInterceptorContext context)
          throws IOException, WebApplicationException {

    GZIPOutputStream os = new GZIPOutputStream(context.getOutputStream());
    context.getHeaders().putSingle(CONTENT_ENCODING, CONTENT_ENCODING_GZIP);
    context.setOutputStream(os);
    context.proceed();
  }
}
