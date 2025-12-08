package com.kicksafe.post.domain;

import com.kicksafe.global.common.BaseEntity;
import com.kicksafe.global.common.BooleanToYNConverter;
import com.kicksafe.post.constant.MediaType;
import com.kicksafe.post.constant.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "POST_MEDIA")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PostMedia extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MEDIA_ID")
    private Long id;

    // 어떤 게시물의 파일인지 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POST_ID", nullable = false)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(name = "MEDIA_TYPE", nullable = false)
    private MediaType mediaType;

    @Column(name = "FILE_URL", nullable = false, length = 500)
    private String fileUrl;

    // [추가됨] 실제 서버에 저장된 파일명 (예: uuid-image.jpg)
    // 파일을 실제로 삭제하거나 관리할 때 이 이름이 꼭 필요합니다[cite: 106].
    @Column(name = "STORED_FILE_NAME", length = 500)
    private String storedFileName;

    @Column(name = "THUMBNAIL_URL", length = 500)
    private String thumbnailUrl;

    @Column(name = "FILE_SIZE_BYTES", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "FILE_ORDER", nullable = false)
    @Builder.Default
    private Integer fileOrder = 1;

    // 개별 파일 점수
    @Column(name = "RISK_SCORE")
    private Double riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "RISK_LEVEL")
    private RiskLevel riskLevel;

    @Column(name = "ANALYZED_AT")
    private LocalDateTime analyzedAt;

    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "IS_ACTIVE", nullable = false, length = 1)
    @Builder.Default
    private boolean isActive = true;

    // [추가됨] 활성화 상태 변경 편의 메서드
    // Lombok @Setter 가 있어서 없어도 되지만, 명시적으로 비즈니스 로직을 표현하기 위해 추가했습니다.
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}