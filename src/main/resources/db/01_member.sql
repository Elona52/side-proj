-- =====================================================
-- 회원 관련 테이블
-- =====================================================

-- 회원 테이블
CREATE TABLE IF NOT EXISTS KNMember (
    id VARCHAR(50) PRIMARY KEY COMMENT '회원 ID',
    pass VARCHAR(500) COMMENT '비밀번호 (암호화)',
    name VARCHAR(50) COMMENT '회원 이름',
    phone VARCHAR(20) COMMENT '전화번호',
    mail VARCHAR(100) COMMENT '이메일',
    zipcode INT COMMENT '우편번호',
    address1 VARCHAR(200) COMMENT '주소 1',
    address2 VARCHAR(200) COMMENT '상세주소',
    marketing VARCHAR(10) COMMENT '마케팅 수신 동의',
    joindate TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '가입일',
    modificationdate TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
    type VARCHAR(20) COMMENT '회원 타입 (USER, ADMIN)'
) COMMENT '회원 정보';

