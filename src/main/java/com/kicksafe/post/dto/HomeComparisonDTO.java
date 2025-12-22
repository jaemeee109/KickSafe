package com.kicksafe.post.dto;

import com.kicksafe.post.domain.PostMedia;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HomeComparisonDTO {
    // 위험한 이미지 정보
    private ImageSummary dangerousImage;
    // 안전한 이미지 정보
    private ImageSummary safeImage;

    @Data
    @Builder
    public static class ImageSummary {
        private Long postId;        // 클릭하면 이동할 게시글 ID
        private String imageUrl;    // 이미지 경로
        private Double riskScore;   // 점수
        private String riskLevel;   // 등급 (HIGH, LOW...)

        // 엔티티 -> DTO 변환 메서드
        public static ImageSummary from(PostMedia media) {
            return ImageSummary.builder()
                    .postId(media.getPost().getId())
                    .imageUrl(media.getFileUrl())
                    .riskScore(media.getRiskScore())
                    .riskLevel(media.getRiskLevel() != null ? media.getRiskLevel().name() : "LOW")
                    .build();
        }
    }
}