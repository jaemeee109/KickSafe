package com.kicksafe.post.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class PostUpdateRequestDTO {
    private String title;
    private String content;

    // [추가] 삭제할 기존 이미지들의 ID 리스트 (프론트에서 [1, 5, 7] 처럼 보냄)
    private List<Long> deletedMediaIds = new ArrayList<>();

    // [추가] 새로 추가할 이미지 파일 리스트
    private List<MultipartFile> newImages = new ArrayList<>();
}