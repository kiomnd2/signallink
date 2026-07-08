-- Testcontainers 초기화용. 운영 스키마(app-api V1__init.sql)의 issue.disclosure 부분과 동일하게 유지할 것.
CREATE SCHEMA IF NOT EXISTS issue;

CREATE TABLE issue.disclosure (
  id           bigserial PRIMARY KEY,
  stock_code   varchar(6) NOT NULL,
  rcept_no     varchar(20) UNIQUE NOT NULL,
  title        varchar(300) NOT NULL,
  category     varchar(30),
  disclosed_at timestamptz NOT NULL,
  dart_url     varchar(300) NOT NULL
);

CREATE TABLE issue.news (
  id           bigserial PRIMARY KEY,
  stock_code   varchar(6) NOT NULL,
  title        varchar(300) NOT NULL,
  summary      text,
  url          varchar(500) NOT NULL,
  source       varchar(50),
  published_at timestamptz NOT NULL,
  UNIQUE (stock_code, url)
);
