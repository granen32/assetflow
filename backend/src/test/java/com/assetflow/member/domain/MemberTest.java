package com.assetflow.member.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Test
    void changesNickname() {
        Member member = new Member("test@example.com", "기존 이름");

        assertThatThrownBy(() -> member.changeNickname(null))
                    .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankNickname() {
        Member member = new Member("test@example.com", "기존 이름");

        assertThatThrownBy(() -> member.changeNickname("   "))
                .isInstanceOf(IllegalArgumentException.class);
            
        assertThat(member.getNickname()).isEqualTo("기존 이름");
    }

    @Test
    void acceptsNicknameWith80Characters() {
        Member member = new Member("test@example.com", "기존 이름");
        String nickname = "가".repeat(80);

        member.changeNickname(nickname);
        assertThat(member.getNickname()).isEqualTo(nickname);
    }

    @Test
    void rejectsNicknameOver80Characters() {
        Member member = new Member("test@example.com", "기존 이름");
        String nickname = "가".repeat(81);

        // 1. changeNickname(nickname) 호출 시
        //    IllegalArgumentException이 발생하는지 확인하세요.

        // 2. 기존 닉네임이 유지되는지 확인하세요.
        assertThatThrownBy(() -> member.changeNickname(nickname))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
   