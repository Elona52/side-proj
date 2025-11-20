package com.api.payment.domain;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistory {
    
    private Long id;
    private Long paymentId;         // 결제 ID
    private String status;          // 상태
    private String action;          // 액션 (create, update, cancel 등)
    private String description;     // 설명
    private Timestamp createdDate;
}


