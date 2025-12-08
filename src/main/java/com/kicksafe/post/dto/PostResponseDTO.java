package com.kicksafe.post.dto;

import com.kicksafe.post.domain.Post;
import com.kicksafe.post.domain.PostMedia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostResponseDTO {

    private Long id;            // 게시글 ID
    private String title;       // 제목
    private String content;     // 내용 (목록에서는 앞부분만 보여줄 예정)
    private String writer;      // 작성자 닉네임
    private String thumbnail;   // 대표 이미지 URL (첫 번째 사진)
    private LocalDateTime createdAt; // 작성일

    // Entity -> DTO 변환 메서드 (팩토리 메서드)
    public static PostResponseDTO from(Post post) {
        // 이미지 리스트가 있으면 첫 번째 사진을 썸네일로 사용
        String thumbnailUrl = null;
        List<PostMedia> medias = post.getMedias();
        if (medias != null && !medias.isEmpty()) {
            thumbnailUrl = medias.get(0).getFileUrl();
        }

        return PostResponseDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .writer(post.getMember().getNickname())
                .thumbnail(thumbnailUrl)
                .createdAt(post.getCreatedAt())
                .build();
    }
}