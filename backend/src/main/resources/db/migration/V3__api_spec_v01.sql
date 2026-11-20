-- ============================================================
-- API 명세 v0.1 반영 (docs/Visafe_API_v01.md 부록 C)
--  1. 근로자 이름 검색(GET /workers)용 unaccent 확장
--  2. 확인번호를 블록체인 기록 중(RECORDING)에 미리 저장할 수 있게 CHECK 분리
--  3. 번역 신고 제외: 신고 항목은 ITEM_MISMATCH만, field_name 필수
-- ============================================================

-- 1) 베트남어 성조 부호를 무시하고 이름을 찾는다 (Nguyen → Nguyễn).
--    PostgreSQL 13부터 trusted 확장이라 데이터베이스에 CREATE 권한이 있으면 만들 수 있다.
CREATE EXTENSION IF NOT EXISTS unaccent;

-- 2) 블록체인에 올리는 anchor_key가 keccak256(확인번호)이므로, 확인번호는 RECORDING에 들어갈 때 정해져야 한다.
--    기록 중에 서버가 재시작되어도 같은 번호로 다시 시도할 수 있게 RECORDING에서도 저장을 허용한다.
--    확정 일시(confirmed_at)는 지금처럼 DONE·ENDED에만 둔다.
ALTER TABLE contracts DROP CONSTRAINT ck_contracts__confirmed_fields;
ALTER TABLE contracts ADD CONSTRAINT ck_contracts__confirmed_fields
  CHECK ((stage IN ('DONE', 'ENDED')) = (confirmed_at IS NOT NULL));
ALTER TABLE contracts ADD CONSTRAINT ck_contracts__confirmation_no_stage
  CHECK (CASE
           WHEN stage IN ('DONE', 'ENDED') THEN confirmation_no IS NOT NULL
           WHEN stage = 'RECORDING' THEN true
           ELSE confirmation_no IS NULL
         END);
COMMENT ON COLUMN contracts.confirmation_no IS '공개 확인번호 (VS-7K3F-9QXM). 블록체인 기록(RECORDING)을 시작할 때 정하고, DONE·ENDED에서는 필수';

-- 3) MVP에서 번역 신고를 뺐다. 신고는 원본과 다른 항목(ITEM_MISMATCH)만 받는다.
ALTER TABLE review_report_items DROP CONSTRAINT ck_review_report_items__type_field;
ALTER TABLE review_report_items ALTER COLUMN field_name SET NOT NULL;
ALTER TABLE review_report_items ADD CONSTRAINT ck_review_report_items__type_field
  CHECK (report_type = 'ITEM_MISMATCH' AND field_name IN (
    'employer_name', 'representative_name', 'employer_address', 'worker_name', 'worker_nationality',
    'start_date', 'end_date', 'work_place', 'job_description', 'work_start_time', 'work_end_time',
    'break_minutes', 'work_days_per_week', 'work_days_text', 'weekly_holiday', 'monthly_base_wage',
    'pay_day', 'pay_method', 'accommodation'));
COMMENT ON COLUMN review_report_items.report_type IS 'ITEM_MISMATCH';
COMMENT ON COLUMN review_report_items.field_name IS '신고한 항목';
