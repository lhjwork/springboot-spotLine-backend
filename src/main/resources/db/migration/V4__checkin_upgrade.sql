-- Checkin 업그레이드: 다회 방문 기록 지원
-- 1. UNIQUE 제약 제거 (다회 체크인 허용)
ALTER TABLE spot_visits DROP CONSTRAINT IF EXISTS uq_spot_visit_user_spot;

-- 2. 새 컬럼 추가
ALTER TABLE spot_visits ADD COLUMN memo VARCHAR(100);
ALTER TABLE spot_visits ADD COLUMN verified BOOLEAN NOT NULL DEFAULT false;

-- 3. 복합 인덱스 (유저+Spot+날짜 조회 최적화)
CREATE INDEX idx_spot_visit_user_spot_created
  ON spot_visits(user_id, spot_id, created_at DESC);
