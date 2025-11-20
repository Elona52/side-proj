package com.api.item.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.api.item.domain.KamcoItem;
import com.api.member.domain.Member;
import com.api.item.domain.NewItemNotification;
import com.api.item.dto.KamcoItemResponse;
import com.api.common.util.HttpUtilService;
import com.api.item.service.KamcoItemFacadeService;
import com.api.item.service.KamcoItemService;
import com.api.item.service.KamcoItemSyncScheduler;
import com.api.item.service.NewItemNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 캠코 온비드 공매 물건 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/kamco-items")
@RequiredArgsConstructor
public class KamcoItemController {

    private final KamcoItemService kamcoItemService;
    private final KamcoItemSyncScheduler syncScheduler;
    private final NewItemNotificationService notificationService;
    private final HttpUtilService httpUtilService;
    private final KamcoItemFacadeService kamcoItemFacadeService;
    private final com.api.admin.service.AdminService adminService;

    // =============================================================================
    // 화면 페이지
    // =============================================================================
    
    /**
     * 물건 목록 페이지
     */
    @GetMapping("/list")
    public String listPage(
            @RequestParam(value = "sido", required = false) String sido,
            Model model) {
        
        List<KamcoItem> items;
        
        if (sido != null && !sido.isEmpty()) {
            items = kamcoItemService.getBySido(sido);
        } else {
            items = kamcoItemService.getAllItems();
        }
        
        model.addAttribute("items", items);
        model.addAttribute("selectedSido", sido);
        
        return "kamco-items/list";
    }
    
    /**
     * 물건 상세 페이지
     */
    @GetMapping("/detail/{id}")
    public String detailPage(
            @PathVariable("id") Long id,
            HttpServletRequest request,
            Model model) {
        
        KamcoItem item = kamcoItemService.getById(id);
        
        if (item == null) {
            return "redirect:/kamco-items/list";
        }
        
        // 조회수 증가
        String memberId = httpUtilService.getMemberId(request);
        String ipAddress = httpUtilService.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        kamcoItemService.incrementViewCount(id, memberId, ipAddress, userAgent);
        
        // 50% 체감 물건 조회
        List<KamcoItem> discountItems = kamcoItemService.get50PercentDiscountItems(20);
        
        model.addAttribute("item", item);
        model.addAttribute("discountItems", discountItems);
        
        return "kamco-items/detail";
    }
    
    /**
     * 메인 페이지용 데이터
     */
    @GetMapping("/main-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMainData() {
        return kamcoItemFacadeService.buildMainDataResponse().toResponseEntity();
    }

    // =============================================================================
    // REST API
    // =============================================================================
    
    /**
     * 전체 물건 조회
     */
    @GetMapping("/api/all")
    @ResponseBody
    public ResponseEntity<List<KamcoItemResponse>> getAllItems() {
        List<KamcoItem> items = kamcoItemService.getAllItems();
        List<KamcoItemResponse> response = items.stream()
                .map(KamcoItemResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 신규 물건 조회
     */
    @GetMapping("/api/new")
    @ResponseBody
    public ResponseEntity<List<KamcoItemResponse>> getNewItems(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        List<KamcoItem> items = kamcoItemService.getNewItems(limit);
        List<KamcoItemResponse> response = items.stream()
                .map(KamcoItemResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 당일 매각 예정 물건 조회
     */
    @GetMapping("/api/today-closing")
    @ResponseBody
    public ResponseEntity<List<KamcoItemResponse>> getTodayClosingItems() {
        List<KamcoItem> items = kamcoItemService.getTodayClosingItems();
        List<KamcoItemResponse> response = items.stream()
                .map(KamcoItemResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 50% 체감 물건 조회
     */
    @GetMapping("/api/discount-50")
    @ResponseBody
    public ResponseEntity<List<KamcoItemResponse>> get50PercentDiscountItems(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        List<KamcoItem> items = kamcoItemService.get50PercentDiscountItems(limit);
        List<KamcoItemResponse> response = items.stream()
                .map(KamcoItemResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    
    /**
     * 시도별 물건 조회
     */
    @GetMapping("/api/sido/{sido}")
    @ResponseBody
    public ResponseEntity<List<KamcoItemResponse>> getBySido(@PathVariable String sido) {
        List<KamcoItem> items = kamcoItemService.getBySido(sido);
        List<KamcoItemResponse> response = items.stream()
                .map(KamcoItemResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 물건 검색
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<List<KamcoItemResponse>> search(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        List<KamcoItem> items = kamcoItemService.search(keyword, limit);
        List<KamcoItemResponse> response = items.stream()
                .map(KamcoItemResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 물건 상세 조회
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<KamcoItemResponse> getItem(@PathVariable("id") Long id) {
        KamcoItem item = kamcoItemService.getById(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(KamcoItemResponse.from(item));
    }

    // =============================================================================
    // 관리자 기능
    // =============================================================================
    
    /**
     * 수동 동기화 (관리자 전용)
     */
    @GetMapping("/api/admin/sync")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> manualSync() {
        return kamcoItemFacadeService.handleManualSync().toResponseEntity();
    }
    
    /**
     * 서울특별시가 아닌 데이터 삭제
     */
    @GetMapping("/api/admin/clear-non-seoul")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearNonSeoulData() {
        return kamcoItemFacadeService.clearNonSeoulDataResponse().toResponseEntity();
    }
    
    /**
     * 서울특별시 데이터 즉시 동기화
     */
    @GetMapping("/api/admin/sync-seoul-now")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> syncSeoulNow() {
        Map<String, Object> result = adminService.syncSeoulNow();
        return ResponseEntity.ok(result);
    }

}

