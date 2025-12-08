package com.kicksafe.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "REFRESH_TOKEN")
public class RefreshToken {

    @Id
    @Column(name = "RT_KEY")
    private Long key; // Member ID (PK) - 회원 1명당 1개의 리프레시 토큰만 허용

    @Column(name = "RT_VALUE", nullable = false)
    private String value; // 리프레시 토큰 값

    /**
     * 새 로그인 = 새 토큰으로 교체 (Dirty Checking)
     */
    public void updateValue(String token) {
        this.value = token;
    }
}
