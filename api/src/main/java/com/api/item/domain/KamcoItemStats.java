package com.api.item.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 캠코 온비드 물건 통계 스냅샷
 */
@Data
public class KamcoItemStats {
    private Long id;
    private Long itemId;
    private String cltrNo;
    private LocalDate statDate;
    private Integer viewCount;
    private Integer interestCount;
    private LocalDateTime createdDate;
}

