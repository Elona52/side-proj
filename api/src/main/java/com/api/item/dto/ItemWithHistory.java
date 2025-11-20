package com.api.item.dto;

import java.util.List;

import com.api.item.domain.Item;

import lombok.Data;

/**
 * 물건 + 입찰 이력을 포함한 DTO
 * 현재 진행중인 최신 입찰과 과거 입찰 이력을 구분하여 표시
 */
@Data
public class ItemWithHistory {
    private String cltrNo;              // 물건번호 (그룹키)
    private String cltrMnmtNo;          // 물건관리번호
    private String cltrNm;              // 물건명
    private String ctgrFullNm;          // 자산유형명
    private String goodsNm;             // 물건상세설명
    private String ldnmAdrs;            // 지번주소
    private String nmrdAdrs;            // 도로명주소
    
    private Item latest;                // 최신 입찰 (현재 진행중)
    private List<Item> past;            // 과거 입찰 이력
    
    private int totalBidCount;          // 총 입찰 회차
    private Long firstPrice;            // 최초 감정가
    private Long currentPrice;          // 현재 최저입찰가
    private Double priceDropRate;       // 가격 하락률 (%)
}

