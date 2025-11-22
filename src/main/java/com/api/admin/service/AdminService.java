package com.api.admin.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.item.domain.Item;
import com.api.item.domain.KamcoItem;
import com.api.member.domain.Member;
import com.api.item.domain.PublicAuctionInfo;
import com.api.item.dto.ItemWithHistory;
import com.api.item.dto.KamcoItemResponse;
import com.api.member.dto.MemberResponse;
import com.api.common.dto.ServiceResponse;
import com.api.member.mapper.MemberMapper;
import com.api.item.mapper.PublicAuctionInfoMapper;
import com.api.item.service.OnbidApiService;
import com.api.item.service.KamcoItemService;
import com.api.item.service.KamcoItemSyncScheduler;
import com.api.member.service.MemberService;
import com.api.auction.service.AuctionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final OnbidApiService onbidApiService;
    private final KamcoItemService kamcoItemService;
    private final KamcoItemSyncScheduler syncScheduler;
    private final PublicAuctionInfoMapper publicAuctionInfoMapper;
    private final MemberService memberService;
    private final MemberMapper memberMapper;
    private final AuctionService auctionService;
    /**
     * 물건번호로 단건 조회 (Admin용)
     */
    public Item getItemByCltrNo(String cltrNo) {
        try {
            // 서울특별시에서 먼저 검색
            List<Item> items = onbidApiService.getUnifyUsageCltr("서울특별시", 1, 100);
            
            if (items != null) {
                for (Item item : items) {
                    if (cltrNo.equals(item.getCltrNo())) {
                        log.info("✅ 물건 조회 성공: {}", cltrNo);
                        return item;
                    }
                }
            }
            
            // 신규 물건에서 검색
            items = onbidApiService.getUnifyNewCltrList("서울특별시", 1, 100);
            if (items != null) {
                for (Item item : items) {
                    if (cltrNo.equals(item.getCltrNo())) {
                        log.info("✅ 물건 조회 성공 (신규): {}", cltrNo);
                        return item;
                    }
                }
            }
            
            log.warn("⚠️ 물건을 찾을 수 없음: {}", cltrNo);
            return null;
            
        } catch (Exception e) {
            log.error("❌ 물건 단건 조회 실패: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 물건 이력 조회 (Admin용)
     */
    public ItemWithHistory getItemWithHistory(String cltrNo) {
        try {
            Item item = getItemByCltrNo(cltrNo);
            
            if (item == null) {
                return null;
            }
            
            // ItemWithHistory로 변환
            ItemWithHistory history = new ItemWithHistory();
            history.setCltrNo(cltrNo);
            history.setCltrNm(item.getCltrNm());
            history.setLdnmAdrs(item.getLdnmAdrs());
            history.setLatest(item);
            history.setPast(new ArrayList<>()); // API에서는 이력 조회 불가, DB에서 조회 필요
            history.setTotalBidCount(1);
            history.setFirstPrice(item.getApslAsesAvgAmt());
            history.setCurrentPrice(item.getMinBidPrc());
            
            log.info("✅ 물건 이력 조회 성공: {}", cltrNo);
            return history;
            
        } catch (Exception e) {
            log.error("❌ 물건 이력 조회 실패: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 날짜 문자열을 SQL Date로 변환
     */
    public java.sql.Date[] parseDateRange(String startDate, String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localStartDate = LocalDate.parse(startDate, formatter);
        java.sql.Date sqlStartDate = java.sql.Date.valueOf(localStartDate);
        LocalDate localEndDate = LocalDate.parse(endDate, formatter);
        java.sql.Date sqlEndDate = java.sql.Date.valueOf(localEndDate);
        return new java.sql.Date[]{sqlStartDate, sqlEndDate};
    }
    
    /**
     * API에서 단건 조회
     */
    public Map<String, Object> fetchSingleFromApi(String cltrNo) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Item item = getItemByCltrNo(cltrNo);
            
            if (item == null) {
                response.put("success", false);
                response.put("message", "물건을 찾을 수 없습니다.");
                return response;
            }
            
            response.put("success", true);
            response.put("source", "API");
            response.put("data", item);
            response.put("message", "API에서 성공적으로 조회했습니다.");
            
        } catch (Exception e) {
            log.error("❌ API 단건 조회 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * API에서 이력 조회
     */
    public Map<String, Object> fetchHistoryFromApi(String cltrNo) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ItemWithHistory history = getItemWithHistory(cltrNo);
            
            if (history == null) {
                response.put("success", false);
                response.put("message", "물건 이력을 찾을 수 없습니다.");
                return response;
            }
            
            response.put("success", true);
            response.put("source", "API");
            response.put("cltrNo", cltrNo);
            response.put("itemName", history.getCltrNm());
            response.put("totalBidCount", history.getTotalBidCount());
            response.put("latest", history.getLatest());
            response.put("pastBids", history.getPast());
            response.put("priceDropRate", history.getPriceDropRate());
            response.put("message", "API에서 이력을 성공적으로 조회했습니다.");
            
        } catch (Exception e) {
            log.error("❌ API 이력 조회 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "이력 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * API 데이터를 DB에 저장
     */
    public Map<String, Object> saveToDatabase(String cltrNo) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. API에서 데이터 조회
            Item item = getItemByCltrNo(cltrNo);
            
            if (item == null) {
                response.put("success", false);
                response.put("message", "API에서 물건을 찾을 수 없습니다.");
                return response;
            }
            
            // 2. DB에 저장
            KamcoItem saved = kamcoItemService.saveFromApiItemAndReturn(item);
            
            response.put("success", true);
            response.put("message", "DB에 성공적으로 저장했습니다.");
            response.put("savedId", saved.getId());
            response.put("cltrNo", saved.getCltrNo());
            response.put("itemName", saved.getCltrNm());
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "입력값 오류: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ DB 저장 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "저장 중 오류가 발생했습니다: " + e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
        }
        
        return response;
    }
    
    /**
     * API 데이터 일괄 저장
     */
    public Map<String, Object> saveBatchToDatabase(String sido, String type, int page, int numOfRows) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Item> items = new ArrayList<>();
            
            if ("all".equals(type)) {
                // 전체 저장: 여러 페이지에서 데이터 가져오기
                log.info("📡 전체 데이터 저장 시작... (최대 50페이지, 페이지당 100개)");
                int emptyPageCount = 0;
                int maxEmptyPages = 5;
                
                // 첫 페이지에서 totalCount 확인
                List<Item> firstPage = onbidApiService.getUnifyUsageCltr(sido, 1, 100);
                if (firstPage != null && !firstPage.isEmpty()) {
                    items.addAll(firstPage);
                }
                Thread.sleep(300);
                
                // 2페이지부터 계속 조회
                for (int p = 2; p <= 50; p++) {
                    List<Item> pageItems = onbidApiService.getUnifyUsageCltr(sido, p, 100);
                    
                    if (pageItems == null || pageItems.isEmpty()) {
                        emptyPageCount++;
                        if (emptyPageCount >= maxEmptyPages) {
                            break;
                        }
                    } else {
                        emptyPageCount = 0;
                        items.addAll(pageItems);
                    }
                    
                    Thread.sleep(300);
                }
            } else {
                // 일부 저장
                items = onbidApiService.getUnifyUsageCltr(sido, page, numOfRows);
            }
            
            if (items == null || items.isEmpty()) {
                response.put("success", false);
                response.put("message", "API에서 물건을 찾을 수 없습니다.");
                return response;
            }
            
            // DB에 일괄 저장
            int savedCount = kamcoItemService.saveBatchFromApiItems(items);
            
            response.put("success", true);
            response.put("message", savedCount + "개 물건을 DB에 저장했습니다.");
            response.put("savedCount", savedCount);
            response.put("totalRequested", items.size());
            
        } catch (Exception e) {
            log.error("❌ DB 일괄 저장 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "일괄 저장 중 오류가 발생했습니다: " + e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
        }
        
        return response;
    }
    
    /**
     * 신규 물건 API 목록 조회
     */
    public Map<String, Object> getNewItemsFromApi(String sido, int page, int size) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Item> items = onbidApiService.getUnifyNewCltrList(sido, page, size);
            
            if (items == null) {
                items = new ArrayList<>();
            }
            
            int estimatedTotal = items.size() >= size ? (page * size) + 100 : items.size();
            
            response.put("success", true);
            response.put("source", "API");
            response.put("page", page);
            response.put("size", size);
            response.put("sido", sido);
            response.put("totalCount", estimatedTotal);
            response.put("currentPageCount", items.size());
            response.put("items", items);
            response.put("message", "신규 물건 API 조회 성공");
            
        } catch (Exception e) {
            log.error("❌ 신규 물건 API 조회 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "신규 물건 API 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 신규 물건 전체 저장
     */
    public Map<String, Object> saveNewItemsBatchToDatabase(String sido) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Item> allItems = new ArrayList<>();
            
            // 여러 페이지에서 신규 물건 가져오기 (최대 50페이지 = 5000개)
            for (int page = 1; page <= 50; page++) {
                List<Item> pageItems = onbidApiService.getUnifyNewCltrList(sido, page, 100);
                if (pageItems == null || pageItems.isEmpty()) {
                    break;
                }
                allItems.addAll(pageItems);
                Thread.sleep(300);
            }
            
            if (allItems.isEmpty()) {
                response.put("success", false);
                response.put("message", "API에서 신규 물건을 찾을 수 없습니다.");
                return response;
            }
            
            // DB에 일괄 저장
            int savedCount = kamcoItemService.saveBatchFromApiItems(allItems);
            
            response.put("success", true);
            response.put("message", savedCount + "개 신규 물건을 DB에 저장했습니다.");
            response.put("savedCount", savedCount);
            response.put("totalRequested", allItems.size());
            
        } catch (Exception e) {
            log.error("❌ 신규 물건 일괄 저장 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "일괄 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 서울특별시 데이터 즉시 동기화
     */
    public Map<String, Object> syncSeoulNow() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("🚀 서울특별시 데이터 즉시 동기화 시작...");
            
            syncScheduler.syncAllItems();
            
            List<KamcoItem> seoulItems = kamcoItemService.getBySido("서울특별시");
            
            result.put("success", true);
            result.put("totalCount", seoulItems.size());
            result.put("message", "동기화 완료! " + seoulItems.size() + "개 서울특별시 물건");
            
        } catch (Exception e) {
            log.error("❌ 동기화 실패", e);
            result.put("success", false);
            result.put("message", "동기화 실패: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 공매 물건 상세 정보 저장 또는 업데이트
     */
    @Transactional
    public int savePublicAuctionInfo(PublicAuctionInfo info) {
        try {
            log.info("공매 물건 상세 정보 저장/업데이트: pbctNo={}, cltrNo={}", info.getPbctNo(), info.getCltrNo());
            return publicAuctionInfoMapper.insertOrUpdate(info);
        } catch (Exception e) {
            log.error("공매 물건 상세 정보 저장/업데이트 실패: {}", e.getMessage(), e);
            throw new RuntimeException("공매 물건 상세 정보 저장 실패", e);
        }
    }

    /**
     * 여러 건 일괄 저장
     */
    @Transactional
    public int savePublicAuctionInfoBatch(List<PublicAuctionInfo> infoList) {
        int count = 0;
        for (PublicAuctionInfo info : infoList) {
            try {
                count += savePublicAuctionInfo(info);
            } catch (Exception e) {
                log.error("일괄 저장 중 오류 발생 (pbctNo={}, cltrNo={}): {}",
                    info.getPbctNo(), info.getCltrNo(), e.getMessage());
            }
        }
        log.info("공매 물건 상세 정보 일괄 저장 완료: {} 건", count);
        return count;
    }

    /**
     * 물건번호로 공매 상세 정보 조회
     */
    public PublicAuctionInfo getPublicAuctionInfoByCltrNo(String cltrNo) {
        return publicAuctionInfoMapper.findByCltrNo(cltrNo);
    }

    /**
     * 공매번호와 물건번호로 공매 상세 정보 조회
     */
    public PublicAuctionInfo getPublicAuctionInfoByPbctNoAndCltrNo(String pbctNo, String cltrNo) {
        return publicAuctionInfoMapper.findByPbctNoAndCltrNo(pbctNo, cltrNo);
    }

    /**
     * 전체 목록 조회 (페이징)
     */
    public List<PublicAuctionInfo> getAllPublicAuctionInfo(int page, int size) {
        int offset = (page - 1) * size;
        return publicAuctionInfoMapper.findAll(offset, size);
    }

    /**
     * 총 레코드 수
     */
    public int getPublicAuctionInfoTotalCount() {
        return publicAuctionInfoMapper.count();
    }

    /**
     * 공매 상세 정보 삭제
     */
    @Transactional
    public int deletePublicAuctionInfo(String pbctNo, String cltrNo) {
        log.info("공매 물건 상세 정보 삭제: pbctNo={}, cltrNo={}", pbctNo, cltrNo);
        return publicAuctionInfoMapper.delete(pbctNo, cltrNo);
    }

    /**
     * 공매 물건 상세 정보 저장 응답 생성
     */
    public ServiceResponse<Map<String, Object>> savePublicAuctionInfoResponse(PublicAuctionInfo info) {
        Map<String, Object> response = new HashMap<>();

        try {
            int result = savePublicAuctionInfo(info);
            response.put("success", result > 0);
            response.put("message", result > 0 ? "저장 성공" : "저장 실패");
            return ServiceResponse.ok(response);
        } catch (Exception e) {
            log.error("공매 물건 상세 정보 저장 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "저장 실패: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 공매 물건 상세 정보 일괄 저장 응답 생성
     */
    public ServiceResponse<Map<String, Object>> savePublicAuctionInfoBatchResponse(List<PublicAuctionInfo> infoList) {
        Map<String, Object> response = new HashMap<>();

        try {
            int count = savePublicAuctionInfoBatch(infoList);
            response.put("success", true);
            response.put("savedCount", count);
            response.put("message", count + "건 저장 완료");
            return ServiceResponse.ok(response);
        } catch (Exception e) {
            log.error("공매 물건 상세 정보 일괄 저장 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "일괄 저장 실패: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 공매 물건 상세 정보 삭제 응답 생성
     */
    public ServiceResponse<Map<String, Object>> deletePublicAuctionInfoResponse(String pbctNo, String cltrNo) {
        Map<String, Object> response = new HashMap<>();

        try {
            int result = deletePublicAuctionInfo(pbctNo, cltrNo);
            response.put("success", result > 0);
            response.put("message", result > 0 ? "삭제 성공" : "삭제할 데이터 없음");
            return ServiceResponse.ok(response);
        } catch (Exception e) {
            log.error("공매 물건 상세 정보 삭제 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "삭제 실패: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<Map<String, Object>> getItemFromDbResponse(Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            KamcoItem item = kamcoItemService.getById(id);

            if (item == null) {
                response.put("success", false);
                response.put("message", "DB에서 물건을 찾을 수 없습니다.");
                return ServiceResponse.of(HttpStatus.NOT_FOUND, response);
            }

            response.put("success", true);
            response.put("source", "DATABASE");
            response.put("data", KamcoItemResponse.from(item));
            response.put("message", "DB에서 성공적으로 조회했습니다.");

            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("❌ DB 단건 조회 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "조회 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<Map<String, Object>> getItemByCltrNoFromDbResponse(String cltrNo) {
        Map<String, Object> response = new HashMap<>();

        try {
            KamcoItem item = kamcoItemService.getByCltrNo(cltrNo);

            if (item == null) {
                response.put("success", false);
                response.put("message", "DB에서 물건을 찾을 수 없습니다.");
                return ServiceResponse.of(HttpStatus.NOT_FOUND, response);
            }

            response.put("success", true);
            response.put("source", "DATABASE");
            response.put("data", KamcoItemResponse.from(item));
            response.put("message", "DB에서 성공적으로 조회했습니다.");

            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("❌ DB 물건번호 조회 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "조회 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<Map<String, Object>> getHistoryFromDbResponse(String cltrNo) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<KamcoItem> items = kamcoItemService.getAllByCltrNo(cltrNo);

            if (items == null || items.isEmpty()) {
                response.put("success", false);
                response.put("message", "DB에서 물건 이력을 찾을 수 없습니다.");
                return ServiceResponse.of(HttpStatus.NOT_FOUND, response);
            }

            List<KamcoItemResponse> itemResponses = items.stream()
                    .map(KamcoItemResponse::from)
                    .toList();

            response.put("success", true);
            response.put("source", "DATABASE");
            response.put("cltrNo", cltrNo);
            response.put("totalCount", items.size());
            response.put("items", itemResponses);
            response.put("message", "DB에서 이력을 성공적으로 조회했습니다.");

            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("❌ DB 이력 조회 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "이력 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<Map<String, Object>> getSeoulItemsFromDbResponse(int page, int size) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<KamcoItem> items = kamcoItemService.getBySido("서울특별시");
            if (items == null) {
                items = new ArrayList<>();
            }

            List<KamcoItem> pagedItems = new ArrayList<>();
            if (!items.isEmpty()) {
                int start = (page - 1) * size;
                int end = Math.min(start + size, items.size());
                if (start < items.size()) {
                    pagedItems = items.subList(start, end);
                }
            }

            List<KamcoItemResponse> itemResponses = pagedItems.stream()
                    .map(KamcoItemResponse::from)
                    .toList();

            response.put("success", true);
            response.put("source", "DATABASE");
            response.put("page", page);
            response.put("size", size);
            response.put("sido", "서울특별시");
            response.put("totalCount", items.size());
            response.put("currentPageCount", itemResponses.size());
            response.put("items", itemResponses);
            response.put("message", "DB에서 서울특별시 목록을 성공적으로 조회했습니다.");

            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("❌ DB 서울특별시 목록 조회 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("source", "DATABASE");
            response.put("page", page);
            response.put("size", size);
            response.put("sido", "서울특별시");
            response.put("totalCount", 0);
            response.put("currentPageCount", 0);
            response.put("items", new ArrayList<>());
            response.put("message", "목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            response.put("error", e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<Map<String, Object>> getAllItemsWithoutPagingResponse() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<KamcoItem> items = kamcoItemService.getAllItems();
            List<KamcoItemResponse> itemResponses = items.stream()
                    .map(KamcoItemResponse::from)
                    .toList();

            response.put("success", true);
            response.put("source", "DATABASE");
            response.put("totalCount", items.size());
            response.put("items", itemResponses);
            response.put("message", "DB에서 전체 목록을 성공적으로 조회했습니다.");

            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("❌ DB 전체 목록 조회 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "전체 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<Map<String, Object>> getAllItemsWithPagingResponse(int page, int size) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<KamcoItem> items = kamcoItemService.getAllItems();

            int start = (page - 1) * size;
            int end = Math.min(start + size, items.size());
            List<KamcoItem> pagedItems = items.subList(start, end);

            List<KamcoItemResponse> itemResponses = pagedItems.stream()
                    .map(KamcoItemResponse::from)
                    .toList();

            response.put("success", true);
            response.put("source", "DATABASE");
            response.put("page", page);
            response.put("size", size);
            response.put("totalCount", items.size());
            response.put("currentPageCount", pagedItems.size());
            response.put("items", itemResponses);
            response.put("message", "DB에서 목록을 성공적으로 조회했습니다.");

            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("❌ DB 전체 목록 조회 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<Map<String, Object>> deleteItemByIdResponse(Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            KamcoItem item = kamcoItemService.getById(id);
            if (item == null) {
                response.put("success", false);
                response.put("message", "삭제할 물건을 찾을 수 없습니다.");
                return ServiceResponse.of(HttpStatus.NOT_FOUND, response);
            }

            boolean deleted = kamcoItemService.deleteById(id);
            if (deleted) {
                response.put("success", true);
                response.put("message", "물건을 성공적으로 삭제했습니다.");
                response.put("deletedId", id);
                response.put("deletedCltrNo", item.getCltrNo());
                response.put("deletedItemName", item.getCltrNm());
                return ServiceResponse.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "물건 삭제에 실패했습니다.");
                return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
            }

        } catch (Exception e) {
            log.error("❌ DB 삭제 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<Map<String, Object>> deleteItemByCltrNoResponse(String cltrNo) {
        Map<String, Object> response = new HashMap<>();

        try {
            KamcoItem item = kamcoItemService.getByCltrNo(cltrNo);
            if (item == null) {
                response.put("success", false);
                response.put("message", "삭제할 물건을 찾을 수 없습니다.");
                return ServiceResponse.of(HttpStatus.NOT_FOUND, response);
            }

            boolean deleted = kamcoItemService.deleteById(item.getId());
            if (deleted) {
                response.put("success", true);
                response.put("message", "물건을 성공적으로 삭제했습니다.");
                response.put("deletedCltrNo", cltrNo);
                response.put("deletedItemName", item.getCltrNm());
                return ServiceResponse.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "물건 삭제에 실패했습니다.");
                return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
            }

        } catch (Exception e) {
            log.error("❌ DB 물건번호 삭제 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<Map<String, Object>> deleteBatchItemsResponse(List<Long> ids) {
        Map<String, Object> response = new HashMap<>();

        try {
            int deletedCount = 0;
            for (Long id : ids) {
                if (kamcoItemService.deleteById(id)) {
                    deletedCount++;
                }
            }

            response.put("success", true);
            response.put("message", deletedCount + "개 물건을 삭제했습니다.");
            response.put("deletedCount", deletedCount);
            response.put("requestedCount", ids.size());
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("❌ DB 일괄 삭제 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "일괄 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<Map<String, Object>> deleteNonSeoulItemsResponse() {
        Map<String, Object> response = new HashMap<>();

        try {
            int deletedCount = kamcoItemService.deleteNonSeoulItems();
            response.put("success", true);
            response.put("message", "서울특별시가 아닌 데이터 삭제 완료");
            response.put("deletedCount", deletedCount);
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("❌ 서울 외 지역 삭제 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<Map<String, Object>> deleteAllItemsResponse() {
        Map<String, Object> response = new HashMap<>();

        try {
            int deletedCount = kamcoItemService.deleteAllItems();
            response.put("success", true);
            response.put("message", "전체 데이터 삭제 완료");
            response.put("deletedCount", deletedCount);
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("❌ 전체 삭제 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "전체 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<List<MemberResponse>> getAllMembersResponse() {
        try {
            List<Member> members = memberMapper != null ? memberMapper.findAllMembers() : List.of();
            List<MemberResponse> memberResponses = members.stream()
                    .map(MemberResponse::from)
                    .toList();
            return ServiceResponse.ok(memberResponses);
        } catch (Exception e) {
            log.error("❌ 회원 목록 조회 오류: {}", e.getMessage(), e);
            return ServiceResponse.ok(List.of());
        }
    }

    public ServiceResponse<Map<String, Object>> createDefaultAdminResponse() {
        Map<String, Object> response = new HashMap<>();

        try {
            Member existing = memberService.getMemberInfo("admin");
            if (existing != null) {
                response.put("success", true);
                response.put("message", "이미 admin 계정이 존재합니다.");
                response.put("id", existing.getId());
                response.put("type", existing.getType());
                return ServiceResponse.ok(response);
            }

            Member admin = new Member();
            admin.setId("admin");
            admin.setPass("tkdwkd22==");
            admin.setName("관리자");
            admin.setPhone("01000000000");
            admin.setMail("admin@example.com");
            admin.setZipcode(0);
            admin.setAddress1("");
            admin.setAddress2("");
            admin.setMarketing("N");
            admin.setType("ADMIN");

            memberService.insertMember(admin);

            response.put("success", true);
            response.put("message", "기본 관리자 계정을 생성했습니다.");
            response.put("id", "admin");
            response.put("type", "ADMIN");
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("❌ 기본 관리자 계정 생성 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "관리자 계정 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<Map<String, Object>> resetMemberPasswordResponse(String memberId, String newPassword) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (memberId == null || memberId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "memberId가 비어있습니다.");
                return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
            }
            if (newPassword == null || newPassword.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "새 비밀번호를 입력해주세요.");
                return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
            }

            boolean success = memberService.resetPassword(memberId, newPassword);
            if (success) {
                response.put("success", true);
                response.put("message", "비밀번호가 성공적으로 변경되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "해당 회원을 찾을 수 없습니다.");
            }
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("❌ 비밀번호 초기화 중 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "비밀번호 초기화 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<Map<String, Object>> deleteMemberResponse(String memberId) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (memberMapper == null) {
                response.put("success", false);
                response.put("message", "MemberMapper를 찾을 수 없습니다.");
                return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
            }
            memberMapper.deleteMember(memberId);
            response.put("success", true);
            response.put("message", "회원 삭제 완료: " + memberId);
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("❌ 회원 삭제 오류: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "삭제 실패: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<Map<String, Object>> updateMemberResponse(Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String memberId = request.get("memberId");
            if (memberId == null || memberId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "memberId가 비어있습니다.");
                return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
            }

            Member member = memberService.getMemberInfo(memberId);
            if (member == null) {
                response.put("success", false);
                response.put("message", "해당 회원을 찾을 수 없습니다.");
                return ServiceResponse.ok(response);
            }

            if (request.containsKey("name")) {
                member.setName(request.get("name"));
            }
            if (request.containsKey("mail")) {
                member.setMail(request.get("mail"));
            }
            if (request.containsKey("phone")) {
                member.setPhone(request.get("phone"));
            }

            memberMapper.updateMember(member);

            response.put("success", true);
            response.put("message", "회원 정보가 수정되었습니다.");
            response.put("member", MemberResponse.from(member));
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("❌ 회원 정보 수정 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "회원 정보 수정 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    public ServiceResponse<List<com.api.auction.domain.FindBoard>> getAllBoardsResponse() {
        try {
            List<com.api.auction.domain.FindBoard> boards = auctionService != null
                    ? auctionService.getBoardList(null, null, null)
                    : new ArrayList<>();
            return ServiceResponse.ok(boards);
        } catch (Exception e) {
            log.error("❌ 게시글 목록 조회 실패: {}", e.getMessage(), e);
            return ServiceResponse.ok(new ArrayList<>());
        }
    }

    public ServiceResponse<Map<String, Object>> deleteBoardResponse(int boardNo) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (auctionService != null) {
                auctionService.deleteBoard(boardNo);
                response.put("success", true);
                response.put("message", "게시글이 삭제되었습니다. no=" + boardNo);
            } else {
                response.put("success", false);
                response.put("message", "AuctionService를 찾을 수 없습니다.");
            }
            return ServiceResponse.ok(response);
        } catch (Exception e) {
            log.error("❌ 게시글 삭제 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "삭제 실패: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }
}

