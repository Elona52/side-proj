package com.api.auction.domain;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Auction {

	private int  no;
	private String id;
	private String name;
	private String content;
	private Timestamp regDate;
	private Date startDate;
	private Date endDate;
	private int startPrice;
	private int endPrice;
	private String img;
	private String buyer;
	private int count;
	private Map<String, Integer> bidder;
	private boolean depositStatus;
	private Date depositDate;
	private boolean deliveryStatus;
	private Date deliveryDate;
	private boolean remitStatus;
	private Date remitDate;
	
	// API 아이템 구분용
	private String cltrNo;          // 물건번호 (API 데이터용)
	private boolean apiItem;        // API 데이터 여부
}

