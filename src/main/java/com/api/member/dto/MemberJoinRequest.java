package com.api.member.dto;

import com.api.member.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberJoinRequest {
    
    private String id;
    private String pass;
    private String name;
    private String phone;
    private String mail;
    private int zipcode;
    private String address1;
    private String address2;
    private String marketing;
    
    /**
     * DTO를 Domain 객체로 변환
     */
    public Member toMember() {
        Member member = new Member();
        member.setId(this.id);
        member.setPass(this.pass);
        member.setName(this.name);
        member.setPhone(this.phone);
        member.setMail(this.mail);
        member.setZipcode(this.zipcode);
        member.setAddress1(this.address1);
        member.setAddress2(this.address2);
        member.setMarketing(this.marketing);
        member.setType("USER"); // 기본값
        return member;
    }
}

