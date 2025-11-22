-- =====================================================
-- 경매 관련 테이블
-- =====================================================

-- 경매 테이블
CREATE TABLE IF NOT EXISTS KNAuction (
    no INT AUTO_INCREMENT PRIMARY KEY COMMENT '경매 번호',
    id VARCHAR(50) COMMENT '판매자 ID',
    name VARCHAR(200) COMMENT '경매 물품명',
    content TEXT COMMENT '물품 설명',
    regdate TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '등록일',
    startdate DATE COMMENT '경매 시작일',
    enddate DATE COMMENT '경매 종료일',
    startprice INT COMMENT '시작 가격',
    endprice INT COMMENT '현재 최고가',
    img VARCHAR(500) COMMENT '이미지 파일명',
    buyer VARCHAR(50) COMMENT '낙찰자 ID',
    count INT DEFAULT 0 COMMENT '조회수',
    bidder JSON COMMENT '입찰자 정보 (JSON)',
    deposit_status TINYINT(1) DEFAULT 0 COMMENT '입금 여부',
    deposit_date DATE COMMENT '입금일',
    delivery_status TINYINT(1) DEFAULT 0 COMMENT '배송 여부',
    delivery_date DATE COMMENT '배송일',
    remit_status TINYINT(1) DEFAULT 0 COMMENT '송금 여부',
    remit_date DATE COMMENT '송금일',
    
    -- 외래키: 판매자는 반드시 회원이어야 함
    FOREIGN KEY (id) REFERENCES KNMember(id) ON DELETE CASCADE,
    -- 외래키: 낙찰자도 회원이어야 함 (NULL 허용: 아직 낙찰되지 않은 경우)
    FOREIGN KEY (buyer) REFERENCES KNMember(id) ON DELETE SET NULL
) COMMENT '경매 정보';

