package com.kicksafe.post.domain;

import com.kicksafe.global.common.BaseEntity;
import com.kicksafe.global.common.BooleanToYNConverter;
import com.kicksafe.member.domain.Member;
import com.kicksafe.post.constant.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "POST")
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "POST_ID")
    private Long id;

    // 작성자 (Member 테이블과 연결)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID", nullable = false)
    private Member member;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Column(name = "CONTENT", nullable = false, columnDefinition = "TEXT")
    private String content;

    // AI 평균 점수
    @Column(name = "RISK_SCORE_AVG")
    private Double riskScoreAvg;

    // 위험 등급 (LOW, MEDIUM...)
    @Enumerated(EnumType.STRING)
    @Column(name = "RISK_LEVEL")
    private RiskLevel riskLevel;

    // 삭제 여부 (기본값 N -> false)
    @Convert(converter = BooleanToYNConverter.class) // [수정] 컨버터 적용
    @Column(name = "IS_DELETED", nullable = false, length = 1)
    @Builder.Default
    private boolean isDeleted = false; // [수정] String("N") -> boolean(false)로 변경

    // 첨부파일 리스트 (Post 가 삭제되면 얘네도 같이 삭제됨)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostMedia> medias = new ArrayList<>();

    // == 비즈니스 로직 ==
    // AI 분석 결과 업데이트용
    public void updateRiskInfo(Double avgScore, RiskLevel level) {
        this.riskScoreAvg = avgScore;
        this.riskLevel = level;
    }
}
