package com.api.auction.domain;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reply {

	private int no;
	private String id;
	private String content;
	private Timestamp regDate;
	private int boardNo;
}

