package com.api.item.mapper;

import org.apache.ibatis.annotations.*;
import com.api.item.domain.KamcoItemViewLog;

/**
 * 캠코 온비드 공매 물건 조회 이력 Mapper
 */
@Mapper
public interface KamcoItemViewLogMapper {
    
    /**
     * 조회 이력 추가
     */
    @Insert("INSERT INTO KNKamcoItemViewLog(item_id, cltr_no, member_id, ip_address, user_agent, view_date) " +
            "VALUES(#{itemId}, #{cltrNo}, #{memberId}, #{ipAddress}, #{userAgent}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(KamcoItemViewLog viewLog);
    
    /**
     * 오래된 로그 삭제 (30일 이상)
     */
    @Delete("DELETE FROM KNKamcoItemViewLog WHERE view_date < DATE_SUB(NOW(), INTERVAL 30 DAY)")
    void deleteOldLogs();
}

