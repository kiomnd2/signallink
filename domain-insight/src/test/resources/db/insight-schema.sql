-- Testcontainers 초기화용. 운영 스키마(app-api V1__init.sql)의 insight.feature_card 부분과 동일하게 유지할 것.
CREATE SCHEMA IF NOT EXISTS insight;

CREATE TABLE insight.feature_card (
  id              bigserial PRIMARY KEY,
  stock_code      varchar(6) NOT NULL,
  trade_date      date NOT NULL,
  change_rate     numeric(6,3) NOT NULL,
  trigger_type    varchar(10) NOT NULL,
  market_contrib  numeric(6,3),
  sector_sync     boolean,
  what_happened   text,
  flow_summary    text,
  context_note    text,
  source_refs     jsonb,
  llm_used        boolean NOT NULL DEFAULT false,
  status          varchar(12) NOT NULL,
  created_at      timestamptz NOT NULL DEFAULT now(),
  UNIQUE (stock_code, trade_date)
);
CREATE INDEX idx_feature_card_date ON insight.feature_card (trade_date);
