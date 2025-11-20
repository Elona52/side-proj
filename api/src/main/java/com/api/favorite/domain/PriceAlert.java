package com.api.favorite.domain;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlert {
    
    private Long id;
    private Long favoriteId;
    private String memberId;
    private String itemPlnmNo;      // 공고번호
    private Long previousPrice;     // 이전 가격
    private Long newPrice;          // 새로운 가격
    private Boolean alertSent;      // 알림 전송 여부
    private Timestamp sentDate;     // 전송 날짜
    private Timestamp createdDate;
}


