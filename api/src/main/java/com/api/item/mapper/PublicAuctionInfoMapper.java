package com.api.item.mapper;

import com.api.item.domain.PublicAuctionInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 공매 물건 상세 정보 Mapper
 */
@Mapper
public interface PublicAuctionInfoMapper {
    
    /**
     * 공매 물건 상세 정보 저장 (INSERT or UPDATE)
     * 주의: KNPublicAuctionInfo가 뷰로 변경되면 이 메서드는 KNKamcoItem을 직접 사용해야 함
     */
    @Insert("INSERT INTO KNKamcoItem (" +
            "rnum, plnm_no, pbct_no, org_base_no, org_nm, cltr_no, pbct_cdtn_no, cltr_hstr_no, " +
            "scrn_grp_cd, ctgr_full_nm, bid_mnmt_no, cltr_nm, cltr_mnmt_no, ldnm_adrs, nmrd_adrs, " +
            "rod_nm, bld_no, dpsl_mtd_cd, dpsl_mtd_nm, bid_mtd_nm, min_bid_prc, apsl_ases_avg_amt, " +
            "fee_rate, pbct_begn_dtm, pbct_cls_dtm, pbct_cltr_stat_nm, uscb_cnt, iqry_cnt, goods_nm" +
            ") VALUES (" +
            "#{rnum}, #{plnmNo}, #{pbctNo}, #{orgBaseNo}, #{orgNm}, #{cltrNo}, #{pbctCdtnNo}, #{cltrHstrNo}, " +
            "#{scrnGrpCd}, #{ctgrFullNm}, #{bidMnmtNo}, #{cltrNm}, #{cltrMnmtNo}, #{ldnmAdrs}, #{nmrdAdrs}, " +
            "#{rodNm}, #{bldNo}, #{dpslMtdCd}, #{dpslMtdNm}, #{bidMtdNm}, #{minBidPrc}, #{apslAsesAvgAmt}, " +
            "#{feeRate}, #{pbctBegnDtm}, #{pbctClsDtm}, #{pbctCltrStatNm}, #{uscbCnt}, #{iqryCnt}, #{goodsNm}" +
            ") ON DUPLICATE KEY UPDATE " +
            "rnum = VALUES(rnum), plnm_no = VALUES(plnm_no), org_base_no = VALUES(org_base_no), " +
            "org_nm = VALUES(org_nm), pbct_cdtn_no = VALUES(pbct_cdtn_no), cltr_hstr_no = VALUES(cltr_hstr_no), " +
            "scrn_grp_cd = VALUES(scrn_grp_cd), ctgr_full_nm = VALUES(ctgr_full_nm), bid_mnmt_no = VALUES(bid_mnmt_no), " +
            "cltr_nm = VALUES(cltr_nm), cltr_mnmt_no = VALUES(cltr_mnmt_no), ldnm_adrs = VALUES(ldnm_adrs), " +
            "nmrd_adrs = VALUES(nmrd_adrs), rod_nm = VALUES(rod_nm), bld_no = VALUES(bld_no), " +
            "dpsl_mtd_cd = VALUES(dpsl_mtd_cd), dpsl_mtd_nm = VALUES(dpsl_mtd_nm), bid_mtd_nm = VALUES(bid_mtd_nm), " +
            "min_bid_prc = VALUES(min_bid_prc), apsl_ases_avg_amt = VALUES(apsl_ases_avg_amt), fee_rate = VALUES(fee_rate), " +
            "pbct_begn_dtm = VALUES(pbct_begn_dtm), pbct_cls_dtm = VALUES(pbct_cls_dtm), " +
            "pbct_cltr_stat_nm = VALUES(pbct_cltr_stat_nm), uscb_cnt = VALUES(uscb_cnt), " +
            "iqry_cnt = VALUES(iqry_cnt), goods_nm = VALUES(goods_nm), updated_date = CURRENT_TIMESTAMP")
    int insertOrUpdate(PublicAuctionInfo info);
    
