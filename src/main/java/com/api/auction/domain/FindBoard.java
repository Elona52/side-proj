package com.api.auction.domain;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindBoard {

	private int no;
	private String id;
	private String title;
	private String content;
	private String category; // real-estate, movable, site, other
	private int views;
	private String relatedLink;
	private Timestamp regDate;
}

