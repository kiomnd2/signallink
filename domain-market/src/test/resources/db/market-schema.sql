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

CREATE TABLE market.daily_price (
  id            bigserial PRIMARY KEY,
  stock_code    varchar(6) NOT NULL,
  trade_date    date NOT NULL,
  close         numeric(12,2) NOT NULL,
  change_rate   numeric(6,3) NOT NULL,
  volume        bigint NOT NULL,
  trading_value bigint NOT NULL,
  vol_ratio_20d numeric(6,2),
  is_final      boolean NOT NULL DEFAULT false,
  UNIQUE (stock_code, trade_date)
);

CREATE TABLE market.index_price (
  index_code    varchar(10) NOT NULL,
  trade_date    date NOT NULL,
  close         numeric(12,2) NOT NULL,
  change_rate   numeric(6,3) NOT NULL,
  PRIMARY KEY (index_code, trade_date)
);

CREATE TABLE market.investor_flow (
  id            bigserial PRIMARY KEY,
  stock_code    varchar(6) NOT NULL,
  trade_date    date NOT NULL,
  investor_type varchar(20) NOT NULL,
  net_buy_qty   bigint NOT NULL,
  net_buy_amt   bigint NOT NULL,
  is_final      boolean NOT NULL DEFAULT false,
  UNIQUE (stock_code, trade_date, investor_type)
);
