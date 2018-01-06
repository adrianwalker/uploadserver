package org.adrianwalker.uploadserver.cassandra.controller;

import static java.util.Collections.EMPTY_LIST;
import static java.util.stream.Collectors.toList;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

import org.adrianwalker.uploadserver.cassandra.entity.Chunk;
import org.adrianwalker.uploadserver.cassandra.entity.File;
import org.adrianwalker.uploadserver.cassandra.entity.ParentPath;
import org.adrianwalker.uploadserver.cassandra.entity.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public final class FileSystemController {

  @Accessor
  private interface ParentPathAccessor {

    @Query("SELECT path, file_id FROM parent_path WHERE path = :path")
    Result<ParentPath> selectParentPathByPath(@Param("path") String path);
  }

  @Accessor
  private interface FileAccessor {

    @Query("SELECT id, name, size, modified, group, owner, hidden, directory "
            + "FROM file WHERE id IN :ids")
    Result<File> selectFilesByIds(@Param("ids") List<UUID> ids);
  }

  @Accessor
  private interface ChunkAccessor {

    @Query("SELECT file_id, chunk_number, content FROM chunk WHERE file_id = :file_id")
    Result<Chunk> selectChunksByFileId(@Param("file_id") UUID fileId);

    @Query("DELETE FROM chunk WHERE file_id = :file_id")
    void deleteChunksByFileId(@Param("file_id") UUID fileId);
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemController.class);
  private static final int CHUNK_SIZE = 1 * 1024 * 1024;
  private static final int BYTE_MASK = 0xFF;

  private final Mapper<ParentPath> parentPathMapper;
  private final Mapper<Path> pathMapper;
  private final Mapper<File> fileMapper;
  private final Mapper<Chunk> chunkMapper;

  private final ParentPathAccessor parentPathAccessor;
  private final FileAccessor fileAccessor;
  private final ChunkAccessor chunkAccessor;

  public FileSystemController(final Session session) {

    LOGGER.debug("session = {}", session);

    if (null == session) {
      throw new IllegalArgumentException("session is null");
    }

    MappingManager manager = new MappingManager(session);

    parentPathMapper = manager.mapper(ParentPath.class);
    pathMapper = manager.mapper(Path.class);
    fileMapper = manager.mapper(File.class);
    chunkMapper = manager.mapper(Chunk.class);

    parentPathAccessor = manager.createAccessor(ParentPathAccessor.class);
    fileAccessor = manager.createAccessor(FileAccessor.class);
    chunkAccessor = manager.createAccessor(ChunkAccessor.class);
  }

  public File getFile(final String path) {

    LOGGER.debug("path = {}", path);

    if (null == path) {
      throw new IllegalArgumentException("path is null");
    }

    Path filePath = pathMapper.get(path);
    if (null == filePath) {
      return null;
    }

    return fileMapper.get(filePath.getFileId());
  }

  public boolean saveFile(final String path, final File file) {

    LOGGER.debug("path = {}, file = {}", path, file);

    if (null == path) {
      throw new IllegalArgumentException("path is null");
    }

    if (null == file) {
      throw new IllegalArgumentException("file is null");
    }

    if (null == file.getId()) {
      file.setId(UUID.randomUUID());
    }

    pathMapper.save(new Path(path, file.getId()));
    parentPathMapper.save(new ParentPath(getParent(path), file.getId()));
    fileMapper.save(file);

    return true;
  }

  public boolean deleteFile(final String path) {

    LOGGER.debug("path = {}", path);

    if (null == path) {
      throw new IllegalArgumentException("path is null");
    }

    File file = getFile(path);

    if (null == file) {
      return false;
    }

    pathMapper.delete(path);

    String parentPath = getParent(path);
    parentPathMapper.delete(parentPath, file.getId());

    chunkAccessor.deleteChunksByFileId(file.getId());

    fileMapper.delete(file.getId());

    return true;
  }

  public List<File> listFiles(final String parentPath) {

    LOGGER.debug("parentPath = {}", parentPath);

    if (null == parentPath) {
      throw new IllegalArgumentException("parentPath is null");
    }

    List<UUID> ids = getFileIds(parentPath);

    List<File> files;
    if (ids.isEmpty()) {
      files = EMPTY_LIST;
    } else {
      files = fileAccessor.selectFilesByIds(ids).all();
    }

    return files;
  }

  public boolean moveFile(final String fromPath, final String toPath) {

    LOGGER.debug("fromPath = {}, toPath = {}", fromPath, toPath);

    if (null == fromPath) {
      throw new IllegalArgumentException("fromPath is null");
    }

    if (null == toPath) {
      throw new IllegalArgumentException("toPath is null");
    }

    File file = getFile(fromPath);

    if (null == file) {
      return false;
    }

    String toParentPath = getParent(toPath);
    pathMapper.save(new Path(toPath, file.getId()));
    parentPathMapper.save(new ParentPath(toParentPath, file.getId()));

    String fromParentPath = getParent(fromPath);

    if (!fromPath.equals(toPath)) {
      pathMapper.delete(fromPath);
    }

    if (!fromParentPath.equals(toParentPath)) {
      parentPathMapper.delete(fromParentPath, file.getId());
    }

    file.setName(getFileName(toPath));
    fileMapper.save(file);

    return true;
  }

  public OutputStream createOutputStream(final File file) {

    LOGGER.debug("file = {}", file);

    if (null == file) {
      throw new IllegalArgumentException("file is null");
    }

    UUID fileId = file.getId();

    chunkAccessor.deleteChunksByFileId(fileId);

    return new OutputStream() {

      private Chunk chunk = null;
      private int chunkNumber = 0;
      private long bytesWritten = 0;

      @Override
      public void write(final int b) throws IOException {

        if (null == chunk) {
          chunk = new Chunk();
          chunk.setFileId(fileId);
          chunk.setChunkNumber(chunkNumber);
          chunk.setContent(ByteBuffer.allocate(CHUNK_SIZE));
        }

        ByteBuffer content = chunk.getContent();
        content.put((byte) (b & BYTE_MASK));

        if (content.remaining() == 0) {
          save(content);
        }
      }

      @Override
      public void close() throws IOException {

        if (null != chunk) {
          ByteBuffer content = chunk.getContent();
          save(content);
        }

        file.setSize(bytesWritten);
        fileMapper.save(file);
      }

      private void save(final ByteBuffer content) {

        content.flip();
        chunkMapper.save(chunk);

        chunk = null;
        chunkNumber++;
        bytesWritten += content.limit();
      }
    };
  }

  public InputStream createInputStream(final File file) {

    LOGGER.debug("file = {}", file);

    if (null == file) {
      throw new IllegalArgumentException("file is null");
    }

    UUID fileId = file.getId();

    return new InputStream() {

      private Chunk chunk = null;
      private int chunkNumber = 0;
      private long bytesRead = 0;

      @Override
      public int read() throws IOException {

        if (bytesRead == file.getSize()) {
          return -1;
        }

        if (null == chunk) {
          chunk = chunkMapper.get(fileId, chunkNumber);
        }

        ByteBuffer content = chunk.getContent();
        byte b = content.get();

        if (content.remaining() == 0) {
          chunk = null;
          chunkNumber++;
          bytesRead += content.position();
        }

        return b & BYTE_MASK;
      }

      @Override
      public int read(final byte[] b, final int off, final int len) throws IOException {

        if (bytesRead == file.getSize()) {
          return -1;
        }

        if (null == chunk) {
          chunk = chunkMapper.get(fileId, chunkNumber);
        }

        ByteBuffer content = chunk.getContent();

        int remaining = content.remaining();
        if (len > remaining) {
          content.get(b, off, remaining);
        } else {
          content.get(b, off, len);
        }

        if (content.remaining() == 0) {
          chunk = null;
          chunkNumber++;
          bytesRead += content.position();
        }

        if (len > remaining) {
          return remaining;
        } else {
          return len;
        }
      }
    };
  }

  private List<UUID> getFileIds(final String parentPath) {

    return parentPathAccessor.selectParentPathByPath(parentPath)
            .all()
            .stream()
            .map(pp -> pp.getFileId())
            .collect(toList());
  }

  private String getParent(final String path) {

    java.nio.file.Path parent = Paths.get(path).getParent();

    if (null == parent) {
      throw new IllegalArgumentException("invalid path");
    }

    return parent.toString();
  }

  private String getFileName(final String path) {

    java.nio.file.Path fileName = Paths.get(path).getFileName();

    if (null == fileName) {
      throw new IllegalArgumentException("invalid path");
    }

    return fileName.toString();
  }
}
