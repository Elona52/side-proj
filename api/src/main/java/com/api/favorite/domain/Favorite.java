package com.api.favorite.domain;

import java.sql.Timestamp;

import com.api.item.domain.KamcoItem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {
    
    private Long favoriteId;      // 즐겨찾기 ID (PK)
    private String userId;        // 회원 ID (FK -> KNMember.id)
    private Long itemId;          // 물건 ID (FK -> KNKamcoItem.id)
    private Timestamp createdAt;  // 생성일
    
    // 조인을 위한 추가 필드 (물건 정보)
    private KamcoItem item;      // 물건 상세 정보 (JOIN 시 사용)
}
