package com.api.payment.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.api.payment.domain.Payment;
import com.api.payment.domain.PaymentHistory;

@Mapper
public interface PaymentMapper {

    // 결제 정보 생성
    void insertPayment(Payment payment);
    
    // 결제 정보 조회 (ID)
    Payment selectPaymentById(@Param("id") Long id);
    
    // 결제 정보 조회 (merchant_uid)
    Payment selectPaymentByMerchantUid(@Param("merchantUid") String merchantUid);
    
    // 결제 정보 조회 (imp_uid)
    Payment selectPaymentByImpUid(@Param("impUid") String impUid);
    
    // 결제 정보 조회 (경매 번호)
    Payment selectPaymentByAuctionNo(@Param("auctionNo") Integer auctionNo);
    
    // 회원의 결제 내역 조회
    List<Payment> selectPaymentsByMemberId(@Param("memberId") String memberId);
    
    // 회원과 auction/item에 대한 기존 payment 조회 (중복 체크용)
    Payment selectPaymentByMemberAndItem(
            @Param("memberId") String memberId,
            @Param("auctionNo") Integer auctionNo,
            @Param("itemId") Long itemId,
            @Param("cltrNo") String cltrNo);
    
    // 결제 정보 업데이트
    void updatePayment(Payment payment);
    
    // 결제 상태 업데이트
    void updatePaymentStatus(@Param("id") Long id, @Param("status") String status);
    
    // 결제 삭제
    void deletePayment(@Param("id") Long id);
    
    // 결제 히스토리 추가
    void insertPaymentHistory(PaymentHistory history);
    
    // 결제 히스토리 조회
    List<PaymentHistory> selectPaymentHistoryByPaymentId(@Param("paymentId") Long paymentId);
}


