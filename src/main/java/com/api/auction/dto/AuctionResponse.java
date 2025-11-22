package com.api.auction.dto;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Map;
import com.api.auction.domain.Auction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 경매 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionResponse {
    
    private int no;
    private String id;
    private String name;
    private String content;
    private Timestamp regDate;
    private Date startDate;
    private Date endDate;
    private int startPrice;
    private int endPrice;
    private String img;
    private String buyer;
    private int count;
    private Map<String, Integer> bidder;
    private boolean depositStatus;
    private Date depositDate;
    private boolean deliveryStatus;
    private Date deliveryDate;
    private boolean remitStatus;
    private Date remitDate;
    private String cltrNo;
    private boolean apiItem;
    
    /**
     * Domain 객체를 DTO로 변환
     */
    public static AuctionResponse from(Auction auction) {
        if (auction == null) {
            return null;
        }
        
        return AuctionResponse.builder()
                .no(auction.getNo())
                .id(auction.getId())
                .name(auction.getName())
                .content(auction.getContent())
                .regDate(auction.getRegDate())
                .startDate(auction.getStartDate())
                .endDate(auction.getEndDate())
                .startPrice(auction.getStartPrice())
                .endPrice(auction.getEndPrice())
                .img(auction.getImg())
                .buyer(auction.getBuyer())
                .count(auction.getCount())
                .bidder(auction.getBidder())
                .depositStatus(auction.isDepositStatus())
                .depositDate(auction.getDepositDate())
                .deliveryStatus(auction.isDeliveryStatus())
                .deliveryDate(auction.getDeliveryDate())
                .remitStatus(auction.isRemitStatus())
                .remitDate(auction.getRemitDate())
                .cltrNo(auction.getCltrNo())
                .apiItem(auction.isApiItem())
                .build();
    }
}

