package com.api.item.mapper;

import java.util.List;
import org.apache.ibatis.annotations.*;
import com.api.item.domain.NewItemNotification;

/**
 * 새로운 물건 공지사항 Mapper
 */
@Mapper
public interface NewItemNotificationMapper {
    
    /**
     * 전체 공지 조회
     */
    @Select("SELECT * FROM KNNewItemNotification WHERE is_displayed = 1 " +
            "AND (expired_date IS NULL OR expired_date > NOW()) " +
            "ORDER BY display_order ASC, created_date DESC")
    List<NewItemNotification> findAll();
    
    /**
     * 타입별 공지 조회
     */
    @Select("SELECT * FROM KNNewItemNotification WHERE is_displayed = 1 " +
            "AND notification_type = #{type} " +
            "AND (expired_date IS NULL OR expired_date > NOW()) " +
            "ORDER BY display_order ASC, created_date DESC LIMIT #{limit}")
    List<NewItemNotification> findByType(@Param("type") String type, @Param("limit") int limit);
    
    /**
     * 신규 물건 공지 추가
     */
    @Insert("INSERT INTO KNNewItemNotification(item_id, cltr_no, cltr_nm, min_bid_prc, pbct_cls_dtm, " +
            "notification_type, is_displayed, display_order, expired_date) " +
            "VALUES(#{itemId}, #{cltrNo}, #{cltrNm}, #{minBidPrc}, #{pbctClsDtm}, " +
            "#{notificationType}, #{isDisplayed}, #{displayOrder}, #{expiredDate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(NewItemNotification notification);
    
    /**
     * 만료된 공지 삭제
     */
    @Delete("DELETE FROM KNNewItemNotification WHERE expired_date < NOW()")
    void deleteExpired();
    
    /**
     * 오래된 공지 숨김 처리 (30일 이상)
     */
    @Update("UPDATE KNNewItemNotification SET is_displayed = 0 " +
            "WHERE created_date < DATE_SUB(NOW(), INTERVAL 30 DAY)")
    void hideOldNotifications();
}

