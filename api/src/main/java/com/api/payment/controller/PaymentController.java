package com.api.payment.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.api.auction.domain.Auction;
import com.api.member.domain.Member;
import com.api.payment.domain.Payment;
import com.api.payment.dto.PaymentResponse;
import com.api.common.dto.ServiceResponse;
import com.api.auction.service.AuctionService;
import com.api.member.service.MemberService;
import com.api.payment.service.PaymentService;
import com.api.config.IamportConfig;

import lombok.extern.slf4j.Slf4j;
import java.sql.Timestamp;

@Slf4j
@Controller
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private AuctionService auctionService;
    
    @Autowired
    private MemberService memberService;
    
    @Autowired
    private IamportConfig iamportConfig;

    /**
     * 입찰서 작성 페이지로 이동
     */
    @GetMapping("/bid-form")
    public String bidFormPage(
            @RequestParam(name = "auctionNo", required = false) Integer auctionNo,
            @RequestParam(name = "itemId", required = false) Long itemId,
            @RequestParam(name = "cltrNo", required = false) String cltrNo,
            Model model, 
            HttpSession session) {
        
        String memberId = (String) session.getAttribute("loginId");
        ServiceResponse<Map<String, Object>> serviceResponse = paymentService.handleBidFormPageRequest(memberId, auctionNo, itemId, cltrNo);
        Map<String, Object> data = serviceResponse.getBody();
        
        return processServiceResponse(data, model, "payment/bid-form");
    }

    /**
     * 결제 페이지로 이동
     */
    @GetMapping("/checkout")
    public String checkoutPage(
            @RequestParam(name = "auctionNo", required = false) Integer auctionNo,
            @RequestParam(name = "itemId", required = false) Long itemId,
            @RequestParam(name = "cltrNo", required = false) String cltrNo,
            @RequestParam(name = "paymentId", required = false) Long paymentId,
            @RequestParam(name = "bidAmount", required = false) Long bidAmount,
            @RequestParam(name = "depositAmount", required = false) Long depositAmount,
            Model model, 
            HttpSession session) {
        
        String memberId = (String) session.getAttribute("loginId");
        ServiceResponse<Map<String, Object>> serviceResponse = paymentService.handleCheckoutPageRequest(
                memberId, auctionNo, itemId, cltrNo, paymentId, bidAmount, depositAmount);
        Map<String, Object> data = serviceResponse.getBody();
        
        String viewName = processServiceResponse(data, model, "payment/payment");
        if (!viewName.startsWith("redirect:")) {
            model.addAttribute("iamportImpCode", iamportConfig.getImpCode());
        }
        
        return viewName;
    }
    

    /**
     * 입찰서 제출 (결제 없이 입찰서만 제출)
     */
    @PostMapping("/submit-bid")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitBid(
            @RequestBody Map<String, Object> requestBody,
            HttpSession session) {
        String memberId = (String) session.getAttribute("loginId");
        return paymentService.handleSubmitBidRequest(memberId, requestBody).toResponseEntity();
    }

    /**
     * 결제 준비 (주문번호 생성)
     */
    @PostMapping("/prepare")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> preparePayment(
            @RequestBody Map<String, Object> requestBody,
            HttpSession session) {
        String memberId = (String) session.getAttribute("loginId");
        return paymentService.handlePreparePaymentRequest(memberId, requestBody).toResponseEntity();
    }

    /**
     * 결제 완료 (아임포트 콜백)
     */
    @PostMapping("/complete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> completePayment(@RequestBody Map<String, Object> requestBody) {
        String impUid = requestBody.get("imp_uid") != null ? requestBody.get("imp_uid").toString() : null;
        String merchantUid = requestBody.get("merchant_uid") != null ? requestBody.get("merchant_uid").toString() : null;
        log.info("결제 완료 콜백 - impUid: {}, merchantUid: {}", impUid, merchantUid);
        return paymentService.handleCompletePaymentRequest(impUid, merchantUid).toResponseEntity();
    }

    /**
     * 입찰서 제출 완료 페이지
     */
    @GetMapping("/bid-submitted")
    public String bidSubmittedPage(
            @RequestParam(name = "paymentId", required = false) Long paymentId,
            @RequestParam(name = "bidAmount", required = false) Long bidAmount,
            @RequestParam(name = "depositAmount", required = false) Long depositAmount,
            @RequestParam(name = "bidMethod", required = false) String bidMethod,
            @RequestParam(name = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(name = "refundBank", required = false) String refundBank,
            @RequestParam(name = "refundAccountNumber", required = false) String refundAccountNumber,
            @RequestParam(name = "refundAccountHolder", required = false) String refundAccountHolder,
            Model model, 
            HttpSession session) {
        
        String memberId = (String) session.getAttribute("loginId");
        ServiceResponse<Map<String, Object>> serviceResponse = paymentService.handleBidSubmittedPageRequest(
                memberId, paymentId, bidAmount, depositAmount, bidMethod, paymentMethod,
                refundBank, refundAccountNumber, refundAccountHolder);
        Map<String, Object> data = serviceResponse.getBody();
        
        return processServiceResponse(data, model, "payment/bid-submitted");
    }

    /**
     * 결제 성공 페이지
     */
    @GetMapping("/success")
    public String paymentSuccess(
            @RequestParam(name = "merchantUid") String merchantUid,
            @RequestParam(name = "bidAmount", required = false) Long bidAmount,
            @RequestParam(name = "depositAmount", required = false) Long depositAmount,
            @RequestParam(name = "bidMethod", required = false) String bidMethod,
            @RequestParam(name = "selectedBank", required = false) String selectedBank,
            Model model) {
        
        ServiceResponse<Map<String, Object>> serviceResponse = paymentService.handlePaymentSuccessPageRequest(
                merchantUid, bidAmount, depositAmount, bidMethod, selectedBank);
        Map<String, Object> data = serviceResponse.getBody();
        
        return processServiceResponse(data, model, "payment/bid-submitted");
    }

    /**
     * 결제 실패 페이지
     */
    @GetMapping("/fail")
    public String paymentFail(@RequestParam(name = "message", required = false) String message, Model model) {
        ServiceResponse<Map<String, Object>> serviceResponse = paymentService.handlePaymentFailPageRequest(message);
        Map<String, Object> data = serviceResponse.getBody();
        model.addAllAttributes(data);
        return "payment/payment";
    }

    /**
     * 내 결제 내역 조회
     */
    @GetMapping("/my-payments")
    public String myPayments(HttpSession session, Model model) {
        String memberId = (String) session.getAttribute("loginId");
        ServiceResponse<Map<String, Object>> serviceResponse = paymentService.handleMyPaymentsPageRequest(memberId);
        Map<String, Object> data = serviceResponse.getBody();
        
        return processServiceResponse(data, model, "payment/my-payments");
    }

    /**
     * 결제 상세 정보 조회
     */
    @GetMapping("/detail/{paymentId}")
    public String paymentDetail(@PathVariable("paymentId") Long paymentId, Model model, HttpSession session) {
        String memberId = (String) session.getAttribute("loginId");
        ServiceResponse<Map<String, Object>> serviceResponse = paymentService.handlePaymentDetailPageRequest(memberId, paymentId);
        Map<String, Object> data = serviceResponse.getBody();
        
        return processServiceResponse(data, model, "payment/bid-detail");
    }

    /**
     * 서비스 응답 처리 (redirect/error 체크 및 모델 설정)
     */
    private String processServiceResponse(Map<String, Object> data, Model model, String defaultView) {
        if (data.containsKey("redirect")) {
            return "redirect:" + data.get("redirect");
        }
        
        if (data.containsKey("error")) {
            model.addAllAttributes(data);
            return "payment/payment";
        }
        
        model.addAllAttributes(data);
        return defaultView;
    }

    /**
     * 결제 취소
     */
    @PostMapping("/cancel/{paymentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelPayment(
            @PathVariable("paymentId") Long paymentId,
            @RequestBody Map<String, String> requestBody,
            HttpSession session) {
        String memberId = (String) session.getAttribute("loginId");
        String reason = requestBody != null ? requestBody.get("reason") : null;
        return paymentService.handleCancelPaymentRequest(memberId, paymentId, reason).toResponseEntity();
    }

    /**
     * 입찰내역 삭제 (DELETE 메서드)
     */
    @DeleteMapping("/delete/{paymentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePayment(
            @PathVariable("paymentId") Long paymentId,
            HttpSession session) {
        String memberId = (String) session.getAttribute("loginId");
        return paymentService.handleDeletePaymentRequest(memberId, paymentId).toResponseEntity();
    }

    /**
     * 입찰내역 삭제 (POST 메서드 - 대안)
     */
    @PostMapping("/delete/{paymentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePaymentPost(
            @PathVariable("paymentId") Long paymentId,
            HttpSession session) {
        String memberId = (String) session.getAttribute("loginId");
        return paymentService.handleDeletePaymentRequest(memberId, paymentId).toResponseEntity();
    }
}


