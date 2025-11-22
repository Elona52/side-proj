package com.api.item.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.item.domain.Item;
import com.api.item.domain.NewItemNotification;
import com.api.item.dto.ItemWithHistory;
import com.api.item.mapper.NewItemNotificationMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 새로운 물건 공지사항 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NewItemNotificationService {

    private final NewItemNotificationMapper notificationMapper;
    private final OnbidApiService onbidApiService;

    /**
     * 전체 공지 조회
     */
    public List<NewItemNotification> getAllNotifications() {
        return notificationMapper.findAll();
    }
    
    /**
     * 타입별 공지 조회
     */
    public List<NewItemNotification> getNotificationsByType(String type, int limit) {
        return notificationMapper.findByType(type, limit);
    }
    
    /**
     * 신규 물건 공지 조회
     */
    public List<NewItemNotification> getNewItemNotifications(int limit) {
        return notificationMapper.findByType("NEW", limit);
    }
    
    /**
     * 가격 하락 공지 조회
     */
    public List<NewItemNotification> getPriceDropNotifications(int limit) {
        return notificationMapper.findByType("PRICE_DROP", limit);
    }
    
    /**
     * 마감 임박 공지 조회
     */
    public List<NewItemNotification> getDeadlineNotifications(int limit) {
        return notificationMapper.findByType("DEADLINE", limit);
    }

    // =============================================================================
    // ItemHistoryService 통합 메서드
    // =============================================================================

    /**
     * 물건별로 최신 입찰과 과거 이력을 구분하여 반환
     */
    public List<ItemWithHistory> getItemsWithHistory() {
        return getItemsWithHistory(null);
    }

    /**
     * 카테고리별 물건 조회
     */
    public List<ItemWithHistory> getItemsWithHistory(String category) {
        List<Item> items = onbidApiService.getUnifyUsageCltr(null, 1, 1000);

        // 카테고리 필터 적용
        if (category != null && !category.isEmpty() && !"all".equals(category)) {
            items = items.stream()
                .filter(item -> category.equals(item.getScrnGrpCd()))
                .collect(Collectors.toList());
        }

        // 물건번호(CLTR_NO)별로 그룹화
        Map<String, List<Item>> groupedByCltrNo = items.stream()
            .collect(Collectors.groupingBy(
                item -> item.getCltrNo() != null ? item.getCltrNo() : item.getCltrMnmtNo(),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<ItemWithHistory> result = new ArrayList<>();

        for (Map.Entry<String, List<Item>> entry : groupedByCltrNo.entrySet()) {
            List<Item> itemList = entry.getValue();

            // 입찰시작일시로 정렬 (오름차순: 과거 -> 미래)
            itemList.sort(Comparator.comparing(
                item -> item.getPbctBegnDtm() != null ? item.getPbctBegnDtm() : "",
                Comparator.naturalOrder()
            ));

            ItemWithHistory itemHistory = new ItemWithHistory();

            // 첫 번째 아이템에서 공통 정보 추출
            Item firstItem = itemList.get(0);
            itemHistory.setCltrNo(entry.getKey());
            itemHistory.setCltrMnmtNo(firstItem.getCltrMnmtNo());
            itemHistory.setCltrNm(firstItem.getCltrNm());
            itemHistory.setCtgrFullNm(firstItem.getCtgrFullNm());
            itemHistory.setGoodsNm(firstItem.getGoodsNm());
            itemHistory.setLdnmAdrs(firstItem.getLdnmAdrs());
            itemHistory.setNmrdAdrs(firstItem.getNmrdAdrs());

            // 최신 입찰 = 입찰시작일시가 가장 늦은 것 (가장 최근 공고)
            Item latestItem = itemList.get(itemList.size() - 1);
            itemHistory.setLatest(latestItem);

            // 과거 입찰 이력 = 최신을 제외한 나머지 (오래된 순서대로)
            List<Item> pastItems = itemList.size() > 1 
                ? new ArrayList<>(itemList.subList(0, itemList.size() - 1))
                : new ArrayList<>();
            itemHistory.setPast(pastItems);

            // 통계 정보
            itemHistory.setTotalBidCount(itemList.size());

            // 가격 분석
            Item oldestItem = itemList.get(0);
            Long firstPrice = oldestItem.getApslAsesAvgAmt();
            itemHistory.setFirstPrice(firstPrice);

            Long currentPrice = latestItem.getMinBidPrc();
            itemHistory.setCurrentPrice(currentPrice);

            if (firstPrice != null && currentPrice != null && firstPrice > 0) {
                double dropRate = ((firstPrice - currentPrice) * 100.0) / firstPrice;
                itemHistory.setPriceDropRate(Math.round(dropRate * 100.0) / 100.0);
            }

            result.add(itemHistory);
        }

        log.info("✅ 물건별 이력 분석 완료: {}개 물건", result.size());

        return result;
    }
}

