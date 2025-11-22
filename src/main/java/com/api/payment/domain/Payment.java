package com.api.payment.domain;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    private Long id;
    private Integer auctionNo;          // 경매 번호 (DB 경매인 경우)
    private Long itemId;               // API 아이템 ID (KamcoItem인 경우)
    private String cltrNo;             // API 아이템 물건번호 (KamcoItem인 경우)
    private String memberId;            // 구매자 회원 ID
    
    // 아임포트 결제 정보
    private String impUid;              // 아임포트 고유 결제번호
    private String merchantUid;         // 가맹점 주문번호
    private String itemName;            // 상품명 (화면 표시용)
    private Long amount;                // 결제 예정 금액
    private Long paidAmount;            // 실제 결제 금액
    private String paymentMethod;       // 결제 수단 (card, trans, vbank 등)
    private String pgProvider;          // PG사 (html5_inicis, kcp 등)
    private String pgTid;               // PG사 거래번호
    
    // 카드 정보
    private String cardName;            // 카드사 명
    private String cardNumber;          // 카드 번호 (마스킹)
    
    // 구매자 정보
    private String buyerName;           // 구매자 이름
    private String buyerEmail;          // 구매자 이메일
    private String buyerTel;            // 구매자 전화번호
    private String buyerAddr;           // 구매자 주소
    private String buyerPostcode;       // 구매자 우편번호
    
    // 결제 상태 및 시간
    private String status;              // ready, paid, failed, cancelled
    private Timestamp paidAt;           // 결제 완료 시간
    private Timestamp failedAt;         // 결제 실패 시간
    private Timestamp cancelledAt;      // 결제 취소 시간
    
    // 실패/취소 사유
    private String failReason;          // 결제 실패 사유
    private String cancelReason;        // 결제 취소 사유
    
    // 기타
    private String receiptUrl;          // 영수증 URL
    private Timestamp createdDate;
    private Timestamp updatedDate;
}


