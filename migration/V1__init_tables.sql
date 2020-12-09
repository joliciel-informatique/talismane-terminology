CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;
SET search_path = public, pg_catalog;
SET default_with_oids = false;

CREATE TABLE file (
  file_id integer NOT NULL,
  file_name character varying(256) NOT NULL,
  CONSTRAINT pk_file PRIMARY KEY (file_id),
  CONSTRAINT uk_file_name UNIQUE (file_name)
);

CREATE TABLE project (
  project_id integer NOT NULL,
  project_code character varying(256) NOT NULL,
  CONSTRAINT pk_project PRIMARY KEY (project_id)
);

CREATE TABLE projectfile (
  projectfile_project_id integer NOT NULL,
  projectfile_file_id integer NOT NULL,
  CONSTRAINT pk_projectfile PRIMARY KEY (projectfile_project_id, projectfile_file_id)
);

CREATE TABLE term (
  term_id integer NOT NULL,
  term_marked boolean DEFAULT false NOT NULL,
  term_text character varying(4000),
  term_lexical_words smallint DEFAULT 0 NOT NULL,
  CONSTRAINT pk_term PRIMARY KEY (term_id),
  CONSTRAINT uk_term UNIQUE (term_text)
);

CREATE TABLE term_expansions (
    termexp_term_id integer NOT NULL,
    termexp_expansion_id integer NOT NULL,
    CONSTRAINT pk_termexp PRIMARY KEY (termexp_term_id, termexp_expansion_id),
    CONSTRAINT fk_termexp_exp FOREIGN KEY (termexp_expansion_id) REFERENCES term(term_id),
    CONSTRAINT fk_termexp_term FOREIGN KEY (termexp_term_id) REFERENCES term(term_id)
);

CREATE TABLE term_heads (
   termhead_term_id integer NOT NULL,
   termhead_head_id integer NOT NULL,
   CONSTRAINT pk_termhead PRIMARY KEY (termhead_term_id, termhead_head_id),
   CONSTRAINT fk_termhead_head FOREIGN KEY (termhead_head_id) REFERENCES term(term_id),
   CONSTRAINT fk_termhead_term FOREIGN KEY (termhead_term_id) REFERENCES term(term_id)
);

CREATE TABLE context (
  context_id integer NOT NULL,
  context_start_row integer,
  context_start_column integer,
  context_text character varying(4000) NOT NULL,
  context_file_id integer NOT NULL,
  context_term_id integer NOT NULL,
  context_end_row integer,
  context_end_column integer,
  CONSTRAINT pk_context PRIMARY KEY (context_id),
  CONSTRAINT uk_context UNIQUE (context_file_id, context_term_id, context_start_row, context_start_column),
  CONSTRAINT fk_context_file FOREIGN KEY (context_file_id) REFERENCES file(file_id),
  CONSTRAINT fk_context_term FOREIGN KEY (context_term_id) REFERENCES term(term_id)
);

CREATE SEQUENCE seq_context_id
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE SEQUENCE seq_file_id
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE SEQUENCE seq_project_id
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE SEQUENCE seq_term_id
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;