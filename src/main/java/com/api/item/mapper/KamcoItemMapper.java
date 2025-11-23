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
     * XML Mapper에서 databaseId로 분기 처리
     */
    KamcoItem findByCltrNo(String cltrNo);
    
    /**
     * 공고번호로 조회
     * XML Mapper에서 databaseId로 분기 처리
     */
    KamcoItem findByPlnmNo(String plnmNo);
    
    /**
     * 물건번호로 모든 입찰 이력 조회
     * XML Mapper에서 databaseId로 분기 처리
     */
    List<KamcoItem> findAllByCltrNo(String cltrNo);
    
    /**
     * ID로 조회
     * XML Mapper에서 databaseId로 분기 처리
     */
    KamcoItem findById(Long id);
    
    /**
     * 전체 조회
     * XML Mapper에서 databaseId로 분기 처리
     */
    List<KamcoItem> findAll();
    
    /**
     * 신규 물건 조회
     * XML Mapper에서 databaseId로 분기 처리 (mysql/postgresql)
     */
    List<KamcoItem> findNewItems(@Param("limit") int limit);

    /**
     * 신규 물건 조회 (시도 필터, 제한 없음)
     * XML Mapper에서 databaseId로 분기 처리 (mysql/postgresql)
     */
    List<KamcoItem> findNewItemsBySido(@Param("sido") String sido);
    
    /**
     * 당일 매각 예정 물건 조회
     * XML Mapper에서 databaseId로 분기 처리 (mysql/postgresql)
     */
    List<KamcoItem> findTodayClosingItems();
    
    /**
     * 50% 체감 물건 조회 (유찰 3회 이상 = 약 50% 체감)
     * XML Mapper에서 databaseId로 분기 처리 (mysql/postgresql)
     */
    List<KamcoItem> find50PercentDiscountItems(@Param("limit") int limit);

    /**
     * 50% 체감 물건 조회 (시도 필터, 제한 없음)
     * XML Mapper에서 databaseId로 분기 처리 (mysql/postgresql)
     */
    List<KamcoItem> find50PercentDiscountItemsBySido(@Param("sido") String sido);
    
    /**
     * 클릭 TOP 20 물건 조회
     * XML Mapper에서 databaseId로 분기 처리
     */
    List<KamcoItem> findTop20ByViews();
    
    /**
     * 관심 TOP 20 물건 조회
     * XML Mapper에서 databaseId로 분기 처리
     */
    List<KamcoItem> findTop20ByInterest();
    
    /**
     * 시도별 조회
     * XML Mapper에서 databaseId로 분기 처리
     */
    List<KamcoItem> findBySido(@Param("sido") String sido);
    
    /**
     * 삽입 (중복 시 업데이트)
     * XML Mapper에서 databaseId로 분기 처리 (mysql/postgresql)
     */
    void insertOrUpdate(KamcoItem item);
    
    /**
     * 조회수 증가
     * XML Mapper에서 처리
     */
    void incrementViewCount(Long id);
    
    /**
     * 관심수 증가
     * XML Mapper에서 처리
     */
    void incrementInterestCount(String cltrNo);
    
    /**
     * 관심수 감소
     * XML Mapper에서 처리
     */
    void decrementInterestCount(String cltrNo);
    
    /**
     * 관심수 설정 (가중치 부여용)
     * XML Mapper에서 처리
     */
    void updateInterestCount(@Param("cltrNo") String cltrNo, @Param("score") int score);
    
    /**
     * ID로 삭제
     * XML Mapper에서 처리
     */
    void deleteById(Long id);
    
    /**
     * 서울특별시가 아닌 데이터 삭제
     * XML Mapper에서 처리
     */
    int deleteNonSeoulItems();
    
    /**
     * 전체 데이터 삭제
     * XML Mapper에서 처리
     */
    int deleteAll();
    
    /**
     * 신규 물건 플래그 해제
     * XML Mapper에서 databaseId로 분기 처리 (mysql/postgresql)
     */
    void unmarkOldNewItems();
    
    /**
     * 종료된 물건 비활성화
     * XML Mapper에서 databaseId로 분기 처리 (mysql/postgresql)
     */
    void deactivateExpiredItems();
    
    /**
     * 검색 (물건명, 주소)
     * XML Mapper에서 databaseId로 분기 처리 (mysql/postgresql)
     */
    List<KamcoItem> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
}

