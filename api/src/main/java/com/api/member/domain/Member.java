package com.api.member.domain;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {

	private String id;
	private String pass;
	private String name;
	private String phone;
	private String mail;
	private int zipcode;
	private String address1;
	private String address2;
	private String marketing;
	private Timestamp joinDate;
	private Timestamp modificationDate;
	private String type;
}

