package com.api.favorite.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.api.favorite.domain.Favorite;
import com.api.favorite.domain.PriceAlert;

@Mapper
public interface FavoriteMapper {

    // 즐겨찾기 추가
    void insertFavorite(Favorite favorite);
    
    // 즐겨찾기 삭제
    void deleteFavorite(@Param("id") Long id);
    
    // ID로 즐겨찾기 조회
    Favorite getFavoriteById(@Param("id") Long id);
    
    // 특정 회원의 즐겨찾기 목록 조회
    List<Favorite> getFavoritesByMemberId(@Param("memberId") String memberId);
    
    // 특정 회원의 특정 물건 즐겨찾기 조회 (itemId 사용)
    Favorite getFavoriteByMemberAndItem(@Param("memberId") String memberId, 
                                        @Param("itemId") Long itemId);
    
    // itemId로 즐겨찾기 조회 (cltrNo 대신 itemId 사용)
    Favorite getFavoriteByMemberAndItemId(@Param("memberId") String memberId, 
                                          @Param("itemId") Long itemId);
    
    // 알림이 활성화된 모든 즐겨찾기 조회
    List<Favorite> getActiveAlertFavorites();
    
    // 가격 알림 히스토리 추가
    void insertPriceAlert(PriceAlert priceAlert);
    
    // 특정 회원의 가격 알림 히스토리 조회
    List<PriceAlert> getPriceAlertsByMemberId(@Param("memberId") String memberId);
    
    // 최근 알림 조회 (중복 알림 방지용)
    PriceAlert getLastPriceAlertByFavoriteId(@Param("favoriteId") Long favoriteId);
}
