-- Spot Visit (체크인) 테이블
CREATE TABLE spot_visits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    spot_id UUID NOT NULL REFERENCES spots(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT now(),
    CONSTRAINT uq_spot_visit_user_spot UNIQUE (user_id, spot_id)
);

CREATE INDEX idx_spot_visit_user ON spot_visits(user_id);
CREATE INDEX idx_spot_visit_spot ON spot_visits(spot_id);
CREATE INDEX idx_spot_visit_created ON spot_visits(created_at DESC);

-- Spot 테이블에 visited_count 컬럼 추가
ALTER TABLE spots ADD COLUMN visited_count INTEGER DEFAULT 0;
