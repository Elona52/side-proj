package com.api.item.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.api.item.domain.KamcoItem;
import com.api.item.domain.NewItemNotification;
import com.api.common.dto.ServiceResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KamcoItemFacadeService {

    private final NewItemNotificationService notificationService;
    private final KamcoItemService kamcoItemService;
    private final KamcoItemSyncScheduler syncScheduler;

    public ServiceResponse<Map<String, Object>> buildMainDataResponse() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<NewItemNotification> newNotifications = notificationService.getNewItemNotifications(10);
            List<KamcoItem> todayItems = kamcoItemService.getTodayClosingItems();

            result.put("success", true);
            result.put("newNotifications", newNotifications);
            result.put("todayItems", todayItems);
            return ServiceResponse.ok(result);

        } catch (Exception e) {
            log.error("메인 데이터 조회 실패", e);
            result.put("success", false);
            result.put("message", "데이터 조회에 실패했습니다.");
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, result);
        }
    }

    public ServiceResponse<Map<String, Object>> handleManualSync() {
        Map<String, Object> result = new HashMap<>();

        try {
            syncScheduler.manualSync();
            result.put("success", true);
            result.put("message", "동기화가 시작되었습니다.");
            return ServiceResponse.ok(result);
        } catch (Exception e) {
            log.error("수동 동기화 실패", e);
            result.put("success", false);
            result.put("message", "동기화에 실패했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, result);
        }
    }

    public ServiceResponse<Map<String, Object>> clearNonSeoulDataResponse() {
        Map<String, Object> result = new HashMap<>();

        try {
            int deleted = kamcoItemService.deleteNonSeoulItems();
            result.put("success", true);
            result.put("deletedCount", deleted);
            result.put("message", deleted + "개 데이터 삭제 완료");
            return ServiceResponse.ok(result);
        } catch (Exception e) {
            log.error("데이터 삭제 실패", e);
            result.put("success", false);
            result.put("message", "삭제 실패: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, result);
        }
    }
}

