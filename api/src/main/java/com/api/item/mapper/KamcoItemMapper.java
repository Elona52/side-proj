package com.api.item.mapper;

import java.util.List;
import org.apache.ibatis.annotations.*;
import com.api.item.domain.KamcoItem;

/**
 * 캠코 온비드 공매 물건 Mapper
 */
@Mapper
public interface KamcoItemMapper {

    // =============================================================================
    // 기본 CRUD
    // =============================================================================
    
    /**
     * 물건번호로 조회
     */
    @Select("SELECT * FROM KNKamcoItem WHERE cltr_no = #{cltrNo}")
    KamcoItem findByCltrNo(String cltrNo);
    
    /**
     * 공고번호로 조회
     */
    @Select("SELECT * FROM KNKamcoItem WHERE plnm_no = #{plnmNo} AND is_active = 1 ORDER BY created_date DESC LIMIT 1")
    KamcoItem findByPlnmNo(String plnmNo);
    
    /**
     * 물건번호로 모든 입찰 이력 조회
     */
    @Select("SELECT * FROM KNKamcoItem WHERE cltr_no = #{cltrNo} ORDER BY created_date DESC")
    List<KamcoItem> findAllByCltrNo(String cltrNo);
    
    /**
     * ID로 조회
     */
    @Select("SELECT * FROM KNKamcoItem WHERE id = #{id}")
    KamcoItem findById(Long id);
    
    /**
     * 전체 조회
     */
    @Select("SELECT * FROM KNKamcoItem WHERE is_active = 1 ORDER BY created_date DESC")
    List<KamcoItem> findAll();
    
    /**
     * 신규 물건 조회
     */
    @Select("SELECT * FROM KNKamcoItem WHERE is_new = 1 AND is_active = 1 AND IFNULL(uscb_cnt, 0) = 0 " +
            "ORDER BY created_date DESC LIMIT #{limit}")
    List<KamcoItem> findNewItems(@Param("limit") int limit);

    /**
     * 신규 물건 조회 (시도 필터, 제한 없음)
     */
    @Select({
        "<script>",
        "SELECT * FROM KNKamcoItem",
        "WHERE is_new = 1 AND is_active = 1 AND IFNULL(uscb_cnt, 0) = 0",
        "<if test='sido != null and sido != \"\" and sido != \"all\"'>",
        "  AND sido = #{sido}",
        "</if>",
        "ORDER BY created_date DESC",
        "</script>"
    })
    List<KamcoItem> findNewItemsBySido(@Param("sido") String sido);
    
    /**
     * 당일 매각 예정 물건 조회
     */
    @Select("SELECT * FROM KNKamcoItem WHERE is_active = 1 " +
            "AND DATE(STR_TO_DATE(pbct_cls_dtm, '%Y%m%d%H%i%s')) = CURDATE() " +
            "ORDER BY pbct_cls_dtm ASC")
    List<KamcoItem> findTodayClosingItems();
    
    /**
     * 50% 체감 물건 조회 (유찰 3회 이상 = 약 50% 체감)
     */
    @Select("SELECT * FROM KNKamcoItem WHERE is_active = 1 AND uscb_cnt >= 3 " +
            "ORDER BY uscb_cnt DESC, min_bid_prc ASC LIMIT #{limit}")
    List<KamcoItem> find50PercentDiscountItems(@Param("limit") int limit);

    /**
     * 50% 체감 물건 조회 (시도 필터, 제한 없음)
     */
    @Select({
        "<script>",
        "SELECT * FROM KNKamcoItem",
        "WHERE is_active = 1 AND uscb_cnt >= 3",
        "<if test='sido != null and sido != \"\" and sido != \"all\"'>",
        "  AND sido = #{sido}",
        "</if>",
        "ORDER BY uscb_cnt DESC, min_bid_prc ASC",
        "</script>"
    })
    List<KamcoItem> find50PercentDiscountItemsBySido(@Param("sido") String sido);
    
    /**
     * 클릭 TOP 20 물건 조회
     */
    @Select("SELECT * FROM KNKamcoItem WHERE is_active = 1 " +
            "ORDER BY view_count DESC, interest_count DESC LIMIT 20")
    List<KamcoItem> findTop20ByViews();
    
    /**
     * 관심 TOP 20 물건 조회
     */
    @Select("SELECT * FROM KNKamcoItem WHERE is_active = 1 " +
            "ORDER BY interest_count DESC, view_count DESC LIMIT 20")
    List<KamcoItem> findTop20ByInterest();
    
    /**
     * 시도별 조회
     */
    @Select("SELECT * FROM KNKamcoItem WHERE is_active = 1 AND sido = #{sido} " +
            "ORDER BY created_date DESC")
    List<KamcoItem> findBySido(@Param("sido") String sido);
    
