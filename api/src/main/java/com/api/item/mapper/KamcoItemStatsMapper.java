package com.api.item.mapper;

import org.apache.ibatis.annotations.*;
import com.api.item.domain.KamcoItemStats;

/**
 * 캠코 온비드 물건 통계 스냅샷 Mapper
 */
@Mapper
public interface KamcoItemStatsMapper {
    
    /**
     * 통계 스냅샷 저장 (중복 시 업데이트)
     */
    @Insert("INSERT INTO KNKamcoItemStats(item_id, cltr_no, stat_date, view_count, interest_count) " +
            "VALUES(#{itemId}, #{cltrNo}, #{statDate}, #{viewCount}, #{interestCount}) " +
            "ON DUPLICATE KEY UPDATE " +
            "view_count=VALUES(view_count), interest_count=VALUES(interest_count)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertOrUpdate(KamcoItemStats stats);
    
    /**
     * 오래된 통계 삭제 (90일 이상)
     */
    @Delete("DELETE FROM KNKamcoItemStats WHERE stat_date < DATE_SUB(CURDATE(), INTERVAL 90 DAY)")
    void deleteOldStats();
}

