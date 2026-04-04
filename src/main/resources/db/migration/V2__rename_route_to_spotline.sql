-- Route → SpotLine 리네이밍 마이그레이션
-- Supabase PostgreSQL에서 직접 실행

ALTER TABLE routes RENAME TO spotlines;
ALTER TABLE route_spots RENAME TO spotline_spots;
ALTER TABLE user_routes RENAME TO user_spotlines;
ALTER TABLE route_likes RENAME TO spotline_likes;
ALTER TABLE route_saves RENAME TO spotline_saves;

-- FK column rename (spotline_spots 테이블)
ALTER TABLE spotline_spots RENAME COLUMN route_id TO spotline_id;

-- FK column rename (user_spotlines 테이블)
ALTER TABLE user_spotlines RENAME COLUMN route_id TO spotline_id;

-- FK column rename (spotline_likes 테이블)
ALTER TABLE spotline_likes RENAME COLUMN route_id TO spotline_id;

-- FK column rename (spotline_saves 테이블)
ALTER TABLE spotline_saves RENAME COLUMN route_id TO spotline_id;

-- spotlines 테이블: parent_route_id → parent_spotline_id
ALTER TABLE spotlines RENAME COLUMN parent_route_id TO parent_spotline_id;

-- CommentTargetType 변경: 기존 ROUTE 댓글 데이터 마이그레이션
UPDATE comments SET target_type = 'SPOTLINE' WHERE target_type = 'ROUTE';
