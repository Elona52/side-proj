package com.api.item.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 캠코 온비드 공매 물건 도메인
 * 온비드 API 데이터를 DB에 저장하기 위한 엔티티
 */
@Data
public class KamcoItem {
    // PK
    private Long id;
    
    // 기본 식별 정보
    private Integer rnum;               // 순번
    private String plnmNo;              // 공고번호
    private String pbctNo;              // 공매번호
    private String orgBaseNo;           // 기관기준번호
    private String orgNm;               // 기관명
    private String cltrNo;              // 물건번호 (고유키)
    private String pbctCdtnNo;          // 공매조건번호
    private String cltrMnmtNo;          // 물건관리번호
    private String cltrHstrNo;          // 물건이력번호
    private String bidMnmtNo;           // 입찰관리번호
    
    // 분류 정보
    private String scrnGrpCd;           // 화면그룹코드
    private String ctgrId;              // 카테고리ID
    private String ctgrFullNm;          // 카테고리 전체명
    
    // 물건 정보
    private String cltrNm;              // 물건명
    private String goodsNm;             // 물건내역
    private String manf;                // 제조사
    
    // 주소 정보
    private String ldnmAdrs;            // 지번주소
    private String nmrdAdrs;            // 도로명주소
    private String rodNm;               // 도로명
    private String bldNo;               // 건물번호
    private String sido;                // 시도 (검색용)
    
    // 처분/입찰 방식
    private String dpslMtdCd;           // 처분방식코드
    private String dpslMtdNm;           // 처분방식명
    private String bidMtdNm;            // 입찰방식명
    
    // 가격 정보
    private Long minBidPrc;             // 최저입찰가
    private Long apslAsesAvgAmt;        // 감정평가액 평균
    private String feeRate;             // 수수료율
    
    // 입찰 일정
    private String pbctBegnDtm;         // 입찰시작일시
    private String pbctClsDtm;          // 입찰종료일시
    
    // 상태 정보
    private String pbctCltrStatNm;      // 입찰상태명
    private Integer uscbCnt;            // 유찰횟수
    private Integer iqryCnt;            // API 조회수
    
    // 통계 정보 (자체 관리)
    private Integer viewCount;          // 조회수 (자체 카운트)
    private Integer interestCount;      // 관심수
    
    // API 데이터 동기화 정보
    private Boolean isNew;              // 신규 물건 여부
    private Boolean isActive;           // 활성화 여부
    private LocalDateTime apiSyncDate;  // API 동기화 일시
    
    // 관리 일시
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}

