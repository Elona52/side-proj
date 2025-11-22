package com.api.item.controller;

import com.api.item.domain.PublicAuctionInfo;
import com.api.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 공매 물건 상세 정보 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/auction-info")
@RequiredArgsConstructor
public class PublicAuctionInfoController {
    
    private final AdminService service;
    
    /**
     * 물건번호로 상세 정보 조회
     * GET /api/auction-info/{cltrNo}
     */
    @GetMapping("/{cltrNo}")
    public ResponseEntity<PublicAuctionInfo> getByCltrNo(@PathVariable String cltrNo) {
        log.info("공매 물건 상세 정보 조회 요청: cltrNo={}", cltrNo);
        
        PublicAuctionInfo info = service.getPublicAuctionInfoByCltrNo(cltrNo);
        
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * 공매번호와 물건번호로 상세 정보 조회
     * GET /api/auction-info/{pbctNo}/{cltrNo}
     */
    @GetMapping("/{pbctNo}/{cltrNo}")
    public ResponseEntity<PublicAuctionInfo> getByPbctNoAndCltrNo(
            @PathVariable String pbctNo,
            @PathVariable String cltrNo) {
        
        log.info("공매 물건 상세 정보 조회 요청: pbctNo={}, cltrNo={}", pbctNo, cltrNo);
        
        PublicAuctionInfo info = service.getPublicAuctionInfoByPbctNoAndCltrNo(pbctNo, cltrNo);
        
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * 전체 목록 조회 (페이징)
     * GET /api/auction-info?page=1&size=10
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        
        log.info("공매 물건 목록 조회 요청: page={}, size={}", page, size);
        
        List<PublicAuctionInfo> list = service.getAllPublicAuctionInfo(page, size);
        int total = service.getPublicAuctionInfoTotalCount();
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", list);
        response.put("currentPage", page);
        response.put("pageSize", size);
        response.put("totalCount", total);
        response.put("totalPages", (int) Math.ceil((double) total / size));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 공매 물건 상세 정보 저장/업데이트
     * POST /api/auction-info
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> save(@RequestBody PublicAuctionInfo info) {
        log.info("공매 물건 상세 정보 저장 요청: pbctNo={}, cltrNo={}", info.getPbctNo(), info.getCltrNo());
        return service.savePublicAuctionInfoResponse(info).toResponseEntity();
    }
    
    /**
     * 여러 건 일괄 저장
     * POST /api/auction-info/batch
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> saveBatch(@RequestBody List<PublicAuctionInfo> infoList) {
        log.info("공매 물건 상세 정보 일괄 저장 요청: {} 건", infoList.size());
        return service.savePublicAuctionInfoBatchResponse(infoList).toResponseEntity();
    }
    
    /**
     * 삭제
     * DELETE /api/auction-info/{pbctNo}/{cltrNo}
     */
    @DeleteMapping("/{pbctNo}/{cltrNo}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String pbctNo,
            @PathVariable String cltrNo) {
        log.info("공매 물건 상세 정보 삭제 요청: pbctNo={}, cltrNo={}", pbctNo, cltrNo);
        return service.deletePublicAuctionInfoResponse(pbctNo, cltrNo).toResponseEntity();
    }
}

