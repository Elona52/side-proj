-- =====================================================
-- 결제 관련 테이블
-- =====================================================

-- 결제 정보 테이블
CREATE TABLE IF NOT EXISTS KNPayment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '결제 ID',
    auction_no INT NOT NULL COMMENT '경매 번호',
    member_id VARCHAR(50) NOT NULL COMMENT '구매자 회원 ID',
    
    -- 아임포트 결제 정보
    imp_uid VARCHAR(100) UNIQUE COMMENT '아임포트 고유 결제번호',
    merchant_uid VARCHAR(100) UNIQUE NOT NULL COMMENT '가맹점 주문번호',
    item_name VARCHAR(500) COMMENT '상품명',
    amount BIGINT NOT NULL COMMENT '결제 예정 금액',
    paid_amount BIGINT COMMENT '실제 결제 금액',
    payment_method VARCHAR(50) COMMENT '결제 수단 (card, trans, vbank 등)',
    pg_provider VARCHAR(50) COMMENT 'PG사 (html5_inicis, kcp 등)',
    pg_tid VARCHAR(100) COMMENT 'PG사 거래번호',
    
    -- 카드 정보
    card_name VARCHAR(50) COMMENT '카드사 명',
    card_number VARCHAR(50) COMMENT '카드 번호 (마스킹)',
    
    -- 구매자 정보
    buyer_name VARCHAR(50) COMMENT '구매자 이름',
    buyer_email VARCHAR(100) COMMENT '구매자 이메일',
    buyer_tel VARCHAR(20) COMMENT '구매자 전화번호',
    buyer_addr VARCHAR(200) COMMENT '구매자 주소',
    buyer_postcode VARCHAR(10) COMMENT '구매자 우편번호',
    
    -- 결제 상태 및 시간
    status VARCHAR(20) DEFAULT 'ready' COMMENT '결제 상태 (ready, paid, failed, cancelled)',
    paid_at TIMESTAMP COMMENT '결제 완료 시간',
    failed_at TIMESTAMP COMMENT '결제 실패 시간',
    cancelled_at TIMESTAMP COMMENT '결제 취소 시간',
    
    -- 실패/취소 사유
    fail_reason TEXT COMMENT '결제 실패 사유',
    cancel_reason TEXT COMMENT '결제 취소 사유',
    
    -- 기타
    receipt_url VARCHAR(500) COMMENT '영수증 URL',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
    
    FOREIGN KEY (auction_no) REFERENCES KNAuction(no) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES KNMember(id) ON DELETE CASCADE,
    
    INDEX idx_status (status),
    INDEX idx_member_id (member_id),
    INDEX idx_created_date (created_date)
) COMMENT '결제 정보';

-- 결제 히스토리 테이블 (로그 기록용)
CREATE TABLE IF NOT EXISTS KNPaymentHistory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '히스토리 ID',
    payment_id BIGINT NOT NULL COMMENT '결제 ID',
    status VARCHAR(20) COMMENT '결제 상태',
    action VARCHAR(50) COMMENT '액션 (결제, 취소, 환불 등)',
    description TEXT COMMENT '상세 설명',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    
    FOREIGN KEY (payment_id) REFERENCES KNPayment(id) ON DELETE CASCADE,
    INDEX idx_payment_id (payment_id),
    INDEX idx_created_date (created_date)
) COMMENT '결제 히스토리';

