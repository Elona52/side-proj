package com.api.item.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.item.domain.KamcoItem;
import com.api.item.domain.KamcoItemViewLog;
import com.api.item.domain.Item;
import com.api.item.mapper.KamcoItemMapper;
import com.api.item.mapper.KamcoItemViewLogMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KamcoItemService {

    private final KamcoItemMapper kamcoItemMapper;
    private final KamcoItemViewLogMapper viewLogMapper;

    // =============================================================================
    // 조회 기능
    // =============================================================================
    
    /**
     * 물건번호로 조회
     */
    public KamcoItem getByCltrNo(String cltrNo) {
        return kamcoItemMapper.findByCltrNo(cltrNo);
    }
    
    /**
     * 공고번호로 조회
     */
    public KamcoItem getByPlnmNo(String plnmNo) {
        return kamcoItemMapper.findByPlnmNo(plnmNo);
    }
    
    /**
     * ID로 조회
     */
    public KamcoItem getById(Long id) {
        return kamcoItemMapper.findById(id);
    }
    
    /**
     * 전체 조회
     */
    public List<KamcoItem> getAllItems() {
        return kamcoItemMapper.findAll();
    }
    
    /**
     * 신규 물건 조회
     */
    public List<KamcoItem> getNewItems(int limit) {
        return kamcoItemMapper.findNewItems(limit);
    }
    
    /**
     * 신규 물건 조회 (시도 필터)
     */
    public List<KamcoItem> getNewItemsBySido(String sido) {
        return kamcoItemMapper.findNewItemsBySido(sido);
    }
    
    /**
     * 당일 매각 예정 물건 조회
     */
    public List<KamcoItem> getTodayClosingItems() {
        return kamcoItemMapper.findTodayClosingItems();
    }
    
    /**
     * 50% 체감 물건 조회
     */
    public List<KamcoItem> get50PercentDiscountItems(int limit) {
        return kamcoItemMapper.find50PercentDiscountItems(limit);
    }
    
    /**
     * 50% 체감 물건 조회 (시도 필터)
     */
    public List<KamcoItem> get50PercentDiscountItemsBySido(String sido) {
        return kamcoItemMapper.find50PercentDiscountItemsBySido(sido);
    }
    
    /**
     * 시도별 조회
     */
    public List<KamcoItem> getBySido(String sido) {
        return kamcoItemMapper.findBySido(sido);
    }
    
    /**
     * 검색
     */
    public List<KamcoItem> search(String keyword, int limit) {
        return kamcoItemMapper.searchByKeyword(keyword, limit);
    }

    // =============================================================================
    // 저장/업데이트 기능
    // =============================================================================
    
    /**
     * Item을 KamcoItem으로 변환하여 저장
     */
    @Transactional
    public void saveFromApiItem(Item apiItem) {
        // 입력값 검증
        if (apiItem == null) {
            throw new IllegalArgumentException("API Item이 null입니다.");
        }
        if (apiItem.getCltrNo() == null || apiItem.getCltrNo().trim().isEmpty()) {
            log.warn("⚠️ 물건번호가 없는 데이터 건너뜀: {}", apiItem);
            throw new IllegalArgumentException("물건번호(cltrNo)가 필수입니다.");
        }
        
        try {
            KamcoItem kamcoItem = convertToKamcoItem(apiItem);
            
            // 변환 후 재검증
            if (kamcoItem.getCltrNo() == null || kamcoItem.getCltrNo().trim().isEmpty()) {
                log.warn("⚠️ 변환 후 물건번호가 없는 데이터 건너뜀: cltrNo={}", apiItem.getCltrNo());
                throw new IllegalArgumentException("물건번호가 변환 후에도 없습니다.");
            }
            
            // 기존 물건인지 확인
            KamcoItem existing = kamcoItemMapper.findByCltrNo(kamcoItem.getCltrNo());
            
            if (existing == null) {
                // 신규 물건
                kamcoItem.setIsNew(true);
                kamcoItem.setIsActive(true);
                log.debug("✨ 신규 물건 저장: {}", kamcoItem.getCltrNm());
            } else {
                // 기존 물건 업데이트 - 통계 정보는 유지
                kamcoItem.setId(existing.getId());
                kamcoItem.setViewCount(existing.getViewCount());
                kamcoItem.setInterestCount(existing.getInterestCount());
                kamcoItem.setIsNew(existing.getIsNew());
                log.debug("♻️ 기존 물건 업데이트: ID={}, {}", existing.getId(), kamcoItem.getCltrNm());
            }
            
            kamcoItemMapper.insertOrUpdate(kamcoItem);
            log.debug("✅ 물건 저장 성공: cltrNo={}", kamcoItem.getCltrNo());
            
        } catch (IllegalArgumentException e) {
            // 검증 오류는 그대로 전달
            throw e;
        } catch (Exception e) {
            log.error("❌ 물건 저장 실패: cltrNo={}, 오류: {}", 
                apiItem.getCltrNo(), e.getMessage(), e);
            throw new RuntimeException("물건 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * Item을 KamcoItem으로 변환하여 저장하고 반환 (Admin용)
     */
    @Transactional
    public KamcoItem saveFromApiItemAndReturn(Item apiItem) {
        log.info("📦 [Service] saveFromApiItemAndReturn() 시작");
        log.info("   물건번호: {}", apiItem.getCltrNo());
        
        KamcoItem kamcoItem = convertToKamcoItem(apiItem);
        
        // 기존 물건인지 확인
        log.info("🔍 기존 데이터 확인 중... (SELECT WHERE cltr_no='{}')", kamcoItem.getCltrNo());
        KamcoItem existing = kamcoItemMapper.findByCltrNo(kamcoItem.getCltrNo());
        
        if (existing == null) {
            // 신규 물건
            kamcoItem.setIsNew(true);
            kamcoItem.setIsActive(true);
            log.info("✨ [신규 물건] INSERT 실행: {}", kamcoItem.getCltrNm());
        } else {
            // 기존 물건 업데이트 - 통계 정보는 유지
            kamcoItem.setId(existing.getId());
            kamcoItem.setViewCount(existing.getViewCount());
            kamcoItem.setInterestCount(existing.getInterestCount());
            kamcoItem.setIsNew(existing.getIsNew());
            log.info("♻️ [기존 물건] UPDATE 실행: ID={}, {}", existing.getId(), kamcoItem.getCltrNm());
        }
        
        kamcoItemMapper.insertOrUpdate(kamcoItem);
        log.info("✅ [DB 저장 완료] INSERT/UPDATE 성공");
        
        // 저장 후 ID가 생성된 경우에만 재조회, 아니면 저장된 객체 반환
        if (kamcoItem.getId() == null) {
            // ID가 없으면 재조회 (신규 저장인 경우)
            log.info("🔍 저장된 데이터 재조회 중... (신규 저장)");
            KamcoItem saved = kamcoItemMapper.findByCltrNo(kamcoItem.getCltrNo());
            log.info("✅ [Service] saveFromApiItemAndReturn() 완료");
            return saved;
        } else {
            // ID가 있으면 저장된 객체 그대로 반환 (업데이트인 경우)
            log.info("✅ [Service] saveFromApiItemAndReturn() 완료 (업데이트)");
            return kamcoItem;
        }
    }
    
    /**
     * 같은 물건번호의 모든 입찰 이력 조회
     */
    public List<KamcoItem> getAllByCltrNo(String cltrNo) {
        return kamcoItemMapper.findAllByCltrNo(cltrNo);
    }
    
    /**
     * ID로 물건 삭제
     */
    @Transactional
    public boolean deleteById(Long id) {
        try {
            log.info("🗑️ [Service] deleteById() 시작: ID={}", id);
            log.info("💾 DELETE FROM KNKamcoItem WHERE id={} 실행 중...", id);
            
            kamcoItemMapper.deleteById(id);
            
            log.info("✅ [Service] deleteById() 완료: 물건 삭제 성공");
            return true;
        } catch (Exception e) {
            log.error("❌ [Service] deleteById() 실패: id={}, 오류: {}", id, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Item 리스트를 KamcoItem으로 변환하여 일괄 저장
     */
    @Transactional
    public int saveBatchFromApiItems(List<Item> apiItems) {
        if (apiItems == null || apiItems.isEmpty()) {
            log.warn("⚠️ 저장할 데이터가 없습니다.");
            return 0;
        }
        
        int savedCount = 0;
        int errorCount = 0;
        List<String> errorItems = new ArrayList<>();
        
        log.info("📦 배치 저장 시작: 총 {}개", apiItems.size());
        
        for (Item apiItem : apiItems) {
            try {
                saveFromApiItem(apiItem);
                savedCount++;
                
                // 100개마다 진행 상황 로그
                if (savedCount % 100 == 0) {
                    log.info("📊 진행 상황: {}개 저장 완료 / {}개", savedCount, apiItems.size());
                }
                
            } catch (IllegalArgumentException e) {
                // 검증 오류는 경고만
                errorCount++;
                String cltrNo = apiItem != null && apiItem.getCltrNo() != null ? apiItem.getCltrNo() : "unknown";
                errorItems.add(cltrNo + ": " + e.getMessage());
                log.warn("⚠️ 물건 저장 건너뜀: {}, 사유: {}", cltrNo, e.getMessage());
            } catch (Exception e) {
                errorCount++;
                String cltrNo = apiItem != null && apiItem.getCltrNo() != null ? apiItem.getCltrNo() : "unknown";
                errorItems.add(cltrNo + ": " + e.getMessage());
                log.error("❌ 물건 저장 실패: {}, 오류: {}", cltrNo, e.getMessage());
            }
        }
        
        log.info("✅ 배치 저장 완료: {}개 성공 / {}개 전체 (실패: {}개)", 
            savedCount, apiItems.size(), errorCount);
        
        if (errorCount > 0 && errorCount <= 10) {
            log.warn("⚠️ 실패한 물건 목록: {}", String.join(", ", errorItems));
        } else if (errorCount > 10) {
            log.warn("⚠️ 실패한 물건이 너무 많습니다 ({}개). 처음 10개만 표시: {}", 
                errorCount, String.join(", ", errorItems.subList(0, Math.min(10, errorItems.size()))));
        }
        
        return savedCount;
    }

    // =============================================================================
    // 통계 기능
    // =============================================================================
    
    /**
     * 조회수 증가 (조회 이력도 함께 저장)
     */
    @Transactional
    public void incrementViewCount(Long id, String memberId, String ipAddress, String userAgent) {
        KamcoItem item = kamcoItemMapper.findById(id);
        if (item != null) {
            // 조회수 증가
            kamcoItemMapper.incrementViewCount(id);
            
            // 조회 이력 저장
            KamcoItemViewLog viewLog = new KamcoItemViewLog();
            viewLog.setItemId(id);
            viewLog.setCltrNo(item.getCltrNo());
            viewLog.setMemberId(memberId);
            viewLog.setIpAddress(ipAddress);
            viewLog.setUserAgent(userAgent);
            viewLogMapper.insert(viewLog);
        }
    }
    
    /**
     * 관심수 증가 (즐겨찾기 추가 시 호출)
     */
    @Transactional
    public void incrementInterestCount(String cltrNo) {
        kamcoItemMapper.incrementInterestCount(cltrNo);
    }
    
    /**
     * 관심수 감소 (즐겨찾기 제거 시 호출)
     */
    @Transactional
    public void decrementInterestCount(String cltrNo) {
        kamcoItemMapper.decrementInterestCount(cltrNo);
    }
    
    /**
     * 서울특별시가 아닌 데이터 삭제
     */
    @Transactional
    public int deleteNonSeoulItems() {
        log.info("🗑️ [Service] deleteNonSeoulItems() 시작");
        log.info("💾 DELETE FROM KNKamcoItem WHERE sido != '서울특별시' 실행 중...");
        int deleted = kamcoItemMapper.deleteNonSeoulItems();
        log.info("✅ [Service] deleteNonSeoulItems() 완료: {}개 데이터 삭제", deleted);
        return deleted;
    }
    
    /**
     * 전체 데이터 삭제
     */
    @Transactional
    public int deleteAllItems() {
        log.info("⚠️⚠️⚠️ [Service] deleteAllItems() 시작 ⚠️⚠️⚠️");
        log.info("💾 DELETE FROM KNKamcoItem 실행 중...");
        int deleted = kamcoItemMapper.deleteAll();
        log.info("✅ [Service] deleteAllItems() 완료: {}개 데이터 삭제", deleted);
        return deleted;
    }

    // =============================================================================
    // 유틸리티 메서드
    // =============================================================================
    
    /**
     * API Item을 KamcoItem으로 변환
     */
    private KamcoItem convertToKamcoItem(Item apiItem) {
        if (apiItem == null) {
            throw new IllegalArgumentException("API Item이 null입니다.");
        }
        
        KamcoItem kamcoItem = new KamcoItem();
        
        // 기본 식별 정보
        try {
            if (apiItem.getRnum() != null && !apiItem.getRnum().trim().isEmpty()) {
                kamcoItem.setRnum(Integer.parseInt(apiItem.getRnum()));
            }
        } catch (NumberFormatException e) {
            log.warn("⚠️ rnum 파싱 실패: {}, 기본값 null 사용", apiItem.getRnum());
            kamcoItem.setRnum(null);
        }
        kamcoItem.setPlnmNo(apiItem.getPlnmNo());
        kamcoItem.setPbctNo(apiItem.getPbctNo());
        kamcoItem.setOrgBaseNo(apiItem.getOrgBaseNo());
        kamcoItem.setOrgNm(apiItem.getOrgNm());
        kamcoItem.setCltrNo(apiItem.getCltrNo());
        kamcoItem.setPbctCdtnNo(apiItem.getPbctCdtnNo());
        kamcoItem.setCltrMnmtNo(apiItem.getCltrMnmtNo());
        kamcoItem.setCltrHstrNo(apiItem.getCltrHstrNo());
        kamcoItem.setBidMnmtNo(apiItem.getBidMnmtNo());
        
        // 분류 정보
        kamcoItem.setScrnGrpCd(apiItem.getScrnGrpCd());
        kamcoItem.setCtgrId(apiItem.getCtgrId());
        kamcoItem.setCtgrFullNm(apiItem.getCtgrFullNm());
        
        // 물건 정보
        kamcoItem.setCltrNm(apiItem.getCltrNm());
        kamcoItem.setGoodsNm(apiItem.getGoodsNm());
        kamcoItem.setManf(apiItem.getManf());
        
        // 주소 정보
        kamcoItem.setLdnmAdrs(apiItem.getLdnmAdrs());
        kamcoItem.setNmrdAdrs(apiItem.getNmrdAdrs());
        kamcoItem.setRodNm(apiItem.getRodNm());
        kamcoItem.setBldNo(apiItem.getBldNo());
        kamcoItem.setSido(extractSido(apiItem.getLdnmAdrs()));
        
        // 처분/입찰 방식
        kamcoItem.setDpslMtdCd(apiItem.getDpslMtdCd());
        kamcoItem.setDpslMtdNm(apiItem.getDpslMtdNm());
        kamcoItem.setBidMtdNm(apiItem.getBidMtdNm());
        
        // 가격 정보 (마이너스 값 검증)
        Long minBidPrc = apiItem.getMinBidPrc();
        if (minBidPrc != null && minBidPrc > 0) {
            kamcoItem.setMinBidPrc(minBidPrc);
        } else {
            kamcoItem.setMinBidPrc(null);
        }
        Long apslAsesAvgAmt = apiItem.getApslAsesAvgAmt();
        if (apslAsesAvgAmt != null && apslAsesAvgAmt > 0) {
            kamcoItem.setApslAsesAvgAmt(apslAsesAvgAmt);
        } else {
            kamcoItem.setApslAsesAvgAmt(null);
        }
        kamcoItem.setFeeRate(apiItem.getFeeRate());
        
        // 입찰 일정
        kamcoItem.setPbctBegnDtm(apiItem.getPbctBegnDtm());
        kamcoItem.setPbctClsDtm(apiItem.getPbctClsDtm());
        
        // 상태 정보
        kamcoItem.setPbctCltrStatNm(apiItem.getPbctCltrStatNm());
        kamcoItem.setUscbCnt(apiItem.getUscbCnt());
        kamcoItem.setIqryCnt(apiItem.getIqryCnt());
        
        // 통계 정보 초기화
        kamcoItem.setViewCount(0);
        kamcoItem.setInterestCount(0);
        
        // API 동기화 정보
        kamcoItem.setIsNew(true);
        kamcoItem.setIsActive(true);
        kamcoItem.setApiSyncDate(LocalDateTime.now());
        
        return kamcoItem;
    }
    
    /**
     * 주소에서 시도 추출
     */
    private String extractSido(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        
        if (address.startsWith("서울특별시")) return "서울특별시";
        if (address.startsWith("부산광역시")) return "부산광역시";
        if (address.startsWith("대구광역시")) return "대구광역시";
        if (address.startsWith("인천광역시")) return "인천광역시";
        if (address.startsWith("광주광역시")) return "광주광역시";
        if (address.startsWith("대전광역시")) return "대전광역시";
        if (address.startsWith("울산광역시")) return "울산광역시";
        if (address.startsWith("세종특별자치시")) return "세종특별자치시";
        if (address.startsWith("경기도")) return "경기도";
        if (address.startsWith("강원")) return "강원도";
        if (address.startsWith("충청북도") || address.startsWith("충북")) return "충청북도";
        if (address.startsWith("충청남도") || address.startsWith("충남")) return "충청남도";
        if (address.startsWith("전라북도") || address.startsWith("전북")) return "전북특별자치도";
        if (address.startsWith("전라남도") || address.startsWith("전남")) return "전라남도";
        if (address.startsWith("경상북도") || address.startsWith("경북")) return "경상북도";
        if (address.startsWith("경상남도") || address.startsWith("경남")) return "경상남도";
        if (address.startsWith("제주")) return "제주특별자치도";
        
        return address.length() >= 3 ? address.substring(0, 3) : address;
    }
    
    // =============================================================================
    // PublicAuctionInfoService 통합 메서드
    // =============================================================================
    
}


