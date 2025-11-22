package com.api.auction.dto;

import java.sql.Date;
import com.api.auction.domain.Auction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 경매 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionUpdateRequest {
    
    private int no;
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
        auction.setNo(this.no);
        auction.setName(this.name);
        auction.setContent(this.content);
        auction.setStartDate(this.startDate);
        auction.setEndDate(this.endDate);
        auction.setStartPrice(this.startPrice);
        auction.setImg(this.img);
        return auction;
    }
}

