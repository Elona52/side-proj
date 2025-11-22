package com.api.favorite.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.favorite.service.FavoriteService;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 즐겨찾기 추가
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addFavorite(
            HttpSession session,
            @RequestBody Map<String, Object> requestBody) {
        log.info("========================================");
        log.info("=== 즐겨찾기 추가 API 호출 ===");
        log.info("========================================");
        log.info("requestBody: {}", requestBody);
        log.info("requestBody keys: {}", requestBody != null ? requestBody.keySet() : "null");
        log.info("requestBody itemId: {}", requestBody != null ? requestBody.get("itemId") : "null");
        log.info("requestBody cltrNo: {}", requestBody != null ? requestBody.get("cltrNo") : "null");
        
        String userId = (String) session.getAttribute("loginId");
        log.info("세션에서 가져온 userId: {}", userId);
        log.info("세션 전체 속성: {}", session != null ? java.util.Collections.list(session.getAttributeNames()) : "session is null");
        
        if (userId == null || userId.isEmpty()) {
            log.error("❌ 로그인되지 않은 사용자의 즐겨찾기 추가 시도");
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "로그인이 필요한 서비스입니다.");
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        
        try {
            ResponseEntity<Map<String, Object>> response = favoriteService.handleAddFavoriteRequest(userId, requestBody).toResponseEntity();
            log.info("즐겨찾기 추가 응답: status={}, body={}", response.getStatusCode(), response.getBody());
            log.info("========================================");
            return response;
        } catch (Exception e) {
            log.error("❌ 즐겨찾기 추가 중 예외 발생", e);
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "즐겨찾기 추가 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 즐겨찾기 삭제
     */
    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Map<String, Object>> removeFavorite(
            HttpSession session,
            @PathVariable("favoriteId") Long favoriteId) {
        log.info("=== 즐겨찾기 삭제 요청 ===");
        log.info("favoriteId: {}", favoriteId);
        
        String userId = (String) session.getAttribute("loginId");
        log.info("세션 userId: {}", userId);
        
        // 권한 체크: 자신의 즐겨찾기만 삭제 가능
        if (userId == null || userId.isEmpty()) {
            log.warn("로그인이 필요합니다.");
        }
        
        return favoriteService.handleRemoveFavoriteRequest(userId, favoriteId).toResponseEntity();
    }

    /**
     * 내 즐겨찾기 목록 조회 (물건 정보 포함)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyFavorites(
            HttpSession session) {
        log.info("=== 즐겨찾기 목록 조회 요청 ===");
        String userId = (String) session.getAttribute("loginId");
        log.info("세션에서 가져온 userId: {}", userId);
        
        ResponseEntity<Map<String, Object>> response = favoriteService.handleFavoritesResponse(userId).toResponseEntity();
        log.info("응답 상태: {}, 응답 body: {}", response.getStatusCode(), response.getBody());
        return response;
    }

    /**
     * 특정 물건 즐겨찾기 여부 확인
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkFavorite(
            HttpSession session,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) String cltrNo,
            @RequestParam(required = false) String itemPlnmNo) {
        String userId = (String) session.getAttribute("loginId");
        return favoriteService.handleFavoriteCheck(userId, itemId, cltrNo, itemPlnmNo).toResponseEntity();
    }

    /**
     * 가격 알림 히스토리 조회 (API)
     */
    @GetMapping("/alerts/api")
    public ResponseEntity<Map<String, Object>> getPriceAlerts(
            HttpSession session) {
        String userId = (String) session.getAttribute("loginId");
        return favoriteService.handlePriceAlertsResponse(userId).toResponseEntity();
    }
}
