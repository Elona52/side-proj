package com.api.item.domain;

import lombok.Data;
import java.sql.Timestamp;

/**
 * 공매 물건 상세 정보 도메인
 */
@Data
public class PublicAuctionInfo {
    private Integer rnum;              // 순번
    private String plnmNo;             // 공고번호
    private String pbctNo;             // 공매번호
    private String orgBaseNo;          // 기관기본번호
    private String orgNm;              // 기관명
    private String cltrNo;             // 물건번호
    private String pbctCdtnNo;         // 공매조건번호
    private String cltrHstrNo;         // 물건이력번호
    private String scrnGrpCd;          // 화면그룹코드
    private String ctgrFullNm;         // 자산분류명 (전체)
    private String bidMnmtNo;          // 입찰관리번호
    private String cltrNm;             // 물건명
    private String cltrMnmtNo;         // 물건관리번호
    private String ldnmAdrs;           // 지번주소
    private String nmrdAdrs;           // 도로명주소
    private String rodNm;              // 도로명
    private String bldNo;              // 건물번호
    private String dpslMtdCd;          // 처분방법코드
    private String dpslMtdNm;          // 처분방법명
    private String bidMtdNm;           // 입찰방식명
    private Long minBidPrc;            // 최저입찰가
    private Long apslAsesAvgAmt;       // 감정가(평균액)
    private String feeRate;            // 수수료율
    private String pbctBegnDtm;        // 입찰시작일시
    private String pbctClsDtm;         // 입찰종료일시
    private String pbctCltrStatNm;     // 입찰상태명
    private Integer uscbCnt;           // 입찰참여수(또는 응찰자 수)
    private Integer iqryCnt;           // 조회수
    private String goodsNm;            // 물건내역(토지/건물 설명 등)
    private Timestamp createdDate;     // 생성일시
    private Timestamp updatedDate;     // 수정일시
}

