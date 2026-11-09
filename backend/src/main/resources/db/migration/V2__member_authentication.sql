-- 기존 학습 회원의 데이터를 보존한다. 해시가 없는 회원은 로그인할 수 없다.
ALTER TABLE members ADD COLUMN password_hash VARCHAR(100);
