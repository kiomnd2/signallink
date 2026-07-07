-- 시그널링크 초기 스키마 (백엔드 상세스펙 v1.0 2장)

CREATE SCHEMA IF NOT EXISTS market;
CREATE SCHEMA IF NOT EXISTS issue;
CREATE SCHEMA IF NOT EXISTS macro;
CREATE SCHEMA IF NOT EXISTS insight;
CREATE SCHEMA IF NOT EXISTS content;

-- ── market ────────────────────────────────────────────
CREATE TABLE market.stock (
  stock_code    varchar(6) PRIMARY KEY,
  name          varchar(50) NOT NULL,
  market_type   varchar(10) NOT NULL,
  sector_code   varchar(10),
  updated_at    timestamptz NOT NULL DEFAULT now()
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
CREATE INDEX idx_daily_price_date ON market.daily_price (trade_date);

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
CREATE INDEX idx_investor_flow_date ON market.investor_flow (trade_date);

CREATE TABLE market.index_price (
  index_code    varchar(10) NOT NULL,
  trade_date    date NOT NULL,
  close         numeric(12,2) NOT NULL,
  change_rate   numeric(6,3) NOT NULL,
  PRIMARY KEY (index_code, trade_date)
);

CREATE TABLE market.stock_beta (
  stock_code    varchar(6) PRIMARY KEY,
  beta_60d      numeric(6,3) NOT NULL,
  calculated_at date NOT NULL
);

-- ── issue ─────────────────────────────────────────────
CREATE TABLE issue.disclosure (
  id           bigserial PRIMARY KEY,
  stock_code   varchar(6) NOT NULL,
  rcept_no     varchar(20) UNIQUE NOT NULL,
  title        varchar(300) NOT NULL,
  category     varchar(30),
  disclosed_at timestamptz NOT NULL,
  dart_url     varchar(300) NOT NULL
);
CREATE INDEX idx_disclosure_stock ON issue.disclosure (stock_code, disclosed_at);

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
CREATE INDEX idx_news_stock ON issue.news (stock_code, published_at);

-- ── macro ─────────────────────────────────────────────
CREATE TABLE macro.indicator (
  id             serial PRIMARY KEY,
  code           varchar(30) UNIQUE NOT NULL,
  name           varchar(50) NOT NULL,
  country        varchar(2) NOT NULL,
  source         varchar(20) NOT NULL,
  good_direction varchar(10) NOT NULL
);

CREATE TABLE macro.indicator_release (
  id           bigserial PRIMARY KEY,
  indicator_id int NOT NULL REFERENCES macro.indicator(id),
  release_at   timestamptz NOT NULL,
  forecast     numeric(12,3),
  previous     numeric(12,3),
  actual       numeric(12,3),
  signal       varchar(10),
  comment      varchar(300),
  UNIQUE (indicator_id, release_at)
);

CREATE TABLE macro.macro_event (
  id           bigserial PRIMARY KEY,
  event_type   varchar(30) NOT NULL,
  occurred_on  date NOT NULL,
  release_id   bigint,
  description  varchar(200),
  UNIQUE (event_type, occurred_on)
);

CREATE TABLE macro.asset_reaction (
  id           bigserial PRIMARY KEY,
  event_id     bigint NOT NULL REFERENCES macro.macro_event(id),
  asset_code   varchar(20) NOT NULL,
  d1_return    numeric(6,3),
  d7_return    numeric(6,3),
  d30_return   numeric(6,3),
  UNIQUE (event_id, asset_code)
);

CREATE TABLE macro.market_weather (
  trade_date   date PRIMARY KEY,
  kospi_change numeric(6,3) NOT NULL,
  vix          numeric(6,2),
  weather      varchar(10) NOT NULL
);

-- ── insight ───────────────────────────────────────────
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

CREATE TABLE insight.health_check (
  id              bigserial PRIMARY KEY,
  card_id         bigint UNIQUE NOT NULL REFERENCES insight.feature_card(id),
  issue_nature    varchar(15) NOT NULL,
  revenue_trend   varchar(15),
  profit_trend    varchar(15),
  flow_after      jsonb,
  updated_at      timestamptz NOT NULL DEFAULT now()
);

-- ── content ───────────────────────────────────────────
CREATE TABLE content.glossary (
  id               serial PRIMARY KEY,
  term             varchar(50) UNIQUE NOT NULL,
  easy_explanation varchar(300) NOT NULL,
  example_sentence varchar(300),
  related_terms    text[],
  category         varchar(20) NOT NULL,
  view_count       int NOT NULL DEFAULT 0
);

CREATE TABLE content.domino_rule (
  id             serial PRIMARY KEY,
  from_node      varchar(30) NOT NULL,
  to_node        varchar(30) NOT NULL,
  direction      varchar(5) NOT NULL,
  strength       smallint NOT NULL CHECK (strength BETWEEN 1 AND 3),
  explanation    varchar(300) NOT NULL,
  exception_case varchar(300) NOT NULL,
  UNIQUE (from_node, to_node)
);
