-- KIS 액세스 토큰 캐시 (단일 행, id=1). tokenP는 재발급 제한이 있어 재시작에도 재사용한다.
CREATE TABLE market.kis_token (
  id           integer PRIMARY KEY,
  access_token varchar(400) NOT NULL,
  issued_at    timestamptz NOT NULL,
  expires_at   timestamptz NOT NULL
);
