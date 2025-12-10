package com.kicksafe.post.dto;

// ... 기존 임포트

import com.kicksafe.post.domain.Post;
import com.kicksafe.post.domain.PostMedia; // 추가
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostDetailResponseDTO {

    private Long id;
    private String title;
    private String content;
    private String writer;
    private Long writerId;
    // [수정] 단순 URL 리스트(List<String>)에서 -> 이미지 정보 객체 리스트(List<ImageInfo>)로 변경
    private List<ImageInfo> images;
    private LocalDateTime createdAt;

    // [추가] 게시글 전체 위험도 정보
    private Double riskScoreAvg;
    private String riskLevel;


    // [추가] 이미지 정보를 담을 내부 클래스 (DTO 안의 DTO)
    @Data
    @AllArgsConstructor
    public static class ImageInfo {
        private Long id;       // 이미지 ID (삭제할 때 필요)
        private String url;    // 이미지 URL (보여줄 때 필요)
        private String type; // "IMAGE" 또는 "VIDEO" 값을 담을 변수
        // [추가] 개별 이미지의 점수 정보
        private Double riskScore;
        private String riskLevel;
        private LocalDateTime analyzedAt;
        private String detectionInfo;
    }

    public static PostDetailResponseDTO from(Post post) {
        // [수정] 엔티티의 MediaType(Enum)을 문자열로 변환해서 DTO 에 담기
        List<ImageInfo> imageInfos = post.getMedias().stream()
                .filter(PostMedia::isActive)
                .map(media -> new ImageInfo(
                        media.getId(),
                        media.getFileUrl(),
                        media.getMediaType().toString(),
                        media.getRiskScore(), // [추가] 점수 넣기
                        media.getRiskLevel() != null ? media.getRiskLevel().name() : "LOW", // [추가] 등급 넣기
                        media.getAnalyzedAt(),
                        media.getDetectionInfo()
                ))
                .collect(Collectors.toList());

        return PostDetailResponseDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .writer(post.getMember().getNickname())
                .writerId(post.getMember().getId())
                .images(imageInfos)
                .createdAt(post.getCreatedAt())
                // [추가] 엔티티에서 꺼내서 DTO 에 담기
                .riskScoreAvg(post.getRiskScoreAvg())
                .riskLevel(post.getRiskLevel() != null ? post.getRiskLevel().name() : "LOW")
                .build();
    }
}