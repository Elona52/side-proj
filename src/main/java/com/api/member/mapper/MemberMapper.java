package com.api.member.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import com.api.member.domain.Member;

@Mapper
public interface MemberMapper {

	void insertMember(Member m);

	void updateMember(Member m);
	
	Member getMemberInfo(String id);
	
	/**
	 * 전체 회원 목록 조회
	 */
	@Select("SELECT * FROM KNMember ORDER BY joindate DESC")
	List<Member> findAllMembers();
	
	/**
	 * 회원 삭제
	 */
	@Delete("DELETE FROM KNMember WHERE id = #{id}")
	void deleteMember(String id);
	
	/**
	 * 이름과 전화번호로 아이디 찾기
	 */
	@Select("SELECT id FROM KNMember WHERE name = #{name} AND phone = #{phone}")
	String findIdByNameAndPhone(@Param("name") String name, @Param("phone") String phone);
	
	/**
	 * 아이디, 이름, 전화번호로 회원 확인
	 */
	@Select("SELECT * FROM KNMember WHERE id = #{id} AND name = #{name} AND phone = #{phone}")
	Member findMemberByIdNamePhone(@Param("id") String id, @Param("name") String name, @Param("phone") String phone);
}

