package com.api.member.dto;

import java.sql.Timestamp;
import com.api.member.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회원 정보 응답 DTO
 * 비밀번호를 제외한 안전한 정보만 제공
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    
    private String id;
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
    
    /**
     * Domain 객체를 DTO로 변환
     */
    public static MemberResponse from(Member member) {
        if (member == null) {
            return null;
        }
        
        return MemberResponse.builder()
                .id(member.getId())
                .name(member.getName())
                .phone(member.getPhone())
                .mail(member.getMail())
                .zipcode(member.getZipcode())
                .address1(member.getAddress1())
                .address2(member.getAddress2())
                .marketing(member.getMarketing())
                .joinDate(member.getJoinDate())
                .modificationDate(member.getModificationDate())
                .type(member.getType())
                .build();
    }
}

