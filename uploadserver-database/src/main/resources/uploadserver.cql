CREATE TABLE uploadserver.file (
  id uuid,
  name text,
  size bigint,
  modified bigint,
  group text,
  owner text,
  hidden boolean,
  directory boolean,
  PRIMARY KEY (id)
);

CREATE TABLE uploadserver.chunk (
  file_id uuid,
  chunk_number int,
  content blob,
  PRIMARY KEY (file_id, chunk_number)
);

CREATE TABLE uploadserver.path (
  path text,
  file_id uuid,
  PRIMARY KEY (path)
);

CREATE TABLE uploadserver.parent_path (
  path text,
  file_id uuid,
  PRIMARY KEY (path, file_id)
);

CREATE TABLE uploadserver.metadata (
  id uuid,
  mime_type text,
  title text,
  tags set<text>,
  PRIMARY KEY (id)
);

CREATE TABLE uploadserver.preview (
  id uuid,
  size bigint,
  content blob,
  mime_type text,
  PRIMARY KEY (id)
);
