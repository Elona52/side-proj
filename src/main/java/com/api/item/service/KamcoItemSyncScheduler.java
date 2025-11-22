package com.api.item.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.api.item.domain.KamcoItem;
import com.api.item.domain.KamcoItemStats;
import com.api.item.domain.Item;
import com.api.item.domain.NewItemNotification;
import com.api.item.mapper.KamcoItemMapper;
import com.api.item.mapper.KamcoItemStatsMapper;
import com.api.item.mapper.KamcoItemViewLogMapper;
import com.api.item.mapper.NewItemNotificationMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KamcoItemSyncScheduler {

    private final OnbidApiService onbidApiService;
    private final KamcoItemService kamcoItemService;
    private final KamcoItemMapper kamcoItemMapper;
    private final NewItemNotificationMapper notificationMapper;
    private final KamcoItemStatsMapper statsMapper;
    private final KamcoItemViewLogMapper viewLogMapper;

    // 동기화할 시도 목록 (서울특별시만)
    private static final String[] SIDO_LIST = {
        "서울특별시"
        // 다른 지역 추가 예시:
        // "경기도",
        // "인천광역시",
        // "부산광역시"
    };

    // =============================================================================
    // 스케줄러 작업들
    // =============================================================================
    
    /**
     * 전체 물건 동기화 (매일 오전 2시 실행)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void syncAllItems() {
        log.info("🔄 ==================== 전체 물건 동기화 시작 ====================");
        
        int totalSynced = 0;
        int totalNew = 0;
        
        try {
            // 1. 모든 API에서 서울특별시 데이터 가져오기
            for (String sido : SIDO_LIST) {
                try {
                    log.info("📍 {} 전체 물건 조회 시작...", sido);
                    
                // 1-1. 용도별 물건 (페이징 - 최대 5000개)
                for (int page = 1; page <= 50; page++) {
                        List<Item> items = onbidApiService.getUnifyUsageCltr(sido, page, 100);
                        if (items != null && !items.isEmpty()) {
                            int synced = kamcoItemService.saveBatchFromApiItems(items);
                            totalSynced += synced;
                            log.info("✅ 용도별 물건 {}페이지: {}개", page, synced);
                            Thread.sleep(500);
                        } else {
                            break; // 더 이상 데이터 없음
                        }
                    }
                    
                // 1-2. 신규 물건 (페이징 - 최대 1000개)
                for (int page = 1; page <= 10; page++) {
                        List<Item> newItems = onbidApiService.getUnifyNewCltrList(sido, page, 100);
                        if (newItems != null && !newItems.isEmpty()) {
                            int synced = kamcoItemService.saveBatchFromApiItems(newItems);
                            totalSynced += synced;
                            log.info("✅ 신규 물건 {}페이지: {}개", page, synced);
                            Thread.sleep(500);
                        } else {
                            break;
                        }
                    }
                    
                // 1-3. 마감임박 물건 (페이징 - 최대 1000개)
                for (int page = 1; page <= 10; page++) {
                        List<Item> deadlineItems = onbidApiService.getUnifyDeadlineCltrList(sido, page, 100);
                        if (deadlineItems != null && !deadlineItems.isEmpty()) {
                            int synced = kamcoItemService.saveBatchFromApiItems(deadlineItems);
                            totalSynced += synced;
                            log.info("✅ 마감임박 물건 {}페이지: {}개", page, synced);
                            Thread.sleep(500);
                        } else {
                            break;
                        }
                    }
                    
                // 1-4. 50% 체감 물건 (페이징 - 최대 1000개)
                for (int page = 1; page <= 10; page++) {
                        List<Item> discountItems = onbidApiService.getUnifyDegression50PerCltrList(sido, page, 100);
                        if (discountItems != null && !discountItems.isEmpty()) {
                            int synced = kamcoItemService.saveBatchFromApiItems(discountItems);
                            totalSynced += synced;
                            log.info("✅ 50% 체감 물건 {}페이지: {}개", page, synced);
                            Thread.sleep(500);
                        } else {
                            break;
                        }
                    }
                    
                    log.info("✅ {} 전체 동기화 완료: 총 {}개", sido, totalSynced);
                    
                } catch (Exception e) {
                    log.error("❌ {} 동기화 실패: {}", sido, e.getMessage());
                }
            }
            
            // 2. 새로운 물건 공지사항 생성
            createNewItemNotifications();
            
            log.info("✅ ==================== 전체 물건 동기화 완료 ====================");
            log.info("📊 총 동기화: {}개, 신규: {}개", totalSynced, totalNew);
            
        } catch (Exception e) {
            log.error("❌ 전체 물건 동기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 신규 물건만 동기화 (매시간 실행)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void syncNewItems() {
        log.info("🔄 신규 물건 동기화 시작...");
        
        try {
            int totalSynced = 0;
            
            for (String sido : SIDO_LIST) {
                try {
                    List<Item> newItems = onbidApiService.getUnifyNewCltrList(sido, 1, 50);
                    
                    if (newItems != null && !newItems.isEmpty()) {
                        int synced = kamcoItemService.saveBatchFromApiItems(newItems);
                        totalSynced += synced;
                    }
                    
                    Thread.sleep(500);
                    
                } catch (Exception e) {
                    log.error("❌ {} 신규 물건 동기화 실패: {}", sido, e.getMessage());
                }
            }
            
            // 새로운 물건 공지사항 생성
            createNewItemNotifications();
            
            log.info("✅ 신규 물건 동기화 완료: {}개", totalSynced);
            
        } catch (Exception e) {
            log.error("❌ 신규 물건 동기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 마감임박 물건 동기화 (4시간마다 실행)
     */
    @Scheduled(cron = "0 0 */4 * * *")
    public void syncDeadlineItems() {
        log.info("🔄 마감임박 물건 동기화 시작...");
        
        try {
            int totalSynced = 0;
            
            for (String sido : SIDO_LIST) {
                try {
                    List<Item> deadlineItems = onbidApiService.getUnifyDeadlineCltrList(sido, 1, 50);
                    
                    if (deadlineItems != null && !deadlineItems.isEmpty()) {
                        int synced = kamcoItemService.saveBatchFromApiItems(deadlineItems);
                        totalSynced += synced;
                    }
                    
                    Thread.sleep(500);
                    
                } catch (Exception e) {
                    log.error("❌ {} 마감임박 물건 동기화 실패: {}", sido, e.getMessage());
                }
            }
            
            log.info("✅ 마감임박 물건 동기화 완료: {}개", totalSynced);
            
        } catch (Exception e) {
            log.error("❌ 마감임박 물건 동기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 새로운 물건 공지사항 생성
     */
    private void createNewItemNotifications() {
        log.info("🔔 새로운 물건 공지사항 생성 시작...");
        
        try {
            // 최근 24시간 이내 생성된 신규 물건 조회
            List<KamcoItem> newItems = kamcoItemMapper.findNewItems(50);
            
            int createdCount = 0;
            for (KamcoItem item : newItems) {
                try {
                    NewItemNotification notification = new NewItemNotification();
                    notification.setItemId(item.getId());
                    notification.setCltrNo(item.getCltrNo());
                    notification.setCltrNm(item.getCltrNm());
                    notification.setMinBidPrc(item.getMinBidPrc());
                    notification.setPbctClsDtm(item.getPbctClsDtm());
                    notification.setNotificationType("NEW");
                    notification.setIsDisplayed(true);
                    notification.setDisplayOrder(0);
                    notification.setExpiredDate(LocalDateTime.now().plusDays(7)); // 7일 후 만료
                    
                    notificationMapper.insert(notification);
                    createdCount++;
                    
                } catch (Exception e) {
                    // 중복 등의 이유로 실패할 수 있음 (무시)
                }
            }
            
            log.info("✅ 새로운 물건 공지사항 생성 완료: {}개", createdCount);
            
        } catch (Exception e) {
            log.error("❌ 새로운 물건 공지사항 생성 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 통계 스냅샷 생성 (매일 자정 실행)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void createDailyStats() {
        log.info("📊 일일 통계 스냅샷 생성 시작...");
        
        try {
            List<KamcoItem> allItems = kamcoItemMapper.findAll();
            int createdCount = 0;
            
            for (KamcoItem item : allItems) {
                try {
                    KamcoItemStats stats = new KamcoItemStats();
                    stats.setItemId(item.getId());
                    stats.setCltrNo(item.getCltrNo());
                    stats.setStatDate(LocalDate.now());
                    stats.setViewCount(item.getViewCount());
                    stats.setInterestCount(item.getInterestCount());
                    
                    statsMapper.insertOrUpdate(stats);
                    createdCount++;
                    
                } catch (Exception e) {
                    // 실패 시 로그만 남기고 계속 진행
                    log.debug("통계 스냅샷 생성 실패: {}", item.getCltrNo());
                }
            }
            
            log.info("✅ 일일 통계 스냅샷 생성 완료: {}개", createdCount);
            
        } catch (Exception e) {
            log.error("❌ 일일 통계 스냅샷 생성 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 데이터 정리 작업 (매일 오전 3시 실행)
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupData() {
        log.info("🧹 데이터 정리 작업 시작...");
        
        try {
            // 1. 오래된 신규 물건 플래그 해제 (7일 이상)
            kamcoItemMapper.unmarkOldNewItems();
            log.info("✅ 오래된 신규 물건 플래그 해제 완료");
            
            // 2. 종료된 물건 비활성화
            kamcoItemMapper.deactivateExpiredItems();
            log.info("✅ 종료된 물건 비활성화 완료");
            
            // 3. 만료된 공지사항 삭제
            notificationMapper.deleteExpired();
            log.info("✅ 만료된 공지사항 삭제 완료");
            
            // 4. 오래된 공지사항 숨김 처리 (30일 이상)
            notificationMapper.hideOldNotifications();
            log.info("✅ 오래된 공지사항 숨김 처리 완료");
            
            // 5. 오래된 조회 이력 삭제 (30일 이상)
            viewLogMapper.deleteOldLogs();
            log.info("✅ 오래된 조회 이력 삭제 완료");
            
            // 6. 오래된 통계 삭제 (90일 이상)
            statsMapper.deleteOldStats();
            log.info("✅ 오래된 통계 삭제 완료");
            
            log.info("✅ 데이터 정리 작업 완료");
            
        } catch (Exception e) {
            log.error("❌ 데이터 정리 작업 중 오류 발생: {}", e.getMessage());
        }
    }
    
    /**
     * 수동 동기화 (관리자가 호출)
     */
    public void manualSync() {
        log.info("🔄 수동 동기화 시작...");
        syncAllItems();
    }
}

