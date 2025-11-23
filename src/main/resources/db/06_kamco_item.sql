-- =====================================================
-- 캠코 온비드 공매 물건 관련 테이블들 (PostgreSQL)
-- =====================================================

-- 캠코 온비드 공매 물건 테이블 (온비드 API 데이터 저장)
-- PostgreSQL에서는 따옴표로 감싸야 대소문자 유지
CREATE TABLE IF NOT EXISTS "KNKamcoItem" (
    id BIGSERIAL PRIMARY KEY,
    
    -- 기본 식별 정보
    rnum INT,
    plnm_no VARCHAR(100),
    pbct_no VARCHAR(100),
    org_base_no VARCHAR(100),
    org_nm VARCHAR(150),
    cltr_no VARCHAR(100) NOT NULL UNIQUE,
    pbct_cdtn_no VARCHAR(100),
    cltr_mnmt_no VARCHAR(100),
    cltr_hstr_no VARCHAR(100),
    bid_mnmt_no VARCHAR(100),
    
    -- 분류 정보
    scrn_grp_cd VARCHAR(50),
    ctgr_id VARCHAR(50),
    ctgr_full_nm VARCHAR(500),
    
    -- 물건 정보
    cltr_nm VARCHAR(500),
    goods_nm TEXT,
    manf VARCHAR(200),
    
    -- 주소 정보
    ldnm_adrs VARCHAR(500),
    nmrd_adrs VARCHAR(500),
    rod_nm VARCHAR(120),
    bld_no VARCHAR(60),
    sido VARCHAR(50),
    
    -- 처분/입찰 방식
    dpsl_mtd_cd VARCHAR(20),
    dpsl_mtd_nm VARCHAR(100),
    bid_mtd_nm VARCHAR(200),
    
    -- 가격 정보
    min_bid_prc BIGINT,
    apsl_ases_avg_amt BIGINT,
    fee_rate VARCHAR(50),
    
    -- 입찰 일정
    pbct_begn_dtm VARCHAR(20),
    pbct_cls_dtm VARCHAR(20),
    
    -- 상태 정보
    pbct_cltr_stat_nm VARCHAR(100),
    uscb_cnt INT DEFAULT 0,
    iqry_cnt INT DEFAULT 0,
    
    -- 통계 정보 (자체 관리)
    view_count INT DEFAULT 0,
    interest_count INT DEFAULT 0,
    
    -- API 데이터 동기화 정보
    is_new BOOLEAN DEFAULT true,
    is_active BOOLEAN DEFAULT true,
    api_sync_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 관리 일시
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_cltr_no ON "KNKamcoItem"(cltr_no);
CREATE INDEX IF NOT EXISTS idx_pbct_cls_dtm ON "KNKamcoItem"(pbct_cls_dtm);
CREATE INDEX IF NOT EXISTS idx_sido ON "KNKamcoItem"(sido);
CREATE INDEX IF NOT EXISTS idx_is_new ON "KNKamcoItem"(is_new);
CREATE INDEX IF NOT EXISTS idx_view_count ON "KNKamcoItem"(view_count);
CREATE INDEX IF NOT EXISTS idx_interest_count ON "KNKamcoItem"(interest_count);
CREATE INDEX IF NOT EXISTS idx_min_bid_prc ON "KNKamcoItem"(min_bid_prc);

-- updated_date 자동 업데이트를 위한 트리거 함수
CREATE OR REPLACE FUNCTION update_updated_date_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_date = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- updated_date 자동 업데이트 트리거
CREATE TRIGGER update_knkamcoitem_updated_date BEFORE UPDATE ON "KNKamcoItem"
    FOR EACH ROW EXECUTE FUNCTION update_updated_date_column();

-- 캠코 온비드 공매 물건 조회 이력 테이블
CREATE TABLE IF NOT EXISTS "KNKamcoItemViewLog" (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL,
    cltr_no VARCHAR(100) NOT NULL,
    member_id VARCHAR(50),
    ip_address VARCHAR(50),
    user_agent TEXT,
    view_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_viewlog_item FOREIGN KEY (item_id) REFERENCES "KNKamcoItem"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_viewlog_item_id ON "KNKamcoItemViewLog"(item_id);
CREATE INDEX IF NOT EXISTS idx_viewlog_view_date ON "KNKamcoItemViewLog"(view_date);

-- 새로운 물건 공지사항 테이블
CREATE TABLE IF NOT EXISTS "KNNewItemNotification" (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL,
    cltr_no VARCHAR(100) NOT NULL,
    cltr_nm VARCHAR(500),
    min_bid_prc BIGINT,
    pbct_cls_dtm VARCHAR(20),
    notification_type VARCHAR(20) DEFAULT 'NEW',
    is_displayed BOOLEAN DEFAULT true,
    display_order INT DEFAULT 0,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expired_date TIMESTAMP,
    
    CONSTRAINT fk_notification_item FOREIGN KEY (item_id) REFERENCES "KNKamcoItem"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_notification_is_displayed ON "KNNewItemNotification"(is_displayed);
CREATE INDEX IF NOT EXISTS idx_notification_created_date ON "KNNewItemNotification"(created_date);
CREATE INDEX IF NOT EXISTS idx_notification_type ON "KNNewItemNotification"(notification_type);

-- 물건 통계 스냅샷 테이블 (일별/주별 통계용)
CREATE TABLE IF NOT EXISTS "KNKamcoItemStats" (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL,
    cltr_no VARCHAR(100) NOT NULL,
    stat_date DATE NOT NULL,
    view_count INT DEFAULT 0,
    interest_count INT DEFAULT 0,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_stats_item FOREIGN KEY (item_id) REFERENCES "KNKamcoItem"(id) ON DELETE CASCADE,
    CONSTRAINT unique_item_stat UNIQUE (item_id, stat_date)
);

CREATE INDEX IF NOT EXISTS idx_stats_stat_date ON "KNKamcoItemStats"(stat_date);
