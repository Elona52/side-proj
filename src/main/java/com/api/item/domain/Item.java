package com.api.item.domain;

import lombok.Data;

@Data
public class Item {
    // 기본 정보
    private String rnum;              // 순번
    private String plnmNo;            // 공고번호
    private String pbctNo;            // 공매번호
    private String orgBaseNo;         // 기관기준번호
    private String orgNm;             // 기관명
    private String cltrNo;            // 물건번호
    private String pbctCdtnNo;        // 공매조건번호
    private String cltrHstrNo;        // 물건이력번호
    private String scrnGrpCd;         // 화면그룹코드
    private String ctgrId;            // 카테고리ID
    private String ctgrFullNm;        // 카테고리 전체명 (예: 주거용건물 / 도시형생활주택)
    private String bidMnmtNo;         // 입찰관리번호
    
    // 물건 정보
    private String cltrNm;            // 물건명 (예: 서울특별시 강서구 등촌동 719 경남파크빌 제101동 제3층 제301호)
    private String cltrMnmtNo;        // 물건관리번호
    private String goodsNm;           // 물건내역 (예: 건물 52.56㎡, 대 26.111㎡ 지분 등)
    private String manf;              // 제조사 (없을 경우 빈값)
    
    // 주소 정보
    private String ldnmAdrs;          // 지번주소
    private String nmrdAdrs;          // 도로명주소
    private String rodNm;             // 도로명
    private String bldNo;             // 건물번호
    
    // 처분/입찰 방식
    private String dpslMtdCd;         // 처분방식코드 (예: 0001)
    private String dpslMtdNm;         // 처분방식명 (예: 매각)
    private String bidMtdNm;          // 입찰방식명 (예: 일반경쟁(최고가방식) / 총액)
    
    // 가격 정보
    private Long minBidPrc;           // 최저입찰가
    private Long apslAsesAvgAmt;      // 감정평가액 평균
    private String feeRate;           // 수수료율 (예: (50%))
    
    // 입찰 일정
    private String pbctBegnDtm;       // 입찰시작일시 (예: 20251110140000)
    private String pbctClsDtm;        // 입찰종료일시 (예: 20251112170000)
    
    // 상태 및 통계
    private String pbctCltrStatNm;    // 입찰상태명 (예: 인터넷입찰진행중)
    private Integer uscbCnt;          // 유찰횟수
    private Integer iqryCnt;          // 조회수
}

