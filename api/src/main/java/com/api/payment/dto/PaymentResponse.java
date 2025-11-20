package com.api.payment.dto;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import com.api.payment.domain.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 결제 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    private Long id;
    private Integer auctionNo;
    private Long itemId;
    private String cltrNo;
    private String memberId;
    
    // 아임포트 정보
    private String impUid;
    private String merchantUid;
    private String itemName;            // 상품명
    private Long amount;
    private Long paidAmount;
    private String paymentMethod;
    private String pgProvider;
    // pgTid는 민감정보이므로 제외
    
    // 카드 정보 (마스킹된 정보만)
    private String cardName;
    private String cardNumber;
    
    // 구매자 정보
    private String buyerName;
    private String buyerEmail;
    private String buyerTel;
    private String buyerAddr;
    private String buyerPostcode;
    
    // 결제 상태 및 시간
    private String status;
    private Timestamp paidAt;
    private Timestamp failedAt;
    private Timestamp cancelledAt;
    private Timestamp deadlineDate;  // 입찰 마감일시
    
    // 실패/취소 사유
    private String failReason;
    private String cancelReason;
    
    // 기타
    private String receiptUrl;
    private Timestamp createdDate;
    private Timestamp updatedDate;
    
    // 포맷된 날짜 문자열 (템플릿에서 사용)
    private String formattedCreatedDate;
    private String formattedDeadlineDate;
    
    /**
     * Domain 객체를 DTO로 변환
     */
    public static PaymentResponse from(Payment payment) {
        if (payment == null) {
            return null;
        }
        
        return PaymentResponse.builder()
                .id(payment.getId())
                .auctionNo(payment.getAuctionNo())
                .itemId(payment.getItemId())
                .cltrNo(payment.getCltrNo())
                .memberId(payment.getMemberId())
                .impUid(payment.getImpUid())
                .merchantUid(payment.getMerchantUid())
                .itemName(payment.getItemName())
                .amount(payment.getAmount())
                .paidAmount(payment.getPaidAmount())
                .paymentMethod(payment.getPaymentMethod())
                .pgProvider(payment.getPgProvider())
                // pgTid는 의도적으로 제외 (민감정보)
                .cardName(payment.getCardName())
                .cardNumber(payment.getCardNumber())
                .buyerName(payment.getBuyerName())
                .buyerEmail(payment.getBuyerEmail())
                .buyerTel(payment.getBuyerTel())
                .buyerAddr(payment.getBuyerAddr())
                .buyerPostcode(payment.getBuyerPostcode())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .failedAt(payment.getFailedAt())
                .cancelledAt(payment.getCancelledAt())
                .failReason(payment.getFailReason())
                .cancelReason(payment.getCancelReason())
                .receiptUrl(payment.getReceiptUrl())
                .createdDate(payment.getCreatedDate())
                .updatedDate(payment.getUpdatedDate())
                .build();
    }
    
    /**
     * 날짜 포맷팅 헬퍼 메서드
     */
    private static String formatTimestamp(Timestamp timestamp, String pattern) {
        if (timestamp == null) {
            return "-";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            return sdf.format(timestamp);
        } catch (Exception e) {
            return "-";
        }
    }
    
    /**
     * 포맷된 날짜 문자열 설정
     */
    public void formatDates() {
        this.formattedCreatedDate = formatTimestamp(this.createdDate, "yyyy-MM-dd HH:mm");
        this.formattedDeadlineDate = formatTimestamp(this.deadlineDate, "yyyy-MM-dd HH:mm");
    }
}

