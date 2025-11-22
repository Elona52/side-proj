package com.api.item.dto;

import java.time.LocalDateTime;
import com.api.item.domain.KamcoItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캠코 온비드 공매 물건 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KamcoItemResponse {
    
    // 기본 식별 정보
    private Long id;
    private Integer rnum;              // admin-panel에서 사용
    private String plnmNo;
    private String pbctNo;
    private String orgNm;
    private String cltrNo;
    private String cltrMnmtNo;
    private String bidMnmtNo;
    
    // 분류 정보
    private String scrnGrpCd;          // NewItemNotificationService에서 사용
    private String ctgrFullNm;
    
    // 물건 정보
    private String cltrNm;
    private String goodsNm;
    
    // 주소 정보
    private String ldnmAdrs;
    private String nmrdAdrs;
    private String rodNm;              // admin-panel에서 사용
    private String bldNo;              // admin-panel에서 사용
    private String sido;
    
    // 처분/입찰 방식
    private String dpslMtdNm;
    private String bidMtdNm;
    
    // 가격 정보
    private Long minBidPrc;
    private Long apslAsesAvgAmt;
    private String feeRate;
    
    // 입찰 일정
    private String pbctBegnDtm;
    private String pbctClsDtm;
    
    // 상태 정보
    private String pbctCltrStatNm;
    private Integer uscbCnt;
    private Integer iqryCnt;
    
    // 통계 정보
    private Integer viewCount;
    private Integer interestCount;
    
    // 상태
    private Boolean isNew;
    private Boolean isActive;
    private LocalDateTime apiSyncDate;
    
    // 관리 일시
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    /**
     * Domain 객체를 DTO로 변환
     */
    public static KamcoItemResponse from(KamcoItem item) {
        if (item == null) {
            return null;
        }
        
        return KamcoItemResponse.builder()
                .id(item.getId())
                .rnum(item.getRnum())
                .plnmNo(item.getPlnmNo())
                .pbctNo(item.getPbctNo())
                .orgNm(item.getOrgNm())
                .cltrNo(item.getCltrNo())
                .cltrMnmtNo(item.getCltrMnmtNo())
                .bidMnmtNo(item.getBidMnmtNo())
                .scrnGrpCd(item.getScrnGrpCd())
                .ctgrFullNm(item.getCtgrFullNm())
                .cltrNm(item.getCltrNm())
                .goodsNm(item.getGoodsNm())
                .ldnmAdrs(item.getLdnmAdrs())
                .nmrdAdrs(item.getNmrdAdrs())
                .rodNm(item.getRodNm())
                .bldNo(item.getBldNo())
                .sido(item.getSido())
                .dpslMtdNm(item.getDpslMtdNm())
                .bidMtdNm(item.getBidMtdNm())
                .minBidPrc(item.getMinBidPrc())
                .apslAsesAvgAmt(item.getApslAsesAvgAmt())
                .feeRate(item.getFeeRate())
                .pbctBegnDtm(item.getPbctBegnDtm())
                .pbctClsDtm(item.getPbctClsDtm())
                .pbctCltrStatNm(item.getPbctCltrStatNm())
                .uscbCnt(item.getUscbCnt())
                .iqryCnt(item.getIqryCnt())
                .viewCount(item.getViewCount())
                .interestCount(item.getInterestCount())
                .isNew(item.getIsNew())
                .isActive(item.getIsActive())
                .apiSyncDate(item.getApiSyncDate())
                .createdDate(item.getCreatedDate())
                .updatedDate(item.getUpdatedDate())
                .build();
    }
}

