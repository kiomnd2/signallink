-- Testcontainers 초기화용. 운영 스키마(app-api V1·V2)의 market.stock / market.kis_token와 동일하게 유지.
CREATE SCHEMA IF NOT EXISTS market;

CREATE TABLE market.stock (
  stock_code   varchar(6) PRIMARY KEY,
  name         varchar(50) NOT NULL,
  market_type  varchar(10) NOT NULL,
  sector_code  varchar(10),
  updated_at   timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE market.kis_token (
  id           integer PRIMARY KEY,
  access_token varchar(400) NOT NULL,
  issued_at    timestamptz NOT NULL,
  expires_at   timestamptz NOT NULL
);
