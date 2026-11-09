package com.assetflow.member.dto;

import com.assetflow.member.domain.Member;

/** 응답에 공개할 필드만 선택한다. 특히 passwordHash는 Entity에 있어도 여기에는 넣지 않는다. */
public record MemberResponse(Long id, String email, String nickname) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getNickname());
    }
}
