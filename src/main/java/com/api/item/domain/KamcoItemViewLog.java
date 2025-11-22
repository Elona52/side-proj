package com.api.item.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 캠코 온비드 공매 물건 조회 이력
 */
@Data
public class KamcoItemViewLog {
    private Long id;
    private Long itemId;
    private String cltrNo;
    private String memberId;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime viewDate;
}

