package com.api.item.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 새로운 물건 공지사항
 */
@Data
public class NewItemNotification {
    private Long id;
    private Long itemId;
    private String cltrNo;
    private String cltrNm;
    private Long minBidPrc;
    private String pbctClsDtm;
    private String notificationType;    // NEW, PRICE_DROP, DEADLINE
    private Boolean isDisplayed;
    private Integer displayOrder;
    private LocalDateTime createdDate;
    private LocalDateTime expiredDate;
}

