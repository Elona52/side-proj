package com.api.admin.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.admin.service.AdminService;

import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

/**
 * 어드민 REST API 컨트롤러
 * UI 없이 API 엔드포인트로만 관리 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    @Autowired
    private AdminService adminService;
    
    // =============================================================================
    // 관리 페이지 (View)
    // =============================================================================
    
    /**
     * 관리 페이지 화면
     * GET /api/admin/panel
     */
    @GetMapping("/panel")
    public org.springframework.web.servlet.ModelAndView adminPanel() {
        log.info("🌐 [URL 호출] GET /api/admin/panel");
        return new org.springframework.web.servlet.ModelAndView("admin/admin-panel");
    }
    
    /**
     * 헬스 체크 엔드포인트 (404 오류 확인용)
     * GET /api/admin/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "AdminRestController is working");
        response.put("timestamp", System.currentTimeMillis());
        log.info("✅ Health check: AdminRestController is accessible");
        return ResponseEntity.ok(response);
    }

    // =============================================================================
    // 1. API에서 바로 데이터 가져와 단건별 보기
    // =============================================================================
    
    /**
     * API에서 물건번호로 단건 조회 (DB 저장 안 함)
     * GET /api/admin/fetch-single/{cltrNo}
     */
    @GetMapping("/fetch-single/{cltrNo}")
    public ResponseEntity<Map<String, Object>> fetchSingleFromApi(@PathVariable String cltrNo) {
        log.info("🌐 [URL 호출] GET /api/admin/fetch-single/{}", cltrNo);
        
        Map<String, Object> response = adminService.fetchSingleFromApi(cltrNo);
        
        if (!(Boolean) response.getOrDefault("success", false)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    // =============================================================================
    // 2. API에서 데이터 가져와서 이력별 보기
    // =============================================================================
    
    /**
     * API에서 물건 이력 조회 (같은 물건의 여러 입찰 회차)
     * GET /api/admin/fetch-history/{cltrNo}
     */
    @GetMapping("/fetch-history/{cltrNo}")
    public ResponseEntity<Map<String, Object>> fetchHistoryFromApi(@PathVariable String cltrNo) {
        log.info("📡 API에서 이력 조회: cltrNo={}", cltrNo);
        
        Map<String, Object> response = adminService.fetchHistoryFromApi(cltrNo);
        
        if (!(Boolean) response.getOrDefault("success", false)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    // =============================================================================
    // 3. API 데이터를 DB에 저장
    // =============================================================================
    
    /**
     * API에서 가져온 데이터를 DB에 저장
     * POST /api/admin/save
     * Body: { "cltrNo": "1946427" }
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveToDatabase(@RequestBody Map<String, String> request) {
        String cltrNo = request.get("cltrNo");
        log.info("🌐 [URL 호출] POST /api/admin/save, cltrNo: {}", cltrNo);
        
        Map<String, Object> response = adminService.saveToDatabase(cltrNo);
        
        if (!(Boolean) response.getOrDefault("success", false)) {
            HttpStatus status = response.containsKey("errorType") 
                ? HttpStatus.INTERNAL_SERVER_ERROR 
                : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * API에서 여러 물건 일괄 저장 (전체 저장)
     * POST /api/admin/save-batch
     * Body: { "sido": "서울특별시", "type": "all" } - 전체 저장
     * Body: { "sido": "서울특별시", "page": 1, "numOfRows": 10 } - 일부 저장
     */
    @PostMapping("/save-batch")
    public ResponseEntity<Map<String, Object>> saveBatchToDatabase(@RequestBody Map<String, Object> request) {
        String sido = (String) request.getOrDefault("sido", "서울특별시");
        String type = (String) request.getOrDefault("type", "");
        int page = request.containsKey("page") ? (int) request.get("page") : 1;
        int numOfRows = request.containsKey("numOfRows") ? (int) request.get("numOfRows") : 10;
        
        log.info("💾 DB 일괄 저장 요청: sido={}, type={}, page={}, numOfRows={}", sido, type, page, numOfRows);
        
        Map<String, Object> response = adminService.saveBatchToDatabase(sido, type, page, numOfRows);
        
        if (!(Boolean) response.getOrDefault("success", false)) {
            HttpStatus status = response.containsKey("errorType") 
                ? HttpStatus.INTERNAL_SERVER_ERROR 
                : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 신규 물건 API 목록 조회 (페이징)
     * GET /api/admin/api/new-items?page=1&size=100
     */
    @GetMapping("/api/new-items")
    public ResponseEntity<Map<String, Object>> getNewItemsFromApi(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "100") int size,
            @RequestParam(name = "sido", defaultValue = "서울특별시") String sido) {
        
        log.info("📡 신규 물건 API 조회: sido={}, page={}, size={}", sido, page, size);
        
        Map<String, Object> response = adminService.getNewItemsFromApi(sido, page, size);
        
        if (!(Boolean) response.getOrDefault("success", false)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 신규 물건 전체 저장
     * POST /api/admin/save-new-items-batch
     * Body: { "sido": "서울특별시" }
     */
    @PostMapping("/save-new-items-batch")
    public ResponseEntity<Map<String, Object>> saveNewItemsBatchToDatabase(@RequestBody Map<String, Object> request) {
        String sido = (String) request.getOrDefault("sido", "서울특별시");
        log.info("💾 신규 물건 전체 저장 요청: sido={}", sido);
        
        Map<String, Object> response = adminService.saveNewItemsBatchToDatabase(sido);
        
        if (!(Boolean) response.getOrDefault("success", false)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    // =============================================================================
    // 4. 저장된 데이터 단건별 확인
    // =============================================================================
    
    /**
     * DB에서 ID로 단건 조회
     * GET /api/admin/db/item/{id}
     */
    @GetMapping("/db/item/{id}")
    public ResponseEntity<Map<String, Object>> getItemFromDb(@PathVariable("id") Long id) {
        log.info("🔍 DB에서 단건 조회: id={}", id);
        
        return adminService.getItemFromDbResponse(id).toResponseEntity();
    }
    
    /**
     * DB에서 물건번호로 단건 조회
     * GET /api/admin/db/item-by-cltr/{cltrNo}
     */
    @GetMapping("/db/item-by-cltr/{cltrNo}")
    public ResponseEntity<Map<String, Object>> getItemByCltrNoFromDb(@PathVariable String cltrNo) {
        log.info("🔍 DB에서 물건번호로 조회: cltrNo={}", cltrNo);
        
        return adminService.getItemByCltrNoFromDbResponse(cltrNo).toResponseEntity();
    }

    // =============================================================================
    // 5. 저장된 데이터 이력별 확인
    // =============================================================================
    
    /**
     * DB에서 물건 이력 조회 (같은 물건의 여러 입찰 회차)
     * GET /api/admin/db/history/{cltrNo}
     */
    @GetMapping("/db/history/{cltrNo}")
    public ResponseEntity<Map<String, Object>> getHistoryFromDb(@PathVariable String cltrNo) {
        log.info("🔍 DB에서 이력 조회: cltrNo={}", cltrNo);
        
        return adminService.getHistoryFromDbResponse(cltrNo).toResponseEntity();
    }
    
    /**
     * DB에서 서울특별시 물건 목록 조회 (페이징)
     * GET /api/admin/db/items-seoul?page=1&size=50
     */
    @GetMapping("/db/items-seoul")
    public ResponseEntity<Map<String, Object>> getSeoulItemsFromDb(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        
        log.info("========================================");
        log.info("🌐 [URL 호출] GET /api/admin/db/items-seoul?page={}&size={}", page, size);
        log.info("🔍 [DB 조회 시작] 서울특별시 목록 조회");
        
        return adminService.getSeoulItemsFromDbResponse(page, size).toResponseEntity();
    }
    
    /**
     * DB에서 전체 물건 목록 조회 (페이징 없이 전체)
     * GET /api/admin/db/items-all
     */
    @GetMapping("/db/items-all")
    public ResponseEntity<Map<String, Object>> getAllItemsWithoutPaging() {
        
        log.info("========================================");
        log.info("🌐 [URL 호출] GET /api/admin/db/items-all");
        log.info("🔍 [DB 조회 시작] 전체 내역 조회 (페이징 없음)");
        
        return adminService.getAllItemsWithoutPagingResponse().toResponseEntity();
    }
    
    /**
     * DB에서 전체 물건 목록 조회 (페이징)
     * GET /api/admin/db/items?page=1&size=20
     */
    @GetMapping("/db/items")
    public ResponseEntity<Map<String, Object>> getAllItemsFromDb(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        
        log.info("========================================");
        log.info("🌐 [URL 호출] GET /api/admin/db/items?page={}&size={}", page, size);
        log.info("🔍 [DB 조회 시작] 전체 목록 조회");
        
        return adminService.getAllItemsWithPagingResponse(page, size).toResponseEntity();
    }

    // =============================================================================
    // 6. 실제 데이터 삭제
    // =============================================================================
    
    /**
     * DB에서 물건 삭제 (ID로)
     * DELETE /api/admin/db/item/{id}
     */
    @DeleteMapping("/db/item/{id}")
    public ResponseEntity<Map<String, Object>> deleteItemById(@PathVariable("id") Long id) {
        log.info("========================================");
        log.info("🌐 [URL 호출] DELETE /api/admin/db/item/{}", id);
        log.info("🗑️ [DB 삭제 시작] ID: {}", id);
        
        return adminService.deleteItemByIdResponse(id).toResponseEntity();
    }
    
    /**
     * DB에서 물건 삭제 (물건번호로)
     * DELETE /api/admin/db/item-by-cltr/{cltrNo}
     */
    @DeleteMapping("/db/item-by-cltr/{cltrNo}")
    public ResponseEntity<Map<String, Object>> deleteItemByCltrNo(@PathVariable String cltrNo) {
        log.info("🗑️ DB에서 물건번호로 삭제 요청: cltrNo={}", cltrNo);
        
        return adminService.deleteItemByCltrNoResponse(cltrNo).toResponseEntity();
    }
    
    /**
     * DB에서 여러 물건 일괄 삭제
     * DELETE /api/admin/db/items
     * Body: { "ids": [1, 2, 3] }
     */
    @DeleteMapping("/db/items")
    public ResponseEntity<Map<String, Object>> deleteBatchItems(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        log.info("🗑️ DB에서 일괄 삭제 요청: {}개", ids.size());
        
        return adminService.deleteBatchItemsResponse(ids).toResponseEntity();
    }
    
    /**
     * DB에서 서울특별시 외 지역 삭제
     * DELETE /api/admin/db/delete-non-seoul
     */
    @DeleteMapping("/db/delete-non-seoul")
    public ResponseEntity<Map<String, Object>> deleteNonSeoulItems() {
        log.info("========================================");
        log.info("🌐 [URL 호출] DELETE /api/admin/db/delete-non-seoul");
        log.info("🗑️ [대량 삭제 시작] 서울특별시 외 지역 삭제");
        
        return adminService.deleteNonSeoulItemsResponse().toResponseEntity();
    }
    
    /**
     * DB에서 전체 데이터 삭제 (위험!)
     * DELETE /api/admin/db/delete-all
     */
    @DeleteMapping("/db/delete-all")
    public ResponseEntity<Map<String, Object>> deleteAllItems() {
        log.info("========================================");
        log.info("🌐 [URL 호출] DELETE /api/admin/db/delete-all");
        log.info("⚠️⚠️⚠️ [전체 삭제 시작] 모든 데이터 삭제 ⚠️⚠️⚠️");
        
        return adminService.deleteAllItemsResponse().toResponseEntity();
    }
    
    // =============================================================================
    // 회원 관리
    // =============================================================================
    
    /**
     * 전체 회원 목록 조회
     * GET /api/members/all
     */
    @GetMapping("/members/all")
    public ResponseEntity<List<com.api.member.dto.MemberResponse>> getAllMembers() {
        log.info("========================================");
        log.info("🌐 [URL 호출] GET /api/admin/members/all");
        log.info("👥 [회원 조회 시작] 전체 회원 목록 조회");
        
        return adminService.getAllMembersResponse().toResponseEntity();
    }

    /**
     * (부트스트랩용) 기본 관리자 계정 생성
     * POST /api/admin/bootstrap/create-admin
     * body 필요 없음. 존재하면 건너뜀.
     *
     * id: admin
     * pass: tkdwkd22==
     * type: ADMIN
     */
    @PostMapping("/bootstrap/create-admin")
    public ResponseEntity<Map<String, Object>> createDefaultAdmin() {
        return adminService.createDefaultAdminResponse().toResponseEntity();
    }
    
    /**
     * 회원 비밀번호 초기화 (관리자용)
     * POST /api/admin/members/reset-password
     * Body: { "memberId": "dlwlals52", "newPassword": "test1234" }
     */
    @PostMapping("/members/reset-password")
    public ResponseEntity<Map<String, Object>> resetMemberPassword(@RequestBody Map<String, String> request) {
        String memberId = request.get("memberId");
        String newPassword = request.get("newPassword");
        
        return adminService.resetMemberPasswordResponse(memberId, newPassword).toResponseEntity();
    }
    
    /**
     * 회원 삭제
     * DELETE /api/admin/members/{memberId}
     */
    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<Map<String, Object>> deleteMember(@PathVariable("memberId") String memberId) {
        log.info("========================================");
        log.info("🌐 [URL 호출] DELETE /api/admin/members/{}", memberId);
        log.info("🗑️ [회원 삭제 시작] 회원 ID: {}", memberId);
        
        return adminService.deleteMemberResponse(memberId).toResponseEntity();
    }
    
    /**
     * 회원 정보 수정 (관리자용)
     * POST /api/admin/members/update
     * Body: { "memberId": "dlwlals52", "name": "...", "mail": "...", "phone": "..." }
     */
    @PostMapping("/members/update")
    public ResponseEntity<Map<String, Object>> updateMember(@RequestBody Map<String, String> request) {
        String memberId = request.get("memberId");
        
        return adminService.updateMemberResponse(request).toResponseEntity();
    }
    
    // =============================================================================
    // 게시판 관리
    // =============================================================================
    
    /**
     * 전체 게시글 목록 조회
     * GET /api/boards/all
     */
    @GetMapping("/boards/all")
    public ResponseEntity<List<com.api.auction.domain.FindBoard>> getAllBoards() {
        log.info("📋 전체 게시글 목록 조회");
        
        return adminService.getAllBoardsResponse().toResponseEntity();
    }
    
    /**
     * 게시글 삭제 (관리자용)
     * DELETE /api/admin/boards/{boardNo}
     */
    @DeleteMapping("/boards/{boardNo}")
    public ResponseEntity<Map<String, Object>> deleteBoardByAdmin(@PathVariable("boardNo") int boardNo) {
        log.info("🗑️ [관리자] 게시글 삭제 요청: no={}", boardNo);
        
        return adminService.deleteBoardResponse(boardNo).toResponseEntity();
    }
}