    /**
     * 삽입 (중복 시 업데이트)
     */
    @Insert("INSERT INTO KNKamcoItem(" +
            "rnum, plnm_no, pbct_no, org_base_no, org_nm, cltr_no, pbct_cdtn_no, cltr_mnmt_no, cltr_hstr_no, bid_mnmt_no, " +
            "scrn_grp_cd, ctgr_id, ctgr_full_nm, " +
            "cltr_nm, goods_nm, manf, " +
            "ldnm_adrs, nmrd_adrs, rod_nm, bld_no, sido, " +
            "dpsl_mtd_cd, dpsl_mtd_nm, bid_mtd_nm, " +
            "min_bid_prc, apsl_ases_avg_amt, fee_rate, " +
            "pbct_begn_dtm, pbct_cls_dtm, " +
            "pbct_cltr_stat_nm, uscb_cnt, iqry_cnt, " +
            "is_new, is_active, api_sync_date" +
            ") VALUES (" +
            "#{rnum}, #{plnmNo}, #{pbctNo}, #{orgBaseNo}, #{orgNm}, #{cltrNo}, #{pbctCdtnNo}, #{cltrMnmtNo}, #{cltrHstrNo}, #{bidMnmtNo}, " +
            "#{scrnGrpCd}, #{ctgrId}, #{ctgrFullNm}, " +
            "#{cltrNm}, #{goodsNm}, #{manf}, " +
            "#{ldnmAdrs}, #{nmrdAdrs}, #{rodNm}, #{bldNo}, #{sido}, " +
            "#{dpslMtdCd}, #{dpslMtdNm}, #{bidMtdNm}, " +
            "#{minBidPrc}, #{apslAsesAvgAmt}, #{feeRate}, " +
            "#{pbctBegnDtm}, #{pbctClsDtm}, " +
            "#{pbctCltrStatNm}, #{uscbCnt}, #{iqryCnt}, " +
            "#{isNew}, #{isActive}, NOW()" +
            ") ON DUPLICATE KEY UPDATE " +
            "rnum=VALUES(rnum), plnm_no=VALUES(plnm_no), pbct_no=VALUES(pbct_no), " +
            "org_base_no=VALUES(org_base_no), org_nm=VALUES(org_nm), " +
            "pbct_cdtn_no=VALUES(pbct_cdtn_no), cltr_mnmt_no=VALUES(cltr_mnmt_no), " +
            "cltr_hstr_no=VALUES(cltr_hstr_no), bid_mnmt_no=VALUES(bid_mnmt_no), " +
            "scrn_grp_cd=VALUES(scrn_grp_cd), ctgr_id=VALUES(ctgr_id), ctgr_full_nm=VALUES(ctgr_full_nm), " +
            "cltr_nm=VALUES(cltr_nm), goods_nm=VALUES(goods_nm), manf=VALUES(manf), " +
            "ldnm_adrs=VALUES(ldnm_adrs), nmrd_adrs=VALUES(nmrd_adrs), " +
            "rod_nm=VALUES(rod_nm), bld_no=VALUES(bld_no), sido=VALUES(sido), " +
            "dpsl_mtd_cd=VALUES(dpsl_mtd_cd), dpsl_mtd_nm=VALUES(dpsl_mtd_nm), bid_mtd_nm=VALUES(bid_mtd_nm), " +
            "min_bid_prc=VALUES(min_bid_prc), apsl_ases_avg_amt=VALUES(apsl_ases_avg_amt), fee_rate=VALUES(fee_rate), " +
            "pbct_begn_dtm=VALUES(pbct_begn_dtm), pbct_cls_dtm=VALUES(pbct_cls_dtm), " +
            "pbct_cltr_stat_nm=VALUES(pbct_cltr_stat_nm), uscb_cnt=VALUES(uscb_cnt), iqry_cnt=VALUES(iqry_cnt), " +
            "is_active=VALUES(is_active), api_sync_date=NOW()")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertOrUpdate(KamcoItem item);
    
    /**
     * 조회수 증가
     */
    @Update("UPDATE KNKamcoItem SET view_count = view_count + 1 WHERE id = #{id}")
    void incrementViewCount(Long id);
    
    /**
     * 관심수 증가
     */
    @Update("UPDATE KNKamcoItem SET interest_count = interest_count + 1 WHERE cltr_no = #{cltrNo}")
    void incrementInterestCount(String cltrNo);
    
    /**
     * 관심수 감소
     */
    @Update("UPDATE KNKamcoItem SET interest_count = interest_count - 1 WHERE cltr_no = #{cltrNo} AND interest_count > 0")
    void decrementInterestCount(String cltrNo);
    
    /**
     * 관심수 설정 (가중치 부여용)
     */
    @Update("UPDATE KNKamcoItem SET interest_count = #{score} WHERE cltr_no = #{cltrNo}")
    void updateInterestCount(@Param("cltrNo") String cltrNo, @Param("score") int score);
    
    /**
     * ID로 삭제
     */
    @Delete("DELETE FROM KNKamcoItem WHERE id = #{id}")
    void deleteById(Long id);
    
    /**
     * 서울특별시가 아닌 데이터 삭제
     */
    @Delete("DELETE FROM KNKamcoItem WHERE sido != '서울특별시'")
    int deleteNonSeoulItems();
    
    /**
     * 전체 데이터 삭제
     */
    @Delete("DELETE FROM KNKamcoItem")
    int deleteAll();
    
    /**
     * 신규 물건 플래그 해제
     */
    @Update("UPDATE KNKamcoItem SET is_new = 0 WHERE is_new = 1 " +
            "AND created_date < DATE_SUB(NOW(), INTERVAL 7 DAY)")
    void unmarkOldNewItems();
    
    /**
     * 종료된 물건 비활성화
     */
    @Update("UPDATE KNKamcoItem SET is_active = 0 WHERE is_active = 1 " +
            "AND STR_TO_DATE(pbct_cls_dtm, '%Y%m%d%H%i%s') < NOW()")
    void deactivateExpiredItems();
    
    /**
     * 검색 (물건명, 주소)
     */
    @Select("SELECT * FROM KNKamcoItem WHERE is_active = 1 " +
            "AND (cltr_nm LIKE CONCAT('%', #{keyword}, '%') " +
            "OR ldnm_adrs LIKE CONCAT('%', #{keyword}, '%') " +
            "OR nmrd_adrs LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY created_date DESC LIMIT #{limit}")
    List<KamcoItem> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
}

