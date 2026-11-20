-- ============================================================
-- 데모 계정 시드 (local·dev 프로필에서만 실행, prod 제외)
--  - spring.flyway.locations에 classpath:db/seed가 있는 프로필에서만 적용된다.
--  - 반복 마이그레이션(R__)이라 이 파일을 고치면 다시 실행된다. 모든 INSERT는 이미 있으면 건너뛴다.
--  - 모든 계정의 비밀번호: visafe-demo-1234 (BCrypt)
--  - 사업체·사람·번호는 모두 가상이다. 이메일은 예약 도메인(.test)을 쓴다.
-- ============================================================

-- 사업주
INSERT INTO companies (name, representative_name, business_reg_no, address)
VALUES ('한빛정밀', '김민수', '000-00-00001', '경기도 안산시 단원구 데모로 1')
ON CONFLICT (business_reg_no) DO NOTHING;

-- 고용주 1명, 관리자 1명
INSERT INTO members (login_email, password_hash, name, role, company_id, ui_language)
SELECT 'employer@demo.visafe.test', '$2a$10$5s1oodRVZIyAO6hSd8uUoe/LqCN.KriwzvUxPTq5FtTG4c5OvNEDe', '김민수', 'EMPLOYER', c.id, 'ko'
FROM companies c WHERE c.business_reg_no = '000-00-00001'
ON CONFLICT (lower(login_email)) DO NOTHING;

INSERT INTO members (login_email, password_hash, name, role, company_id, ui_language)
VALUES ('admin@demo.visafe.test', '$2a$10$5s1oodRVZIyAO6hSd8uUoe/LqCN.KriwzvUxPTq5FtTG4c5OvNEDe', '감독기관 담당자', 'ADMIN', NULL, 'ko')
ON CONFLICT (lower(login_email)) DO NOTHING;

-- 근로자 3명. worker1·worker2는 이름이 같아 생년월일과 외국인등록번호 뒷자리로 구분한다. (근로자 검색 확인용)
INSERT INTO members (login_email, password_hash, name, role, company_id, ui_language)
VALUES
  ('worker1@demo.visafe.test', '$2a$10$5s1oodRVZIyAO6hSd8uUoe/LqCN.KriwzvUxPTq5FtTG4c5OvNEDe', 'Nguyễn Văn An', 'WORKER', NULL, 'vi'),
  ('worker2@demo.visafe.test', '$2a$10$5s1oodRVZIyAO6hSd8uUoe/LqCN.KriwzvUxPTq5FtTG4c5OvNEDe', 'Nguyễn Văn An', 'WORKER', NULL, 'vi'),
  ('worker3@demo.visafe.test', '$2a$10$5s1oodRVZIyAO6hSd8uUoe/LqCN.KriwzvUxPTq5FtTG4c5OvNEDe', 'Lê Thị Hoa', 'WORKER', NULL, 'vi')
ON CONFLICT (lower(login_email)) DO NOTHING;

-- 근로자 정보. 외국인등록번호 암호문·검색 해시는 아직 암호화 방식이 없어 자리만 채운 가짜 값이다.
-- (API는 뒷자리 4개만 보여 준다. 암호화를 구현하면 이 값도 다시 만든다)
INSERT INTO member_worker_profiles (member_id, nationality, birth_date, preferred_language,
                                    alien_reg_no_enc, alien_reg_no_hash, alien_reg_no_last4)
SELECT m.id, v.nationality, v.birth_date::date, 'vi',
       convert_to('demo-only:' || v.email, 'UTF8'),
       '0x' || encode(sha256(convert_to('demo-alien-reg-no:' || v.email, 'UTF8')), 'hex'),
       v.last4
FROM (VALUES
        ('worker1@demo.visafe.test', 'VN', '1998-04-17', '4821'),
        ('worker2@demo.visafe.test', 'VN', '2001-09-03', '1357'),
        ('worker3@demo.visafe.test', 'VN', '1996-12-25', '2468')
     ) AS v (email, nationality, birth_date, last4)
JOIN members m ON lower(m.login_email) = v.email
ON CONFLICT (member_id) DO NOTHING;
