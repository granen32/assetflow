package com.assetflow.member.controller;

import com.assetflow.member.dto.MemberResponse;
import com.assetflow.member.dto.UpdateMemberRequest;
import com.assetflow.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PatchMapping ("/me")
    public MemberResponse update(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid UpdateMemberRequest request) {
        Long memberId = Long.valueOf(jwt.getSubject());
        return memberService.update(memberId, request);
    }
    
}
