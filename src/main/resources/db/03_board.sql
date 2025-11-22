-- =====================================================
-- 게시판 관련 테이블
-- =====================================================

-- 게시판 테이블
CREATE TABLE IF NOT EXISTS KNfind (
    no INT AUTO_INCREMENT PRIMARY KEY COMMENT '게시글 번호',
    id VARCHAR(50) COMMENT '작성자 ID',
    title VARCHAR(200) COMMENT '제목',
    content TEXT COMMENT '내용',
    category VARCHAR(20) DEFAULT 'other' COMMENT '카테고리 (real-estate:부동산, movable:동산, site:사이트, other:기타)',
    views INT DEFAULT 0 COMMENT '조회수',
    related_link VARCHAR(500) COMMENT '관련링크',
    reg_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '작성일',
    
    -- 외래키: 작성자는 반드시 회원이어야 함
    FOREIGN KEY (id) REFERENCES KNMember(id) ON DELETE CASCADE
) COMMENT '게시판';

-- 댓글 테이블
CREATE TABLE IF NOT EXISTS KNReply (
    no INT AUTO_INCREMENT PRIMARY KEY COMMENT '댓글 번호',
    id VARCHAR(50) COMMENT '작성자 ID',
    content TEXT COMMENT '댓글 내용',
    reg_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '작성일',
    board_no INT COMMENT '게시글 번호',
    
    -- 외래키: 댓글은 게시글에 속함
    FOREIGN KEY (board_no) REFERENCES KNfind(no) ON DELETE CASCADE,
    -- 외래키: 댓글 작성자는 반드시 회원이어야 함
    FOREIGN KEY (id) REFERENCES KNMember(id) ON DELETE CASCADE
) COMMENT '댓글';

