package com.api.payment.service;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.config.IamportConfig;
import com.api.payment.domain.Payment;
import com.api.payment.domain.PaymentHistory;
import com.api.payment.dto.PaymentResponse;
import com.api.auction.domain.Auction;
import com.api.member.domain.Member;
import com.api.common.dto.ServiceResponse;
import com.api.payment.mapper.PaymentMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

	private final PaymentMapper paymentMapper;
	private final com.api.auction.service.AuctionService auctionService;
	private final com.api.member.service.MemberService memberService;
	private final com.api.item.service.KamcoItemService kamcoItemService;
	private final IamportConfig iamportConfig;

	// 결제 정보를 메모리에 임시 저장 (결제 완료 전까지)
	private final Map<String, Payment> paymentStore = new ConcurrentHashMap<>();

	/**
	 * 결제 준비 요청 처리
	 */
	public ServiceResponse<Map<String, Object>> handlePreparePaymentRequest(String memberId,
			Map<String, Object> requestBody) {

		Map<String, Object> response = new HashMap<>();

		try {
			if (memberId == null || memberId.isEmpty()) {
				response.put("success", false);
				response.put("message", "로그인이 필요합니다.");
				return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
			}

			if (requestBody == null || !requestBody.containsKey("amount")) {
				response.put("success", false);
				response.put("message", "amount가 필요합니다.");
				return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
			}

			Integer auctionNo = requestBody.get("auctionNo") != null
					? Integer.parseInt(requestBody.get("auctionNo").toString())
					: null;
			Long itemId = requestBody.get("itemId") != null ? Long.parseLong(requestBody.get("itemId").toString())
					: null;
			String cltrNo = requestBody.get("cltrNo") != null ? requestBody.get("cltrNo").toString() : null;
			Long amount = Long.parseLong(requestBody.get("amount").toString());
			String itemName = requestBody.get("itemName") != null ? requestBody.get("itemName").toString() : "상품";

			Payment payment = preparePayment(auctionNo, itemId, cltrNo, memberId, amount, itemName);
			response.put("success", true);
			response.put("merchantUid", payment.getMerchantUid());
			response.put("amount", payment.getAmount());
			return ServiceResponse.ok(response);

		} catch (IllegalArgumentException e) {
			response.put("success", false);
			response.put("message", e.getMessage());
			return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
		} catch (RuntimeException e) {
			response.put("success", false);
			response.put("message", e.getMessage());
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
		} catch (Exception e) {
			log.error("결제 준비 중 예상치 못한 오류", e);
			response.put("success", false);
			response.put("message", "결제 준비 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
		}
	}

	/**
	 * 결제 완료 처리 요청
	 */
	public ServiceResponse<Map<String, Object>> handleCompletePaymentRequest(String impUid, String merchantUid) {
		Map<String, Object> response = new HashMap<>();

		try {
			if (merchantUid == null || merchantUid.isEmpty()) {
				response.put("success", false);
				response.put("message", "merchant_uid가 필요합니다.");
				return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
			}

			Map<String, Object> result = completePayment(impUid, merchantUid);

			if (Boolean.TRUE.equals(result.get("success"))) {
				Payment payment = (Payment) result.get("payment");
				if (payment != null) {
					Auction auction = auctionService.getAuction(payment.getAuctionNo());
					if (auction != null) {
						auction.setDepositStatus(true);
						auction.setDepositDate(new java.sql.Date(System.currentTimeMillis()));
						auctionService.updateAuction(auction);
					}
				}
			}

			return ServiceResponse.ok(result);

		} catch (Exception e) {
			log.error("결제 완료 처리 중 오류", e);
			response.put("success", false);
			response.put("message", "결제 완료 처리 중 오류가 발생했습니다.");
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
		}
	}

	/**
	 * 결제 취소 요청 처리
	 */
	public ServiceResponse<Map<String, Object>> handleCancelPaymentRequest(String memberId, Long paymentId,
			String reason) {

		Map<String, Object> response = new HashMap<>();

		try {
			if (memberId == null || memberId.isEmpty()) {
				response.put("success", false);
				response.put("message", "로그인이 필요합니다.");
				return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
			}

			if (paymentId == null) {
				response.put("success", false);
				response.put("message", "paymentId가 필요합니다.");
				return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
			}

			Payment payment = getPayment(paymentId);
			if (payment == null) {
				response.put("success", false);
				response.put("message", "결제 정보를 찾을 수 없습니다.");
				return ServiceResponse.of(HttpStatus.NOT_FOUND, response);
			}

			if (!memberId.equals(payment.getMemberId())) {
				response.put("success", false);
				response.put("message", "권한이 없습니다.");
				return ServiceResponse.of(HttpStatus.FORBIDDEN, response);
			}

			String cancelReason = (reason == null || reason.trim().isEmpty()) ? "구매자 요청" : reason;
			boolean success = cancelPayment(paymentId, cancelReason);

			if (success) {
				Auction auction = auctionService.getAuction(payment.getAuctionNo());
				if (auction != null) {
					auction.setDepositStatus(false);
					auctionService.updateAuction(auction);
				}
				response.put("success", true);
				response.put("message", "결제가 취소되었습니다.");
			} else {
				response.put("success", false);
				response.put("message", "결제 취소에 실패했습니다.");
			}

			return ServiceResponse.ok(response);

		} catch (Exception e) {
			log.error("결제 취소 중 오류", e);
			response.put("success", false);
			response.put("message", "결제 취소 중 오류가 발생했습니다.");
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
		}
	}

	@Transactional
	public Payment preparePayment(Integer auctionNo, Long itemId, String cltrNo, String memberId, Long amount,
			String itemName) {
		// 입력값 검증
		if (memberId == null || memberId.trim().isEmpty()) {
			throw new IllegalArgumentException("회원 ID가 필요합니다.");
		}
		if (auctionNo == null && itemId == null && (cltrNo == null || cltrNo.trim().isEmpty())) {
			throw new IllegalArgumentException("경매 번호 또는 아이템 정보가 필요합니다.");
		}
		if (amount == null || amount <= 0) {
			throw new IllegalArgumentException("결제 금액이 올바르지 않습니다.");
		}

		Payment payment = new Payment();

		// API 아이템인 경우 auctionNo를 NULL로 설정 (외래키 제약조건 방지)
		if (itemId != null || (cltrNo != null && !cltrNo.isEmpty())) {
			payment.setAuctionNo(null); // API 아이템은 KNAuction에 없으므로 NULL

			// itemId와 cltrNo 중 하나만 있어도 다른 하나를 자동으로 채워줌
			Long finalItemId = itemId;
			String finalCltrNo = cltrNo;

			// itemId만 있고 cltrNo가 없는 경우, KamcoItem을 조회하여 cltrNo 채우기
			if (itemId != null && (cltrNo == null || cltrNo.isEmpty())) {
				try {
					com.api.item.domain.KamcoItem kamcoItem = getKamcoItemById(itemId);
					if (kamcoItem != null && kamcoItem.getCltrNo() != null && !kamcoItem.getCltrNo().isEmpty()) {
						finalCltrNo = kamcoItem.getCltrNo();
						log.info("itemId로부터 cltrNo 자동 채움 - itemId={}, cltrNo={}", itemId, finalCltrNo);
					} else {
						log.warn("itemId로부터 cltrNo를 찾을 수 없음: itemId={}", itemId);
						throw new IllegalArgumentException("물건 정보를 찾을 수 없습니다. itemId: " + itemId);
					}
				} catch (IllegalArgumentException e) {
					throw e;
				} catch (Exception e) {
					log.error("itemId로부터 cltrNo 조회 실패: itemId={}, error={}", itemId, e.getMessage(), e);
					throw new RuntimeException("물건 정보 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
				}
			}

			// cltrNo만 있고 itemId가 없는 경우, KamcoItem을 조회하여 itemId 채우기
			if ((itemId == null) && (cltrNo != null && !cltrNo.isEmpty())) {
				try {
					com.api.item.domain.KamcoItem kamcoItem = getKamcoItemByCltrNo(cltrNo);
					if (kamcoItem != null && kamcoItem.getId() != null) {
						finalItemId = kamcoItem.getId();
						log.info("cltrNo로부터 itemId 자동 채움 - cltrNo={}, itemId={}", cltrNo, finalItemId);
					} else {
						log.warn("cltrNo로부터 itemId를 찾을 수 없음: cltrNo={}", cltrNo);
						throw new IllegalArgumentException("물건 정보를 찾을 수 없습니다. cltrNo: " + cltrNo);
					}
				} catch (IllegalArgumentException e) {
					throw e;
				} catch (Exception e) {
					log.error("cltrNo로부터 itemId 조회 실패: cltrNo={}, error={}", cltrNo, e.getMessage(), e);
					throw new RuntimeException("물건 정보 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
				}
			}

			// 최종 검증: itemId와 cltrNo가 모두 null이면 오류
			if (finalItemId == null && (finalCltrNo == null || finalCltrNo.isEmpty())) {
				log.error("API 아이템 정보가 부족함 - itemId={}, cltrNo={}", itemId, cltrNo);
				throw new IllegalArgumentException("물건 정보가 올바르지 않습니다. itemId 또는 cltrNo가 필요합니다.");
			}

			payment.setItemId(finalItemId);
			payment.setCltrNo(finalCltrNo);
			log.info("API 아이템으로 Payment 생성 - auctionNo=NULL, itemId={}, cltrNo={}", finalItemId, finalCltrNo);
		} else {
			payment.setAuctionNo(auctionNo); // DB 경매인 경우에만 설정
			payment.setItemId(null);
			payment.setCltrNo(null);
			log.info("DB 경매로 Payment 생성 - auctionNo={}", auctionNo);
		}

		payment.setMemberId(memberId);
		payment.setAmount(amount);
		payment.setItemName(itemName != null ? itemName : "상품");
		payment.setStatus("ready");
		payment.setCreatedDate(new Timestamp(System.currentTimeMillis()));

		String merchantUid = "merchant_" + System.currentTimeMillis() + "_" + memberId;
		payment.setMerchantUid(merchantUid);

		// Payment 객체의 실제 값 확인 (디버깅)
		log.info("Payment 객체 값 확인 - auctionNo={}, itemId={}, cltrNo={}, memberId={}, amount={}", payment.getAuctionNo(),
				payment.getItemId(), payment.getCltrNo(), payment.getMemberId(), payment.getAmount());

		try {
			// DB에 저장
			paymentMapper.insertPayment(payment);
			log.info("결제 준비 성공: paymentId={}, merchantUid={}, memberId={}, auctionNo={}, itemId={}, cltrNo={}",
					payment.getId(), merchantUid, memberId, payment.getAuctionNo(), payment.getItemId(),
					payment.getCltrNo());
		} catch (Exception e) {
			log.error("결제 준비 실패: memberId={}, auctionNo={}, itemId={}, cltrNo={}, error={}", memberId, auctionNo,
					itemId, cltrNo, e.getMessage(), e);
			// 외래키 제약조건 오류인지 확인
			if (e.getMessage() != null && e.getMessage().contains("foreign key constraint")) {
				throw new RuntimeException("회원 정보 또는 경매 정보를 확인할 수 없습니다.", e);
			}
			throw new RuntimeException("결제 준비에 실패했습니다: " + e.getMessage(), e);
		}

		// 메모리에도 임시 저장 (결제 완료 전까지)
		paymentStore.put(merchantUid, payment);
		return payment;
	}

	@Transactional
	public Map<String, Object> completePayment(String impUid, String merchantUid) {
		Map<String, Object> result = new HashMap<>();

		// 먼저 DB에서 조회 시도
		Payment payment = paymentMapper.selectPaymentByMerchantUid(merchantUid);

		// DB에 없으면 메모리에서 조회
		if (payment == null) {
			payment = paymentStore.get(merchantUid);
		}

		if (payment == null) {
			result.put("success", false);
			result.put("message", "결제 정보가 존재하지 않습니다.");
			return result;
		}

		payment.setImpUid(impUid != null ? impUid : "imp_" + System.currentTimeMillis());
		payment.setStatus("paid");
		payment.setPaidAt(new Timestamp(System.currentTimeMillis()));
		payment.setUpdatedDate(new Timestamp(System.currentTimeMillis()));

		// DB에 업데이트
		paymentMapper.updatePayment(payment);

		// 결제 히스토리 추가
		PaymentHistory history = new PaymentHistory();
		history.setPaymentId(payment.getId());
		history.setStatus("paid");
		history.setAction("결제 완료");
		history.setDescription("결제가 완료되었습니다. imp_uid: " + impUid);
		paymentMapper.insertPaymentHistory(history);

		result.put("success", true);
		result.put("payment", payment);
		return result;
	}

	/**
	 * 결제 정보 조회
	 */
	public Payment getPayment(Long paymentId) {
		// DB에서 조회
		Payment payment = paymentMapper.selectPaymentById(paymentId);
		if (payment != null) {
			return payment;
		}

		// DB에 없으면 메모리에서 조회
		return paymentStore.values().stream().filter(p -> p.getId().equals(paymentId)).findFirst().orElse(null);
	}

	/**
	 * 주문번호로 결제 정보 조회
	 */
	public Payment getPaymentByMerchantUid(String merchantUid) {
		// DB에서 조회
		Payment payment = paymentMapper.selectPaymentByMerchantUid(merchantUid);
		if (payment != null) {
			return payment;
		}

		// DB에 없으면 메모리에서 조회
		return paymentStore.get(merchantUid);
	}

	/**
	 * 경매 번호로 결제 정보 조회
	 */
	public Payment getPaymentByAuctionNo(Integer auctionNo) {
		// DB에서 조회
		Payment payment = paymentMapper.selectPaymentByAuctionNo(auctionNo);
		if (payment != null) {
			return payment;
		}

		// DB에 없으면 메모리에서 조회
		return paymentStore.values().stream().filter(p -> p.getAuctionNo().equals(auctionNo)).findFirst().orElse(null);
	}

	/**
	 * 회원의 결제 내역 조회
	 */
	public List<Payment> getPaymentsByMemberId(String memberId) {
		// DB에서 조회
		return paymentMapper.selectPaymentsByMemberId(memberId);
	}

	/**
	 * 회원과 auction/item에 대한 기존 payment 조회 (중복 체크용)
	 */
	public Payment getExistingPayment(String memberId, Integer auctionNo, Long itemId, String cltrNo) {
		return paymentMapper.selectPaymentByMemberAndItem(memberId, auctionNo, itemId, cltrNo);
	}

	/**
	 * 결제 취소
	 */
	@Transactional
	public boolean cancelPayment(Long paymentId, String reason) {
		Payment payment = getPayment(paymentId);
		if (payment == null)
			return false;

		payment.setStatus("cancelled");
		payment.setCancelReason(reason);
		payment.setCancelledAt(new Timestamp(System.currentTimeMillis()));
		payment.setUpdatedDate(new Timestamp(System.currentTimeMillis()));

		// DB에 업데이트
		paymentMapper.updatePayment(payment);

		// 결제 히스토리 추가
		PaymentHistory history = new PaymentHistory();
		history.setPaymentId(paymentId);
		history.setStatus("cancelled");
		history.setAction("결제 취소");
		history.setDescription("결제가 취소되었습니다. 사유: " + reason);
		paymentMapper.insertPaymentHistory(history);

		return true;
	}

	/**
	 * 입찰내역 삭제
	 */
	@Transactional
	public boolean deletePayment(Long paymentId) {
		try {
			Payment payment = getPayment(paymentId);
			if (payment == null) {
				log.warn("삭제할 Payment를 찾을 수 없음: paymentId={}", paymentId);
				return false;
			}

			// DB에서 삭제
			paymentMapper.deletePayment(paymentId);
			log.info("Payment 삭제 완료: paymentId={}", paymentId);
			return true;
		} catch (Exception e) {
			log.error("Payment 삭제 중 오류 발생: paymentId={}", paymentId, e);
			return false;
		}
	}

	/**
	 * 결제 히스토리 조회
	 */
	public List<PaymentHistory> getPaymentHistory(Long paymentId) {
		return paymentMapper.selectPaymentHistoryByPaymentId(paymentId);
	}

	/**
	 * 아임포트 액세스 토큰 발급
	 */
	public String getIamportToken() {
		// TODO: 실제 아임포트 API 토큰 발급 구현 필요
		throw new UnsupportedOperationException("아임포트 토큰 발급 기능이 구현되지 않았습니다.");
	}

	public Map<String, Object> getIamportPaymentData(String impUid) {
		// TODO: 실제 아임포트 API에서 결제 데이터 조회 구현 필요
		throw new UnsupportedOperationException("아임포트 결제 데이터 조회 기능이 구현되지 않았습니다.");
	}

	/**
	 * 결제 검증 (IamportService 통합)
	 */
	public boolean verifyPayment(String impUid, int amount) throws Exception {
		throw new UnsupportedOperationException("결제 검증 기능이 구현되지 않았습니다. (impCode=" + iamportConfig.getImpCode() + ")");
	}

	/**
	 * 결제 정보 조회 (IamportService 통합)
	 */
	public String getPaymentInfo(String impUid) throws Exception {
		throw new UnsupportedOperationException(
				"결제 정보 조회 기능이 구현되지 않았습니다. (callbackUrl=" + iamportConfig.getCallbackUrl() + ")");
	}

	/**
	 * 결제 페이지 데이터 준비
	 */
	public Map<String, Object> prepareCheckoutPageData(String memberId, Integer auctionNo, Long itemId, String cltrNo) {
		Map<String, Object> data = new HashMap<>();

		// 회원 정보 조회
		Member member = memberService.getMemberInfo(memberId);
		if (member == null) {
			data.put("error", "회원 정보를 찾을 수 없습니다.");
			data.put("pageType", "fail");
			return data;
		}
		data.put("member", member);

		Auction auction = null;

		// 1. auctionNo로 조회 (DB 경매)
		if (auctionNo != null) {
			auction = auctionService.getAuction(auctionNo);

			if (auction == null) {
				data.put("error", "경매 정보를 찾을 수 없습니다.");
				data.put("pageType", "fail");
				return data;
			}

			// 낙찰자 확인
			if (auction.getBuyer() != null && !memberId.equals(auction.getBuyer())) {
				data.put("error", "낙찰자만 결제할 수 있습니다.");
				data.put("pageType", "fail");
				return data;
			}
		}
		// 2. itemId로 조회 (API 아이템)
		else if (itemId != null) {
			com.api.item.domain.KamcoItem kamcoItem = kamcoItemService.getById(itemId);

			if (kamcoItem == null) {
				data.put("error", "물건 정보를 찾을 수 없습니다.");
				data.put("pageType", "fail");
				return data;
			}

			// KamcoItem을 Auction 객체로 변환
			auction = auctionService.convertKamcoItemToAuction(kamcoItem);

			// itemId와 cltrNo를 명시적으로 추가
			data.put("itemId", itemId);
			if (kamcoItem.getCltrNo() != null) {
				data.put("cltrNo", kamcoItem.getCltrNo());
			}
		}
		// 3. cltrNo로 조회 (API 아이템)
		else if (cltrNo != null && !cltrNo.isEmpty()) {
			com.api.item.domain.KamcoItem kamcoItem = kamcoItemService.getByCltrNo(cltrNo);

			if (kamcoItem == null) {
				data.put("error", "물건 정보를 찾을 수 없습니다.");
				data.put("pageType", "fail");
				return data;
			}

			// KamcoItem을 Auction 객체로 변환
			auction = auctionService.convertKamcoItemToAuction(kamcoItem);

			// itemId와 cltrNo를 명시적으로 추가
			if (kamcoItem.getId() != null) {
				data.put("itemId", kamcoItem.getId());
			}
			data.put("cltrNo", cltrNo);
		} else {
			data.put("error", "경매 번호 또는 물건 번호를 제공해주세요.");
			data.put("pageType", "fail");
			return data;
		}

		// 이미 결제했는지 확인 (DB 경매인 경우에만)
		if (auctionNo != null) {
			Payment existingPayment = getPaymentByAuctionNo(auction.getNo());
			if (existingPayment != null && "paid".equals(existingPayment.getStatus())) {
				data.put("error", "이미 결제가 완료된 경매입니다.");
				data.put("pageType", "fail");
				return data;
			}
		}
		// API 아이템인 경우 itemId나 cltrNo로 결제 내역 확인
		else if (itemId != null || cltrNo != null) {
			// API 아이템은 여러 번 입찰 가능하므로 중복 체크 생략
			// 필요시 여기에 추가 로직 구현
		}

		data.put("auction", auction);
		data.put("amount", auction.getEndPrice() > 0 ? auction.getEndPrice() : auction.getStartPrice());
		data.put("pageType", "checkout");

		// API 아이템인 경우 itemId와 cltrNo 전달 (이미 위에서 추가했지만, 다시 확인)
		if (itemId != null && !data.containsKey("itemId")) {
			data.put("itemId", itemId);
		}
		if (cltrNo != null && !cltrNo.isEmpty() && !data.containsKey("cltrNo")) {
			data.put("cltrNo", cltrNo);
		}

		// KamcoItem 데이터도 직접 전달 (이미 추가했는지 확인)
		if (!data.containsKey("kamcoItem")) {
			if (itemId != null) {
				com.api.item.domain.KamcoItem kamcoItem = kamcoItemService.getById(itemId);
				if (kamcoItem != null) {
					data.put("kamcoItem", kamcoItem);
					// itemId와 cltrNo가 없으면 추가
					if (!data.containsKey("itemId") && kamcoItem.getId() != null) {
						data.put("itemId", kamcoItem.getId());
					}
					if (!data.containsKey("cltrNo") && kamcoItem.getCltrNo() != null) {
						data.put("cltrNo", kamcoItem.getCltrNo());
					}
				}
			} else if (cltrNo != null && !cltrNo.isEmpty()) {
				com.api.item.domain.KamcoItem kamcoItem = kamcoItemService.getByCltrNo(cltrNo);
				if (kamcoItem != null) {
					data.put("kamcoItem", kamcoItem);
					// itemId와 cltrNo가 없으면 추가
					if (!data.containsKey("itemId") && kamcoItem.getId() != null) {
						data.put("itemId", kamcoItem.getId());
					}
					if (!data.containsKey("cltrNo") && kamcoItem.getCltrNo() != null) {
						data.put("cltrNo", kamcoItem.getCltrNo());
					}
				}
			} else if (auction != null && auction.getCltrNo() != null) {
				com.api.item.domain.KamcoItem kamcoItem = kamcoItemService.getByCltrNo(auction.getCltrNo());
				if (kamcoItem != null) {
					data.put("kamcoItem", kamcoItem);
					// itemId와 cltrNo가 없으면 추가
					if (!data.containsKey("itemId") && kamcoItem.getId() != null) {
						data.put("itemId", kamcoItem.getId());
					}
					if (!data.containsKey("cltrNo") && kamcoItem.getCltrNo() != null) {
						data.put("cltrNo", kamcoItem.getCltrNo());
					}
				}
			}
		}

		// 회원 정보를 안전하게 전달 (null 체크)
		if (member != null) {
			data.put("buyerName", member.getName() != null ? member.getName() : "");
			data.put("buyerEmail", member.getMail() != null ? member.getMail() : "");
			data.put("buyerPhone", member.getPhone() != null ? member.getPhone() : "");
		} else {
			data.put("buyerName", "");
			data.put("buyerEmail", "");
			data.put("buyerPhone", "");
		}

		return data;
	}

	/**
	 * KamcoItem 조회 (ID로)
	 */
	public com.api.item.domain.KamcoItem getKamcoItemById(Long itemId) {
		if (itemId == null) {
			return null;
		}
		return kamcoItemService.getById(itemId);
	}

	/**
	 * KamcoItem 조회 (cltrNo로)
	 */
	public com.api.item.domain.KamcoItem getKamcoItemByCltrNo(String cltrNo) {
		if (cltrNo == null || cltrNo.isEmpty()) {
			return null;
		}
		return kamcoItemService.getByCltrNo(cltrNo);
	}

	/**
	 * 회원 정보 조회
	 */
	public Member getMemberInfo(String memberId) {
		if (memberId == null || memberId.isEmpty()) {
			return null;
		}
		return memberService.getMemberInfo(memberId);
	}

	/**
	 * 입찰서 작성 페이지 데이터 준비
	 */
	public ServiceResponse<Map<String, Object>> handleBidFormPageRequest(String memberId, Integer auctionNo,
			Long itemId, String cltrNo) {
		Map<String, Object> data = new HashMap<>();

		try {
			if (memberId == null || memberId.isEmpty()) {
				data.put("error", "로그인이 필요합니다.");
				return ServiceResponse.of(HttpStatus.UNAUTHORIZED, data);
			}

			// 이미 작성된 입찰서가 있는지 확인
			Payment existingPayment = getExistingPayment(memberId, auctionNo, itemId, cltrNo);
			if (existingPayment != null) {
				log.warn("이미 작성된 입찰서 존재 - paymentId: {}, memberId: {}", existingPayment.getId(), memberId);
				data.put("error", "이미 입찰서가 작성되었습니다.");
				data.put("existingPaymentId", existingPayment.getId());
				return ServiceResponse.ok(data);
			}

			Map<String, Object> checkoutData = prepareCheckoutPageData(memberId, auctionNo, itemId, cltrNo);
			if (checkoutData.containsKey("error")) {
				log.error("입찰서 작성 페이지 데이터 준비 실패: {}", checkoutData.get("error"));
				return ServiceResponse.ok(checkoutData);
			}

			return ServiceResponse.ok(checkoutData);
		} catch (Exception e) {
			log.error("입찰서 작성 페이지 오류 발생", e);
			data.put("error", "입찰서 작성 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, data);
		}
	}

	/**
	 * 입찰서 제출 처리
	 */
	public ServiceResponse<Map<String, Object>> handleSubmitBidRequest(String memberId,
			Map<String, Object> requestBody) {
		Map<String, Object> response = new HashMap<>();

		try {
			if (memberId == null || memberId.isEmpty()) {
				response.put("success", false);
				response.put("message", "로그인이 필요합니다.");
				return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
			}

			log.info("입찰서 제출 요청 - memberId: {}, requestBody: {}", memberId, requestBody);

			Integer auctionNo = requestBody.get("auctionNo") != null
					? Integer.parseInt(requestBody.get("auctionNo").toString())
					: null;
			Long itemId = requestBody.get("itemId") != null ? Long.parseLong(requestBody.get("itemId").toString())
					: null;
			String cltrNo = requestBody.get("cltrNo") != null ? requestBody.get("cltrNo").toString() : null;
			Long bidAmount = requestBody.get("bidAmount") != null
					? Long.parseLong(requestBody.get("bidAmount").toString())
					: null;
			Long depositAmount = requestBody.get("depositAmount") != null
					? Long.parseLong(requestBody.get("depositAmount").toString())
					: null;
			String itemName = requestBody.get("itemName") != null ? requestBody.get("itemName").toString() : "상품";

			// API 아이템인 경우 auctionNo를 NULL로 설정
			Integer finalAuctionNo = auctionNo;
			if (itemId != null || (cltrNo != null && !cltrNo.isEmpty())) {
				finalAuctionNo = null;
				log.info("API 아이템 감지 - auctionNo를 NULL로 설정: itemId={}, cltrNo={}", itemId, cltrNo);
			}

			log.info(
					"입찰서 제출 파라미터 (최종) - auctionNo: {}, itemId: {}, cltrNo: {}, bidAmount: {}, depositAmount: {}, itemName: {}",
					finalAuctionNo, itemId, cltrNo, bidAmount, depositAmount, itemName);

			// 이미 작성된 입찰서가 있는지 확인
			Payment existingPayment = getExistingPayment(memberId, finalAuctionNo, itemId, cltrNo);
			if (existingPayment != null) {
				log.warn("입찰서 제출 실패 - 이미 작성된 입찰서 존재: paymentId={}, memberId={}", existingPayment.getId(), memberId);
				response.put("success", false);
				response.put("message", "이미 입찰서가 작성되었습니다.");
				response.put("existingPaymentId", existingPayment.getId());
				return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
			}

			if (depositAmount == null || depositAmount <= 0) {
				response.put("success", false);
				response.put("message", "보증금액이 올바르지 않습니다.");
				log.warn("입찰서 제출 실패 - 보증금액이 올바르지 않음: {}", depositAmount);
				return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
			}

			// Payment 생성
			Payment payment = preparePayment(finalAuctionNo, itemId, cltrNo, memberId, depositAmount, itemName);

			if (payment == null || payment.getId() == null) {
				log.error("입찰서 제출 실패 - Payment 생성 실패");
				response.put("success", false);
				response.put("message", "입찰서 제출에 실패했습니다. Payment 생성에 실패했습니다.");
				return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
			}

			log.info("입찰서 제출 성공 - paymentId: {}, merchantUid: {}", payment.getId(), payment.getMerchantUid());

			response.put("success", true);
			response.put("paymentId", payment.getId());
			response.put("merchantUid", payment.getMerchantUid());
			response.put("bidAmount", bidAmount);
			response.put("depositAmount", depositAmount);
			return ServiceResponse.ok(response);

		} catch (NumberFormatException e) {
			log.error("입찰서 제출 중 숫자 형식 오류", e);
			response.put("success", false);
			response.put("message", "입력값 형식이 올바르지 않습니다: " + e.getMessage());
			return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
		} catch (IllegalArgumentException e) {
			log.error("입찰서 제출 중 유효성 검사 오류", e);
			response.put("success", false);
			response.put("message", e.getMessage());
			return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
		} catch (Exception e) {
			log.error("입찰서 제출 중 예상치 못한 오류", e);
			response.put("success", false);
			response.put("message", "입찰서 제출 중 오류가 발생했습니다: " + e.getMessage());
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
		}
	}

	/**
	 * 입찰내역 삭제 처리
	 */
	public ServiceResponse<Map<String, Object>> handleDeletePaymentRequest(String memberId, Long paymentId) {
		Map<String, Object> response = new HashMap<>();

		try {
			if (memberId == null || memberId.isEmpty()) {
				response.put("success", false);
				response.put("message", "로그인이 필요합니다.");
				return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
			}

			// Payment 조회
			Payment payment = getPayment(paymentId);
			if (payment == null) {
				response.put("success", false);
				response.put("message", "입찰내역을 찾을 수 없습니다.");
				return ServiceResponse.of(HttpStatus.NOT_FOUND, response);
			}

			// 본인의 입찰내역만 삭제 가능
			if (!memberId.equals(payment.getMemberId())) {
				response.put("success", false);
				response.put("message", "본인의 입찰내역만 삭제할 수 있습니다.");
				return ServiceResponse.of(HttpStatus.FORBIDDEN, response);
			}

			// 결제가 완료된 경우 삭제 불가
			if ("paid".equals(payment.getStatus())) {
				response.put("success", false);
				response.put("message", "결제가 완료된 입찰내역은 삭제할 수 없습니다.");
				return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
			}

			// 삭제 실행
			boolean deleted = deletePayment(paymentId);
			if (deleted) {
				log.info("입찰내역 삭제 성공 - paymentId: {}, memberId: {}", paymentId, memberId);
				response.put("success", true);
				response.put("message", "입찰내역이 삭제되었습니다.");
				return ServiceResponse.ok(response);
			} else {
				log.error("입찰내역 삭제 실패 - paymentId: {}, memberId: {}", paymentId, memberId);
				response.put("success", false);
				response.put("message", "입찰내역 삭제에 실패했습니다.");
				return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
			}
		} catch (Exception e) {
			log.error("입찰내역 삭제 중 오류 발생 - paymentId: {}, memberId: {}", paymentId, memberId, e);
			response.put("success", false);
			response.put("message", "입찰내역 삭제 중 오류가 발생했습니다: " + e.getMessage());
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
		}
	}

	/**
	 * 내 결제 내역 페이지 요청 처리
	 */
	public ServiceResponse<Map<String, Object>> handleMyPaymentsPageRequest(String memberId) {
		Map<String, Object> data = new HashMap<>();

		try {
			if (memberId == null || memberId.isEmpty()) {
				data.put("redirect", "/memberLogin");
				return ServiceResponse.of(HttpStatus.UNAUTHORIZED, data);
			}

			List<PaymentResponse> paymentResponses = prepareMyPaymentsData(memberId);
			data.put("payments", paymentResponses);
			data.put("pageType", "list");
			return ServiceResponse.ok(data);
		} catch (Exception e) {
			log.error("내 결제 내역 페이지 데이터 준비 중 오류 발생", e);
			data.put("error", "내 결제 내역을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, data);
		}
	}

	/**
	 * 내 결제 내역 데이터 준비 (중복 제거 및 정보 조회 포함)
	 */
	private List<PaymentResponse> prepareMyPaymentsData(String memberId) {
		try {
			List<Payment> payments = getPaymentsByMemberId(memberId);

			// 중복 제거: 같은 auctionNo/itemId/cltrNo에 대한 payment 중 가장 최근 것만 유지
			// itemId와 cltrNo는 같은 물건을 나타낼 수 있으므로 통합 처리
			Map<String, Payment> uniquePayments = new LinkedHashMap<>();
			for (Payment payment : payments) {
				String key = generatePaymentKey(payment);

				// itemId만 있고 cltrNo가 없는 경우, KamcoItem을 조회하여 cltrNo 기반 키로 변환
				if (payment.getItemId() != null && (payment.getCltrNo() == null || payment.getCltrNo().isEmpty())) {
					try {
						com.api.item.domain.KamcoItem kamcoItem = getKamcoItemById(payment.getItemId());
						if (kamcoItem != null && kamcoItem.getCltrNo() != null && !kamcoItem.getCltrNo().isEmpty()) {
							// cltrNo 기반 키로 변경 (더 고유함)
							key = "cltr_" + kamcoItem.getCltrNo();
						}
					} catch (Exception e) {
						log.warn("KamcoItem 조회 실패로 인한 키 변환 실패: itemId={}, error={}", payment.getItemId(),
								e.getMessage());
					}
				}

				// 중복 제거: 같은 키가 없거나, 현재 payment가 더 최근이면 업데이트
				if (!uniquePayments.containsKey(key)) {
					uniquePayments.put(key, payment);
				} else {
					Payment existing = uniquePayments.get(key);
					// createdDate null 체크 및 비교
					Timestamp currentDate = payment.getCreatedDate();
					Timestamp existingDate = existing.getCreatedDate();

					if (currentDate != null && existingDate != null) {
						if (currentDate.after(existingDate)) {
							uniquePayments.put(key, payment);
						}
					} else if (currentDate != null && existingDate == null) {
						// 현재 payment에만 날짜가 있으면 현재 것으로 교체
						uniquePayments.put(key, payment);
					}
					// 둘 다 null이거나 existing만 있으면 기존 것 유지
				}
			}

			// Domain 리스트를 DTO 리스트로 변환
			return uniquePayments.values().stream().map(payment -> {
				PaymentResponse response = PaymentResponse.from(payment);
				// 각 Payment에 대한 Auction/KamcoItem 정보 조회하여 deadlineDate와 itemName 설정
				try {
					if (payment.getAuctionNo() != null) {
						Auction auction = auctionService.getAuction(payment.getAuctionNo());
						if (auction != null) {
							if (auction.getEndDate() != null) {
								response.setDeadlineDate(new Timestamp(auction.getEndDate().getTime()));
							}
							if (response.getItemName() == null || response.getItemName().isEmpty()) {
								response.setItemName(auction.getName());
							}
						}
					} else if (payment.getItemId() != null) {
						com.api.item.domain.KamcoItem kamcoItem = getKamcoItemById(payment.getItemId());
						if (kamcoItem != null) {
							if (kamcoItem.getPbctClsDtm() != null) {
								String dateStr = kamcoItem.getPbctClsDtm().replaceAll("[^0-9]", "");
								if (dateStr.length() >= 14) {
									java.util.Date date = new java.text.SimpleDateFormat("yyyyMMddHHmmss")
											.parse(dateStr);
									response.setDeadlineDate(new Timestamp(date.getTime()));
								}
							}
							if (response.getItemName() == null || response.getItemName().isEmpty()) {
								response.setItemName(
										kamcoItem.getCltrNm() != null ? kamcoItem.getCltrNm() : kamcoItem.getGoodsNm());
							}
							if (response.getCltrNo() == null || response.getCltrNo().isEmpty()) {
								response.setCltrNo(kamcoItem.getCltrNo());
							}
						}
					} else if (payment.getCltrNo() != null) {
						com.api.item.domain.KamcoItem kamcoItem = getKamcoItemByCltrNo(payment.getCltrNo());
						if (kamcoItem != null) {
							if (kamcoItem.getPbctClsDtm() != null) {
								String dateStr = kamcoItem.getPbctClsDtm().replaceAll("[^0-9]", "");
								if (dateStr.length() >= 14) {
									java.util.Date date = new java.text.SimpleDateFormat("yyyyMMddHHmmss")
											.parse(dateStr);
									response.setDeadlineDate(new Timestamp(date.getTime()));
								}
							}
							if (response.getItemName() == null || response.getItemName().isEmpty()) {
								response.setItemName(
										kamcoItem.getCltrNm() != null ? kamcoItem.getCltrNm() : kamcoItem.getGoodsNm());
							}
							if (response.getItemId() == null) {
								response.setItemId(kamcoItem.getId());
							}
						}
					}
				} catch (Exception e) {
					log.warn("입찰 정보 조회 실패: paymentId={}, error={}", payment.getId(), e.getMessage());
				}
				response.formatDates();
				return response;
			}).collect(java.util.stream.Collectors.toList());
		} catch (Exception e) {
			log.error("내 결제 내역 데이터 준비 중 오류 발생", e);
			return new ArrayList<>();
		}
	}

	/**
	 * Payment의 고유 키 생성 (중복 체크용) 같은 물건을 나타내는 payment는 같은 키를 반환해야 함
	 */
	private String generatePaymentKey(Payment payment) {
		if (payment == null) {
			return "null";
		}

		// auctionNo가 있으면 auction 기반 키 사용
		if (payment.getAuctionNo() != null) {
			return "auction_" + payment.getAuctionNo();
		}

		// itemId와 cltrNo는 같은 물건을 나타낼 수 있으므로 통합 처리
		// cltrNo가 있으면 우선 사용 (더 고유함)
		if (payment.getCltrNo() != null && !payment.getCltrNo().isEmpty()) {
			return "cltr_" + payment.getCltrNo();
		}

		// itemId가 있으면 사용
		if (payment.getItemId() != null) {
			return "item_" + payment.getItemId();
		}

		// 모든 식별자가 없으면 개별 키 사용 (중복 제거 불가)
		return "unknown_" + payment.getId();
	}

	/**
	 * PaymentResponse에 deadlineDate 설정 (KamcoItem의 날짜 파싱 포함)
	 */
	public void setDeadlineDateForPaymentResponse(PaymentResponse response, Payment payment, Auction auction) {
		try {
			if (auction != null && auction.getEndDate() != null) {
				response.setDeadlineDate(new Timestamp(auction.getEndDate().getTime()));
			} else if (payment.getItemId() != null || payment.getCltrNo() != null) {
				com.api.item.domain.KamcoItem kamcoItem = payment.getItemId() != null
						? getKamcoItemById(payment.getItemId())
						: getKamcoItemByCltrNo(payment.getCltrNo());
				if (kamcoItem != null && kamcoItem.getPbctClsDtm() != null) {
					String dateStr = kamcoItem.getPbctClsDtm().replaceAll("[^0-9]", "");
					if (dateStr.length() >= 14) {
						java.util.Date date = new java.text.SimpleDateFormat("yyyyMMddHHmmss").parse(dateStr);
						response.setDeadlineDate(new Timestamp(date.getTime()));
					}
				}
			}
		} catch (Exception e) {
			log.warn("입찰 마감일시 조회 실패: paymentId={}, error={}", payment.getId(), e.getMessage());
		}
	}

	/**
	 * 결제 페이지 데이터 준비 (paymentId 처리 포함)
	 */
	public ServiceResponse<Map<String, Object>> handleCheckoutPageRequest(String memberId, Integer auctionNo,
			Long itemId, String cltrNo, Long paymentId, Long bidAmount, Long depositAmount) {
		Map<String, Object> data = new HashMap<>();

		try {
			if (memberId == null || memberId.isEmpty()) {
				data.put("redirect", "/memberLogin");
				return ServiceResponse.of(HttpStatus.UNAUTHORIZED, data);
			}

			// paymentId가 있으면 Payment에서 정보 가져오기
			if (paymentId != null) {
				Payment payment = getPayment(paymentId);
				if (payment != null && memberId.equals(payment.getMemberId())) {
					if (payment.getAuctionNo() != null) {
						auctionNo = payment.getAuctionNo();
					}
					if (payment.getItemId() != null) {
						itemId = payment.getItemId();
					}
					if (payment.getCltrNo() != null) {
						cltrNo = payment.getCltrNo();
					}
					if (bidAmount == null && payment.getAmount() != null) {
						bidAmount = payment.getAmount();
					}
				}
			}

			Map<String, Object> checkoutData = prepareCheckoutPageData(memberId, auctionNo, itemId, cltrNo);
			if (checkoutData.containsKey("error")) {
				return ServiceResponse.ok(checkoutData);
			}

			checkoutData.put("bidAmount", bidAmount);
			checkoutData.put("depositAmount", depositAmount);
			if (depositAmount != null) {
				checkoutData.put("amount", depositAmount);
			}

			return ServiceResponse.ok(checkoutData);
		} catch (Exception e) {
			log.error("결제 페이지 데이터 준비 중 오류 발생", e);
			data.put("error", "결제 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, data);
		}
	}

	/**
	 * 입찰서 제출 완료 페이지 데이터 준비
	 */
	public ServiceResponse<Map<String, Object>> handleBidSubmittedPageRequest(String memberId, Long paymentId,
			Long bidAmount, Long depositAmount, String bidMethod, String paymentMethod, String refundBank,
			String refundAccountNumber, String refundAccountHolder) {
		Map<String, Object> data = new HashMap<>();

		try {
			if (memberId == null || memberId.isEmpty()) {
				data.put("redirect", "/memberLogin");
				return ServiceResponse.of(HttpStatus.UNAUTHORIZED, data);
			}

			if (paymentId == null) {
				log.error("입찰서 제출 완료 페이지 접근 - paymentId가 없음");
				data.put("error", "입찰서 정보를 찾을 수 없습니다. paymentId가 필요합니다.");
				data.put("pageType", "fail");
				return ServiceResponse.ok(data);
			}

			Payment payment = getPayment(paymentId);
			if (payment == null) {
				log.error("입찰서 제출 완료 페이지 접근 - Payment를 찾을 수 없음: paymentId={}", paymentId);
				data.put("error", "입찰서 정보를 찾을 수 없습니다.");
				data.put("pageType", "fail");
				return ServiceResponse.ok(data);
			}

			if (!memberId.equals(payment.getMemberId())) {
				data.put("error", "권한이 없습니다.");
				return ServiceResponse.ok(data);
			}

			// 입찰서 정보 추가
			if (bidAmount != null) {
				data.put("bidAmount", bidAmount);
			}
			if (depositAmount != null) {
				data.put("depositAmount", depositAmount);
			}
			if (bidMethod != null) {
				data.put("bidMethod", bidMethod);
			}
			if (paymentMethod != null) {
				data.put("paymentMethod", paymentMethod);
			}
			if (refundBank != null) {
				data.put("refundBank", refundBank);
			}
			if (refundAccountNumber != null) {
				data.put("refundAccountNumber", refundAccountNumber);
			}
			if (refundAccountHolder != null) {
				data.put("refundAccountHolder", refundAccountHolder);
			}

			// Auction 정보 조회
			Auction auction = null;
			com.api.item.domain.KamcoItem kamcoItem = null;
			if (payment.getAuctionNo() != null) {
				auction = auctionService.getAuction(payment.getAuctionNo());
			} else if (payment.getItemId() != null) {
				kamcoItem = getKamcoItemById(payment.getItemId());
				if (kamcoItem != null) {
					auction = auctionService.convertKamcoItemToAuction(kamcoItem);
				}
			} else if (payment.getCltrNo() != null) {
				kamcoItem = getKamcoItemByCltrNo(payment.getCltrNo());
				if (kamcoItem != null) {
					auction = auctionService.convertKamcoItemToAuction(kamcoItem);
				}
			}

			if (kamcoItem != null) {
				data.put("kamcoItem", kamcoItem);
			}

			Member member = null;
			if (memberId != null) {
				member = getMemberInfo(memberId);
			}

			PaymentResponse paymentResponse = PaymentResponse.from(payment);
			setDeadlineDateForPaymentResponse(paymentResponse, payment, auction);

			data.put("payment", paymentResponse);
			data.put("auction", auction);
			data.put("member", member);
			data.put("buyerName", payment.getBuyerName());
			data.put("buyerEmail", payment.getBuyerEmail());
			data.put("buyerPhone", payment.getBuyerTel());

			return ServiceResponse.ok(data);
		} catch (Exception e) {
			log.error("입찰서 제출 완료 페이지 데이터 준비 중 오류 발생", e);
			data.put("error", "입찰서 제출 완료 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, data);
		}
	}

	/**
	 * 결제 성공 페이지 데이터 준비
	 */
	public ServiceResponse<Map<String, Object>> handlePaymentSuccessPageRequest(String merchantUid, Long bidAmount,
			Long depositAmount, String bidMethod, String selectedBank) {
		Map<String, Object> data = new HashMap<>();

		try {
			Payment payment = getPaymentByMerchantUid(merchantUid);

			if (payment == null) {
				data.put("error", "결제 정보를 찾을 수 없습니다.");
				data.put("pageType", "fail");
				return ServiceResponse.ok(data);
			}

			if (bidAmount != null) {
				data.put("bidAmount", bidAmount);
			}
			if (depositAmount != null) {
				data.put("depositAmount", depositAmount);
			}
			if (bidMethod != null) {
				data.put("bidMethod", bidMethod);
			}
			if (selectedBank != null) {
				data.put("selectedBank", selectedBank);
			}

			// Auction 정보 조회
			Auction auction = null;
			if (payment.getAuctionNo() != null) {
				auction = auctionService.getAuction(payment.getAuctionNo());
			} else if (payment.getItemId() != null) {
				com.api.item.domain.KamcoItem kamcoItem = getKamcoItemById(payment.getItemId());
				if (kamcoItem != null) {
					auction = auctionService.convertKamcoItemToAuction(kamcoItem);
				}
			} else if (payment.getCltrNo() != null) {
				com.api.item.domain.KamcoItem kamcoItem = getKamcoItemByCltrNo(payment.getCltrNo());
				if (kamcoItem != null) {
					auction = auctionService.convertKamcoItemToAuction(kamcoItem);
				}
			}

			String memberId = payment.getMemberId();
			Member member = null;
			if (memberId != null) {
				member = getMemberInfo(memberId);
			}

			PaymentResponse paymentResponse = PaymentResponse.from(payment);
			setDeadlineDateForPaymentResponse(paymentResponse, payment, auction);

			data.put("payment", paymentResponse);
			data.put("auction", auction);
			data.put("member", member);
			data.put("buyerName", payment.getBuyerName());
			data.put("buyerEmail", payment.getBuyerEmail());
			data.put("buyerPhone", payment.getBuyerTel());
			data.put("pageType", "success");

			return ServiceResponse.ok(data);
		} catch (Exception e) {
			log.error("결제 성공 페이지 데이터 준비 중 오류 발생", e);
			data.put("error", "결제 성공 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, data);
		}
	}

	/**
	 * 결제 실패 페이지 데이터 준비
	 */
	public ServiceResponse<Map<String, Object>> handlePaymentFailPageRequest(String message) {
		Map<String, Object> data = new HashMap<>();
		data.put("message", message != null ? message : "결제에 실패했습니다.");
		data.put("pageType", "fail");
		return ServiceResponse.ok(data);
	}

	/**
	 * 결제 상세 페이지 데이터 준비
	 */
	public ServiceResponse<Map<String, Object>> handlePaymentDetailPageRequest(String memberId, Long paymentId) {
		Map<String, Object> data = new HashMap<>();

		try {
			if (memberId == null || memberId.isEmpty()) {
				data.put("redirect", "/memberLogin");
				return ServiceResponse.of(HttpStatus.UNAUTHORIZED, data);
			}

			Payment payment = getPayment(paymentId);

			if (payment == null) {
				data.put("error", "결제 정보를 찾을 수 없습니다.");
				data.put("pageType", "fail");
				return ServiceResponse.ok(data);
			}

			if (!memberId.equals(payment.getMemberId())) {
				data.put("error", "권한이 없습니다.");
				data.put("pageType", "fail");
				return ServiceResponse.ok(data);
			}

			// Auction 정보 조회
			Auction auction = null;
			if (payment.getAuctionNo() != null) {
				auction = auctionService.getAuction(payment.getAuctionNo());
			} else if (payment.getItemId() != null) {
				com.api.item.domain.KamcoItem kamcoItem = getKamcoItemById(payment.getItemId());
				if (kamcoItem != null) {
					auction = auctionService.convertKamcoItemToAuction(kamcoItem);
				}
			} else if (payment.getCltrNo() != null) {
				com.api.item.domain.KamcoItem kamcoItem = getKamcoItemByCltrNo(payment.getCltrNo());
				if (kamcoItem != null) {
					auction = auctionService.convertKamcoItemToAuction(kamcoItem);
				}
			}

			PaymentResponse paymentResponse = PaymentResponse.from(payment);
			setDeadlineDateForPaymentResponse(paymentResponse, payment, auction);

			data.put("payment", paymentResponse);
			data.put("auction", auction);
			data.put("pageType", "detail");

			return ServiceResponse.ok(data);
		} catch (Exception e) {
			log.error("결제 상세 페이지 데이터 준비 중 오류 발생", e);
			data.put("error", "결제 상세 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
			return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, data);
		}
	}
}