    /**
     * 물건번호로 조회
     */
    @Select("SELECT * FROM KNPublicAuctionInfo WHERE cltr_no = #{cltrNo}")
    @Results({
        @Result(property = "plnmNo", column = "plnm_no"),
        @Result(property = "pbctNo", column = "pbct_no"),
        @Result(property = "orgBaseNo", column = "org_base_no"),
        @Result(property = "orgNm", column = "org_nm"),
        @Result(property = "cltrNo", column = "cltr_no"),
        @Result(property = "pbctCdtnNo", column = "pbct_cdtn_no"),
        @Result(property = "cltrHstrNo", column = "cltr_hstr_no"),
        @Result(property = "scrnGrpCd", column = "scrn_grp_cd"),
        @Result(property = "ctgrFullNm", column = "ctgr_full_nm"),
        @Result(property = "bidMnmtNo", column = "bid_mnmt_no"),
        @Result(property = "cltrNm", column = "cltr_nm"),
        @Result(property = "cltrMnmtNo", column = "cltr_mnmt_no"),
        @Result(property = "ldnmAdrs", column = "ldnm_adrs"),
        @Result(property = "nmrdAdrs", column = "nmrd_adrs"),
        @Result(property = "rodNm", column = "rod_nm"),
        @Result(property = "bldNo", column = "bld_no"),
        @Result(property = "dpslMtdCd", column = "dpsl_mtd_cd"),
        @Result(property = "dpslMtdNm", column = "dpsl_mtd_nm"),
        @Result(property = "bidMtdNm", column = "bid_mtd_nm"),
        @Result(property = "minBidPrc", column = "min_bid_prc"),
        @Result(property = "apslAsesAvgAmt", column = "apsl_ases_avg_amt"),
        @Result(property = "feeRate", column = "fee_rate"),
        @Result(property = "pbctBegnDtm", column = "pbct_begn_dtm"),
        @Result(property = "pbctClsDtm", column = "pbct_cls_dtm"),
        @Result(property = "pbctCltrStatNm", column = "pbct_cltr_stat_nm"),
        @Result(property = "uscbCnt", column = "uscb_cnt"),
        @Result(property = "iqryCnt", column = "iqry_cnt"),
        @Result(property = "goodsNm", column = "goods_nm"),
        @Result(property = "createdDate", column = "created_date"),
        @Result(property = "updatedDate", column = "updated_date")
    })
    PublicAuctionInfo findByCltrNo(@Param("cltrNo") String cltrNo);
    
    /**
     * 공매번호와 물건번호로 조회
     */
    @Select("SELECT * FROM KNPublicAuctionInfo WHERE pbct_no = #{pbctNo} AND cltr_no = #{cltrNo}")
    @Results({
        @Result(property = "plnmNo", column = "plnm_no"),
        @Result(property = "pbctNo", column = "pbct_no"),
        @Result(property = "orgBaseNo", column = "org_base_no"),
        @Result(property = "orgNm", column = "org_nm"),
        @Result(property = "cltrNo", column = "cltr_no"),
        @Result(property = "pbctCdtnNo", column = "pbct_cdtn_no"),
        @Result(property = "cltrHstrNo", column = "cltr_hstr_no"),
        @Result(property = "scrnGrpCd", column = "scrn_grp_cd"),
        @Result(property = "ctgrFullNm", column = "ctgr_full_nm"),
        @Result(property = "bidMnmtNo", column = "bid_mnmt_no"),
        @Result(property = "cltrNm", column = "cltr_nm"),
        @Result(property = "cltrMnmtNo", column = "cltr_mnmt_no"),
        @Result(property = "ldnmAdrs", column = "ldnm_adrs"),
        @Result(property = "nmrdAdrs", column = "nmrd_adrs"),
        @Result(property = "rodNm", column = "rod_nm"),
        @Result(property = "bldNo", column = "bld_no"),
        @Result(property = "dpslMtdCd", column = "dpsl_mtd_cd"),
        @Result(property = "dpslMtdNm", column = "dpsl_mtd_nm"),
        @Result(property = "bidMtdNm", column = "bid_mtd_nm"),
        @Result(property = "minBidPrc", column = "min_bid_prc"),
        @Result(property = "apslAsesAvgAmt", column = "apsl_ases_avg_amt"),
        @Result(property = "feeRate", column = "fee_rate"),
        @Result(property = "pbctBegnDtm", column = "pbct_begn_dtm"),
        @Result(property = "pbctClsDtm", column = "pbct_cls_dtm"),
        @Result(property = "pbctCltrStatNm", column = "pbct_cltr_stat_nm"),
        @Result(property = "uscbCnt", column = "uscb_cnt"),
        @Result(property = "iqryCnt", column = "iqry_cnt"),
        @Result(property = "goodsNm", column = "goods_nm"),
        @Result(property = "createdDate", column = "created_date"),
        @Result(property = "updatedDate", column = "updated_date")
    })
    PublicAuctionInfo findByPbctNoAndCltrNo(@Param("pbctNo") String pbctNo, @Param("cltrNo") String cltrNo);
    
