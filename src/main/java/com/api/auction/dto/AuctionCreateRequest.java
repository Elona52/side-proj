package com.api.auction.dto;

import java.sql.Date;
import com.api.auction.domain.Auction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 경매 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionCreateRequest {
    
    private String id;
    private String name;
    private String content;
    private Date startDate;
    private Date endDate;
    private int startPrice;
    private String img;
    
    /**
     * DTO를 Domain 객체로 변환
     */
    public Auction toAuction() {
        Auction auction = new Auction();
        auction.setId(this.id);
        auction.setName(this.name);
        auction.setContent(this.content);
        auction.setStartDate(this.startDate);
        auction.setEndDate(this.endDate);
        auction.setStartPrice(this.startPrice);
        auction.setEndPrice(this.startPrice); // 초기값은 시작가와 동일
        auction.setImg(this.img);
        auction.setCount(0); // 초기 조회수
        auction.setDepositStatus(false);
        auction.setDeliveryStatus(false);
        auction.setRemitStatus(false);
        auction.setApiItem(false);
        return auction;
    }
}

