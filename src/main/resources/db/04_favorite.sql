-- =====================================================
-- 즐겨찾기 / 가격 알림 관련 테이블
-- =====================================================

-- 즐겨찾기 테이블 (User_Favorite)
CREATE TABLE IF NOT EXISTS User_Favorite (
    favorite_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '즐겨찾기 ID',
    user_id VARCHAR(50) NOT NULL COMMENT '회원 ID (FK -> KNMember.id)',
    item_id BIGINT NOT NULL COMMENT '물건 ID (FK -> KNKamcoItem.id)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    
    UNIQUE KEY unique_user_favorite (user_id, item_id),
    FOREIGN KEY (user_id) REFERENCES KNMember(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES KNKamcoItem(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_item_id (item_id)
) COMMENT '사용자 즐겨찾기';

-- 가격 알림 히스토리 테이블
CREATE TABLE IF NOT EXISTS KNPriceAlert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알림 ID',
    favorite_id BIGINT NOT NULL COMMENT '즐겨찾기 ID',
    member_id VARCHAR(50) NOT NULL COMMENT '회원 ID',
    item_plnm_no VARCHAR(100) NOT NULL COMMENT '물건 공고번호',
    previous_price BIGINT COMMENT '이전 가격',
    new_price BIGINT COMMENT '새 가격',
    alert_sent TINYINT(1) DEFAULT 0 COMMENT '알림 전송 여부',
    sent_date TIMESTAMP COMMENT '전송일',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    
    FOREIGN KEY (favorite_id) REFERENCES User_Favorite(favorite_id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES KNMember(id) ON DELETE CASCADE,
    INDEX idx_favorite_id (favorite_id),
    INDEX idx_member_id (member_id),
    INDEX idx_created_date (created_date)
) COMMENT '가격 알림 히스토리';