    /**
     * 전체 목록 조회 (페이징)
     */
    @Select("SELECT * FROM KNPublicAuctionInfo ORDER BY created_date DESC LIMIT #{offset}, #{limit}")
    @Results({
        @Result(property = "plnmNo", column = "plnm_no"),
        @Result(property = "pbctNo", column = "pbct_no"),
        @Result(property = "orgBaseNo", column = "org_base_no"),
        @Result(property = "orgNm", column = "org_nm"),
        @Result(property = "cltrNo", column = "cltr_no"),
        @Result(property = "pbctCdtnNo", column = "pbct_cdtn_no"),
        @Result(property = "cltrHstrNo", column = "cltr_hstr_no"),
        @Result(property = "scrnGrpCd", column = "scrn_grp_cd"),
        @Result(property = "ctgrFullNm", column = "ctgr_full_nm"),
        @Result(property = "bidMnmtNo", column = "bid_mnmt_no"),
        @Result(property = "cltrNm", column = "cltr_nm"),
        @Result(property = "cltrMnmtNo", column = "cltr_mnmt_no"),
        @Result(property = "ldnmAdrs", column = "ldnm_adrs"),
        @Result(property = "nmrdAdrs", column = "nmrd_adrs"),
        @Result(property = "rodNm", column = "rod_nm"),
        @Result(property = "bldNo", column = "bld_no"),
        @Result(property = "dpslMtdCd", column = "dpsl_mtd_cd"),
        @Result(property = "dpslMtdNm", column = "dpsl_mtd_nm"),
        @Result(property = "bidMtdNm", column = "bid_mtd_nm"),
        @Result(property = "minBidPrc", column = "min_bid_prc"),
        @Result(property = "apslAsesAvgAmt", column = "apsl_ases_avg_amt"),
        @Result(property = "feeRate", column = "fee_rate"),
        @Result(property = "pbctBegnDtm", column = "pbct_begn_dtm"),
        @Result(property = "pbctClsDtm", column = "pbct_cls_dtm"),
        @Result(property = "pbctCltrStatNm", column = "pbct_cltr_stat_nm"),
        @Result(property = "uscbCnt", column = "uscb_cnt"),
        @Result(property = "iqryCnt", column = "iqry_cnt"),
        @Result(property = "goodsNm", column = "goods_nm"),
        @Result(property = "createdDate", column = "created_date"),
        @Result(property = "updatedDate", column = "updated_date")
    })
    List<PublicAuctionInfo> findAll(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 총 레코드 수 조회
     */
    @Select("SELECT COUNT(*) FROM KNPublicAuctionInfo")
    int count();
    
    /**
     * 삭제
     * 주의: KNPublicAuctionInfo가 뷰로 변경되면 이 메서드는 KNKamcoItem을 직접 사용해야 함
     */
    @Delete("DELETE FROM KNKamcoItem WHERE pbct_no = #{pbctNo} AND cltr_no = #{cltrNo}")
    int delete(@Param("pbctNo") String pbctNo, @Param("cltrNo") String cltrNo);
}

