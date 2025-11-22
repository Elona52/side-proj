package com.api.favorite.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.favorite.domain.Favorite;
import com.api.item.domain.Item;
import com.api.item.domain.KamcoItem;
import com.api.member.domain.Member;
import com.api.favorite.domain.PriceAlert;
import com.api.common.dto.ServiceResponse;
import com.api.favorite.mapper.FavoriteMapper;
import com.api.member.mapper.MemberMapper;
import com.api.item.service.OnbidApiService;
import com.api.item.service.KamcoItemService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final MemberMapper memberMapper;
    private final OnbidApiService onbidApiService;
    private final JavaMailSender mailSender;
    private final KamcoItemService kamcoItemService;

    private Long extractItemId(Map<String, Object> requestBody) {
        log.info("========================================");
        log.info("=== extractItemId 메서드 호출 ===");
        log.info("========================================");
        if (requestBody == null) {
            log.error("❌ extractItemId: requestBody가 null");
            return null;
        }

        log.info("extractItemId: requestBody keys={}", requestBody.keySet());
        log.info("extractItemId: requestBody 전체={}", requestBody);
        log.info("extractItemId: itemId 값={}, 타입={}", 
            requestBody.get("itemId"), 
            requestBody.get("itemId") != null ? requestBody.get("itemId").getClass().getName() : "null");
        log.info("extractItemId: cltrNo 값={}, 타입={}", 
            requestBody.get("cltrNo"),
            requestBody.get("cltrNo") != null ? requestBody.get("cltrNo").getClass().getName() : "null");

        // itemId가 있으면 직접 사용
        Object itemIdObj = requestBody.get("itemId");
        if (itemIdObj != null) {
            try {
                Long itemId;
                if (itemIdObj instanceof Number) {
                    itemId = ((Number) itemIdObj).longValue();
                } else {
                    itemId = Long.parseLong(itemIdObj.toString());
                }
                log.info("✅ extractItemId: itemId 직접 사용={}", itemId);
                return itemId;
            } catch (NumberFormatException e) {
                log.error("❌ extractItemId: itemId 파싱 실패 - value={}, error={}", 
                    itemIdObj, e.getMessage());
            }
        }

        // itemPlnmNo (공고번호)로 조회
        if (requestBody.get("itemPlnmNo") != null) {
            String plnmNo = requestBody.get("itemPlnmNo").toString();
            log.info("extractItemId: itemPlnmNo로 조회={}", plnmNo);
            Long itemId = getItemIdByPlnmNo(plnmNo);
            log.info("extractItemId: itemPlnmNo로 조회 결과 itemId={}", itemId);
            return itemId;
        }

        // cltrNo (물건번호)로 조회
        Object cltrNoObj = requestBody.get("cltrNo");
        if (cltrNoObj != null) {
            String cltrNo = cltrNoObj.toString().trim();
            log.info("extractItemId: cltrNo로 조회={}", cltrNo);
            if (!cltrNo.isEmpty() && !cltrNo.equals("null")) {
                Long itemId = getItemIdByCltrNo(cltrNo);
                log.info("extractItemId: cltrNo로 조회 결과 itemId={}", itemId);
                if (itemId == null) {
                    log.error("❌ extractItemId: cltrNo로 itemId를 찾을 수 없음 - cltrNo={}", cltrNo);
                } else {
                    log.info("✅ extractItemId: cltrNo로 itemId 찾기 성공 - cltrNo={}, itemId={}", cltrNo, itemId);
                }
                return itemId;
            } else {
                log.warn("⚠️ extractItemId: cltrNo가 빈 문자열이거나 'null' 문자열임");
            }
        }

        log.error("❌ extractItemId: itemId, itemPlnmNo, cltrNo 모두 없거나 유효하지 않음 - requestBody={}", requestBody);
        log.info("========================================");
        return null;
    }

    /**
     * 즐겨찾기 추가 요청 처리
     */
    public ServiceResponse<Map<String, Object>> handleAddFavoriteRequest(
            String userId,
            Map<String, Object> requestBody) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== 즐겨찾기 추가 요청 ===");
            log.info("userId: {}, requestBody: {}", userId, requestBody);
            
            if (userId == null || userId.isEmpty()) {
                log.warn("로그인 필요: userId가 null이거나 비어있음");
                response.put("success", false);
                response.put("message", "로그인이 필요한 서비스입니다.");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
            }

            Long itemId = extractItemId(requestBody);
            log.info("extractItemId 결과: itemId={}", itemId);
            
            if (itemId == null) {
                log.warn("itemId 추출 실패: requestBody={}", requestBody);
                response.put("success", false);
                response.put("message", "itemId 또는 cltrNo가 필요합니다.");
                return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
            }

            Favorite favorite = addFavorite(userId, itemId);
            log.info("즐겨찾기 추가 성공: favoriteId={}, userId={}, itemId={}", 
                favorite.getFavoriteId(), userId, itemId);
            
            response.put("success", true);
            response.put("message", "즐겨찾기에 추가되었습니다.");
            response.put("favorite", favorite);
            return ServiceResponse.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
        } catch (Exception e) {
            log.error("즐겨찾기 추가 중 오류", e);
            response.put("success", false);
            response.put("message", "즐겨찾기 추가 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 즐겨찾기 삭제 요청 처리
     */
    public ServiceResponse<Map<String, Object>> handleRemoveFavoriteRequest(String userId, Long favoriteId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("즐겨찾기 삭제 요청 처리: userId={}, favoriteId={}", userId, favoriteId);
            
            if (favoriteId == null) {
                response.put("success", false);
                response.put("message", "favoriteId가 필요합니다.");
                return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
            }
            
            // 로그인 체크
            if (userId == null || userId.isEmpty()) {
                response.put("success", false);
                response.put("message", "로그인이 필요한 서비스입니다.");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
            }

            // 즐겨찾기 정보 조회하여 권한 확인
            Favorite favorite = favoriteMapper.getFavoriteById(favoriteId);
            if (favorite == null) {
                response.put("success", false);
                response.put("message", "즐겨찾기를 찾을 수 없습니다.");
                return ServiceResponse.of(HttpStatus.NOT_FOUND, response);
            }
            
            // 자신의 즐겨찾기만 삭제 가능
            if (!userId.equals(favorite.getUserId())) {
                log.warn("권한 없음: userId={}, favorite.userId={}", userId, favorite.getUserId());
                response.put("success", false);
                response.put("message", "삭제 권한이 없습니다.");
                return ServiceResponse.of(HttpStatus.FORBIDDEN, response);
            }

            removeFavorite(favoriteId);
            response.put("success", true);
            response.put("message", "즐겨찾기가 삭제되었습니다.");
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("즐겨찾기 삭제 중 오류: userId={}, favoriteId={}", userId, favoriteId, e);
            response.put("success", false);
            response.put("message", "즐겨찾기 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 즐겨찾기 목록 응답 생성
     */
    public ServiceResponse<Map<String, Object>> handleFavoritesResponse(String userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (userId == null || userId.isEmpty()) {
                response.put("success", false);
                response.put("message", "로그인이 필요한 서비스입니다.");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
            }

            List<Favorite> favorites = getFavoritesByMemberId(userId);
            response.put("success", true);
            response.put("favorites", favorites != null ? favorites : List.of());
            response.put("count", favorites != null ? favorites.size() : 0);
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("즐겨찾기 목록 조회 중 오류", e);
            response.put("success", false);
            response.put("message", "즐겨찾기 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 즐겨찾기 여부 확인 응답 생성
     */
    public ServiceResponse<Map<String, Object>> handleFavoriteCheck(String userId, Long itemId, String cltrNo, String itemPlnmNo) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (userId == null || userId.isEmpty()) {
                response.put("success", true);
                response.put("isFavorite", false);
                return ServiceResponse.ok(response);
            }

            boolean isFavorite = false;
            if (itemId != null) {
                isFavorite = isFavorite(userId, itemId);
            } else if (itemPlnmNo != null && !itemPlnmNo.isEmpty()) {
                isFavorite = isFavoriteByPlnmNo(userId, itemPlnmNo);
            } else if (cltrNo != null && !cltrNo.isEmpty()) {
                isFavorite = isFavoriteByCltrNo(userId, cltrNo);
            } else {
                response.put("success", false);
                response.put("message", "itemId, itemPlnmNo 또는 cltrNo가 필요합니다.");
                return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
            }

            response.put("success", true);
            response.put("isFavorite", isFavorite);
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("즐겨찾기 확인 중 오류", e);
            response.put("success", false);
            response.put("message", "즐겨찾기 확인 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 가격 알림 히스토리 응답 생성
     */
    public ServiceResponse<Map<String, Object>> handlePriceAlertsResponse(String userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (userId == null || userId.isEmpty()) {
                response.put("success", false);
                response.put("message", "로그인이 필요한 서비스입니다.");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
            }

            List<PriceAlert> alerts = getPriceAlertsByMemberId(userId);
            response.put("success", true);
            response.put("alerts", alerts);
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("가격 알림 히스토리 조회 중 오류", e);
            response.put("success", false);
            response.put("message", "알림 히스토리 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 즐겨찾기 추가 (새 구조: itemId 사용)
     * @param userId 회원 ID
     * @param itemId 물건 ID (KNKamcoItem.id)
     * @return 추가된 Favorite 객체
     */
    @Transactional
    public Favorite addFavorite(String userId, Long itemId) {
        log.info("=== addFavorite 메서드 호출 ===");
        log.info("입력 파라미터: userId={}, itemId={}", userId, itemId);
        
        // 입력값 검증
        if (userId == null || userId.trim().isEmpty()) {
            log.error("회원 ID가 null이거나 비어있음");
            throw new IllegalArgumentException("회원 ID가 필요합니다.");
        }
        if (itemId == null) {
            log.error("물건 ID가 null");
            throw new IllegalArgumentException("물건 ID가 필요합니다.");
        }
        
        log.info("입력값 검증 통과");
        
        // 이미 즐겨찾기에 있는지 확인
        log.info("기존 즐겨찾기 확인 중: userId={}, itemId={}", userId, itemId);
        Favorite existing = favoriteMapper.getFavoriteByMemberAndItem(userId, itemId);
        if (existing != null) {
            log.info("이미 즐겨찾기에 존재: favoriteId={}, userId={}, itemId={}", 
                existing.getFavoriteId(), userId, itemId);
            return existing; // 이미 존재하면 기존 객체 반환
        }
        log.info("기존 즐겨찾기 없음 - 새로 추가 진행");
        
        // 물건 정보 조회 (관심수 업데이트용)
        log.info("물건 정보 조회 중: itemId={}", itemId);
        KamcoItem item = kamcoItemService.getById(itemId);
        if (item == null) {
            log.warn("물건 정보를 찾을 수 없음: itemId={}", itemId);
        } else {
            log.info("물건 정보 조회 성공: cltrNo={}, cltrNm={}", item.getCltrNo(), item.getCltrNm());
        }
        
        // 새로운 즐겨찾기 추가
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setItemId(itemId);
        log.info("Favorite 객체 생성: userId={}, itemId={}", userId, itemId);
        
        try {
            log.info("데이터베이스에 즐겨찾기 INSERT 시도");
            log.info("INSERT할 데이터: userId={}, itemId={}", userId, itemId);
            
            // 외래키 제약조건 확인을 위한 사전 검증
            // KNMember 확인
            Member member = memberMapper.getMemberInfo(userId);
            if (member == null) {
                log.error("회원을 찾을 수 없음: userId={}", userId);
                throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다: " + userId);
            }
            log.info("회원 확인 성공: userId={}, name={}", userId, member.getName());
            
            // KNKamcoItem 확인
            if (item == null) {
                log.error("물건 정보를 찾을 수 없음: itemId={}", itemId);
                throw new IllegalArgumentException("물건 정보를 찾을 수 없습니다: itemId=" + itemId);
            }
            log.info("물건 확인 성공: itemId={}, cltrNo={}", itemId, item.getCltrNo());
            
            favoriteMapper.insertFavorite(favorite);
            log.info("즐겨찾기 INSERT 실행 완료: favoriteId={}, userId={}, itemId={}", 
                favorite.getFavoriteId(), userId, itemId);
            
            // INSERT 후 즉시 조회하여 확인
            Favorite insertedFavorite = favoriteMapper.getFavoriteByMemberAndItem(userId, itemId);
            if (insertedFavorite != null) {
                log.info("✅ 즐겨찾기 추가 성공 확인: favoriteId={}, userId={}, itemId={}", 
                    insertedFavorite.getFavoriteId(), userId, itemId);
            } else {
                log.error("❌ 즐겨찾기 추가 후 조회 실패: userId={}, itemId={}", userId, itemId);
                throw new RuntimeException("즐겨찾기 추가 후 조회에 실패했습니다. 데이터베이스에 저장되지 않았을 수 있습니다.");
            }
            
            // 관심수 증가
            if (item != null && item.getCltrNo() != null) {
                log.info("관심수 증가 시도: cltrNo={}", item.getCltrNo());
                kamcoItemService.incrementInterestCount(item.getCltrNo());
                log.info("관심수 증가 완료: cltrNo={}", item.getCltrNo());
            }
        } catch (Exception e) {
            log.error("즐겨찾기 추가 실패: userId={}, itemId={}, error={}", 
                userId, itemId, e.getMessage(), e);
            log.error("예외 스택 트레이스:", e);
            throw new RuntimeException("즐겨찾기 추가에 실패했습니다: " + e.getMessage(), e);
        }
        
        return favorite;
    }

    /**
     * 즐겨찾기 삭제
     * @param favoriteId 즐겨찾기 ID
     */
    @Transactional
    public void removeFavorite(Long favoriteId) {
        try {
            // 삭제 전에 즐겨찾기 정보 조회 (관심수 업데이트용)
            Favorite favorite = favoriteMapper.getFavoriteById(favoriteId);
            String cltrNo = null;
            if (favorite != null && favorite.getItemId() != null && kamcoItemService != null) {
                KamcoItem item = kamcoItemService.getById(favorite.getItemId());
                if (item != null) {
                    cltrNo = item.getCltrNo();
                }
            }
            
            favoriteMapper.deleteFavorite(favoriteId);
            log.info("즐겨찾기 삭제 성공: favoriteId={}", favoriteId);
            
            // 관심수 감소
            if (cltrNo != null && kamcoItemService != null) {
                kamcoItemService.decrementInterestCount(cltrNo);
                log.debug("관심수 감소: cltrNo={}", cltrNo);
            }
        } catch (Exception e) {
            log.error("즐겨찾기 삭제 실패: favoriteId={}, error={}", favoriteId, e.getMessage(), e);
            throw new RuntimeException("즐겨찾기 삭제에 실패했습니다.", e);
        }
    }

    public List<Favorite> getFavoritesByMemberId(String memberId) {
        log.info("즐겨찾기 목록 조회 시작: memberId={}", memberId);
        
        if (memberId == null || memberId.trim().isEmpty()) {
            log.warn("⚠️ memberId가 null이거나 비어있습니다.");
            return new java.util.ArrayList<>();
        }
        
        try {
            List<Favorite> favorites = favoriteMapper.getFavoritesByMemberId(memberId);
            log.info("✅ 즐겨찾기 목록 조회 완료: memberId={}, count={}", memberId, favorites != null ? favorites.size() : 0);
            
            if (favorites != null && !favorites.isEmpty()) {
                log.info("📋 즐겨찾기 상세 정보:");
                for (int i = 0; i < favorites.size(); i++) {
                    Favorite fav = favorites.get(i);
                    String itemName = fav.getItem() != null ? fav.getItem().getCltrNm() : "null";
                    log.info("  [{}] favoriteId={}, itemId={}, itemName={}", 
                        i + 1, fav.getFavoriteId(), fav.getItemId(), itemName);
                }
            } else {
                log.warn("⚠️ 즐겨찾기 목록이 비어있습니다.");
                log.warn("💡 데이터베이스 확인 쿼리: SELECT * FROM User_Favorite WHERE user_id = '{}'", memberId);
            }
            
            return favorites != null ? favorites : new java.util.ArrayList<>();
        } catch (Exception e) {
            log.error("❌ 즐겨찾기 목록 조회 중 오류: memberId={}, error={}", memberId, e.getMessage(), e);
            throw new RuntimeException("즐겨찾기 목록 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    public boolean isFavorite(String memberId, Long itemId) {
        Favorite favorite = favoriteMapper.getFavoriteByMemberAndItem(memberId, itemId);
        return favorite != null;
    }

    public boolean isFavoriteByPlnmNo(String memberId, String plnmNo) {
        try {
            if (kamcoItemService != null && plnmNo != null && !plnmNo.trim().isEmpty()) {
                KamcoItem item = kamcoItemService.getByPlnmNo(plnmNo.trim());
                if (item != null && item.getId() != null) {
                    return isFavorite(memberId, item.getId());
                }
            }
        } catch (Exception e) {
            log.warn("plnmNo로 즐겨찾기 확인 중 오류: {}", e.getMessage());
        }
        return false;
    }

    public boolean isFavoriteByCltrNo(String memberId, String cltrNo) {
        try {
            if (kamcoItemService != null && cltrNo != null) {
                KamcoItem item = kamcoItemService.getByCltrNo(cltrNo);
                if (item != null && item.getId() != null) {
                    return isFavorite(memberId, item.getId());
                }
            }
        } catch (Exception e) {
            log.warn("cltrNo로 즐겨찾기 확인 중 오류: {}", e.getMessage());
        }
        return false;
    }

    public List<PriceAlert> getPriceAlertsByMemberId(String memberId) {
        return favoriteMapper.getPriceAlertsByMemberId(memberId);
    }

    public Long getItemIdByPlnmNo(String plnmNo) {
        try {
            if (plnmNo != null && !plnmNo.trim().isEmpty()) {
                KamcoItem item = kamcoItemService.getByPlnmNo(plnmNo.trim());
                if (item != null && item.getId() != null) {
                    log.debug("✅ plnmNo로 itemId 조회 성공: plnmNo={}, itemId={}", plnmNo, item.getId());
                    return item.getId();
                } else {
                    log.warn("⚠️ plnmNo로 물건을 찾을 수 없음: plnmNo={}", plnmNo);
                }
            }
        } catch (Exception e) {
            log.error("❌ plnmNo로 itemId 조회 중 오류: plnmNo={}, error={}", plnmNo, e.getMessage(), e);
        }
        return null;
    }
    
    public Long getItemIdByCltrNo(String cltrNo) {
        try {
            log.info("getItemIdByCltrNo: cltrNo={}", cltrNo);
            if (cltrNo != null && !cltrNo.trim().isEmpty()) {
                String trimmedCltrNo = cltrNo.trim();
                log.info("getItemIdByCltrNo: kamcoItemService.getByCltrNo 호출 - cltrNo={}", trimmedCltrNo);
                KamcoItem item = kamcoItemService.getByCltrNo(trimmedCltrNo);
                log.info("getItemIdByCltrNo: 조회 결과 item={}, itemId={}", 
                    item != null ? "존재함" : "null", item != null ? item.getId() : null);
                if (item != null && item.getId() != null) {
                    log.info("getItemIdByCltrNo: 성공 - cltrNo={}, itemId={}", trimmedCltrNo, item.getId());
                    return item.getId();
                } else {
                    log.error("getItemIdByCltrNo: cltrNo로 물건을 찾을 수 없음 - cltrNo={}, item={}", 
                        trimmedCltrNo, item);
                }
            } else {
                log.warn("getItemIdByCltrNo: cltrNo가 null이거나 비어있음 - cltrNo={}", cltrNo);
            }
        } catch (Exception e) {
            log.error("getItemIdByCltrNo: cltrNo로 itemId 조회 중 오류 - cltrNo={}, error={}", 
                cltrNo, e.getMessage(), e);
        }
        return null;
    }
    
    // =============================================================================
    // EmailService 통합 메서드
    // =============================================================================
    
    @Async // 비동기 처리
    public void sendPriceDropAlert(String toEmail, String memberName, Favorite favorite, Long newPrice, Long currentPrice) {
        try {
            String itemName = favorite.getItem() != null && favorite.getItem().getCltrNm() != null 
                ? favorite.getItem().getCltrNm() : "상품";
            String cltrNo = favorite.getItem() != null && favorite.getItem().getCltrNo() != null 
                ? favorite.getItem().getCltrNo() : "";
            
            String subject = "[가격 하락 알림] " + itemName;
            StringBuilder content = new StringBuilder();
            content.append("안녕하세요, ").append(memberName).append("님!\n\n");
            content.append("즐겨찾기하신 상품의 가격이 하락했습니다.\n\n");
            content.append("===========================================\n");
            content.append("상품명: ").append(itemName).append("\n");
            content.append("공고번호: ").append(cltrNo).append("\n");
            content.append("이전 가격: ").append(formatPrice(currentPrice)).append("원\n");
            content.append("현재 가격: ").append(formatPrice(newPrice)).append("원\n");

            if (currentPrice != null && currentPrice > 0) {
                long priceDrop = currentPrice - newPrice;
                double dropRate = (double) priceDrop / currentPrice * 100;
                content.append("하락 금액: ").append(formatPrice(priceDrop)).append("원 (")
                       .append(String.format("%.1f", dropRate)).append("%)\n");
            }

            content.append("===========================================\n\n");
            content.append("자세한 내용은 사이트에서 확인해주세요.\n\n");
            content.append("감사합니다.");

            sendEmailWithRetry(toEmail, subject, content.toString(), 3);

            log.info("가격 하락 알림 이메일 전송 성공: {} -> {}", toEmail, itemName);

        } catch (Exception e) {
            log.error("가격 하락 알림 이메일 전송 실패: {}", e.getMessage(), e);
        }
    }

    public void sendEmail(String toEmail, String subject, String content) {
        sendEmailWithRetry(toEmail, subject, content, 3);
    }

    /**
     * 이메일 재시도 처리
     */
    private void sendEmailWithRetry(String toEmail, String subject, String content, int retryCount) {
        for (int i = 0; i < retryCount; i++) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(content);

                mailSender.send(message);
                log.info("이메일 전송 성공: {}", toEmail);
                return;

            } catch (Exception e) {
                log.error("이메일 전송 실패 ({}회차): {} - {}", i + 1, toEmail, e.getMessage());
            }
        }
        throw new RuntimeException("이메일 전송에 실패했습니다. 최대 재시도 횟수 초과.");
    }

    /**
     * 가격 포맷팅
     */
    private String formatPrice(Long price) {
        if (price == null) return "0";
        return String.format("%,d", price);
    }
    
  
    /**
     * 가격 모니터링 스케줄러
     * 매일 오전 9시와 오후 6시에 실행
     */
    @Scheduled(cron = "0 0 9,18 * * *")
    @Transactional
    public void monitorPrices() {
        log.info("======================================");
        log.info("가격 모니터링 시작");
        log.info("======================================");
        
        try {
            // 알림이 활성화된 모든 즐겨찾기 조회
            List<Favorite> favorites = favoriteMapper.getActiveAlertFavorites();
            log.info("모니터링 대상 즐겨찾기 수: {}", favorites.size());
            
            if (favorites.isEmpty()) {
                log.info("모니터링할 즐겨찾기가 없습니다.");
                return;
            }
            
            // API에서 최신 상품 정보 조회
            List<Item> items = onbidApiService.getUnifyUsageCltr(null, 1, 100);
            log.info("API에서 조회한 상품 수: {}", items.size());
            
            int alertCount = 0;
            
            // 각 즐겨찾기에 대해 가격 확인
            for (Favorite favorite : favorites) {
                try {
                    // 새로운 구조에서는 item 정보가 JOIN으로 포함됨
                    if (favorite.getItem() == null || favorite.getItem().getCltrNo() == null) {
                        log.debug("물건 정보가 없습니다: favoriteId={}", favorite.getFavoriteId());
                        continue;
                    }
                    
                    // 해당 상품 찾기
                    Item matchedItem = findItemByPlnmNo(items, favorite.getItem().getCltrNo());
                    
                    if (matchedItem == null) {
                        log.debug("상품을 찾을 수 없습니다: {}", favorite.getItem().getCltrNo());
                        continue;
                    }
                    
                    // 현재 가격 (최저입찰가 사용)
                    Long newPrice = matchedItem.getMinBidPrc();
                    if (newPrice == null) {
                        log.debug("가격 정보가 없습니다: {}", favorite.getItem().getCltrNo());
                        continue;
                    }
                    
                    // 기존 가격 (물건의 최저입찰가 사용)
                    Long currentPrice = favorite.getItem().getMinBidPrc();
                    
                    // 가격 변동 확인
                    if (currentPrice != null && newPrice < currentPrice) {
                        // 최근 알림 기록 확인
                        PriceAlert lastAlert = favoriteMapper.getLastPriceAlertByFavoriteId(favorite.getFavoriteId());
                        if (lastAlert != null && lastAlert.getNewPrice() != null && lastAlert.getNewPrice().equals(newPrice)) {
                            log.info("이미 같은 가격으로 알림 전송됨: {} -> {}", favorite.getItem().getCltrNm(), newPrice);
                            continue; // 중복 알림 방지
                        }

                        // 알림 전송
                        Member member = memberMapper.getMemberInfo(favorite.getUserId());

                        if (member != null && member.getMail() != null && !member.getMail().isEmpty()) {
                            // 임시 Favorite 객체 생성 (이메일 전송용)
                            Favorite tempFavorite = new Favorite();
                            tempFavorite.setFavoriteId(favorite.getFavoriteId());
                            tempFavorite.setItem(favorite.getItem());
                            
                            sendPriceDropAlert(
                                member.getMail(),
                                member.getName(),
                                tempFavorite,
                                newPrice,
                                currentPrice
                            );

                            // 알림 히스토리 저장
                            PriceAlert alert = new PriceAlert();
                            alert.setFavoriteId(favorite.getFavoriteId());
                            alert.setMemberId(favorite.getUserId());
                            alert.setItemPlnmNo(favorite.getItem().getCltrNo());
                            alert.setPreviousPrice(currentPrice);
                            alert.setNewPrice(newPrice);
                            alert.setAlertSent(true);
                            alert.setSentDate(new Timestamp(System.currentTimeMillis()));

                            favoriteMapper.insertPriceAlert(alert);

                            alertCount++;
                            log.info("알림 전송 완료: {} -> {}", member.getMail(), favorite.getItem().getCltrNm());
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("즐겨찾기 처리 중 오류: favoriteId={} - {}", 
                        favorite.getFavoriteId(), e.getMessage(), e);
                }
            }
            
            log.info("======================================");
            log.info("가격 모니터링 완료 - 전송된 알림 수: {}", alertCount);
            log.info("======================================");
            
        } catch (Exception e) {
            log.error("가격 모니터링 중 오류 발생", e);
        }
    }
    
    /**
     * 공고번호로 상품 찾기
     */
    private Item findItemByPlnmNo(List<Item> items, String plnmNo) {
        if (plnmNo == null) {
            return null;
        }
        
        for (Item item : items) {
            if (plnmNo.equals(item.getPlnmNo())) {
                return item;
            }
        }
        
        return null;
    }
    
    /**
     * 수동으로 가격 모니터링 실행 (테스트용)
     */
    public void monitorPricesManually() {
        log.info("수동 가격 모니터링 실행");
        monitorPrices();
    }
}
