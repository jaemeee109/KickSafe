package com.kicksafe.post.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * [게시글 작성 요청 DTO]
 * 프론트엔드(PostCreate.js)에서 FormData 로 보낸 데이터를 받는 객체입니다.
 * - title: 제목
 * - content: 내용
 * - images: 업로드할 파일들 (List<MultipartFile>)
 */
@Data
@NoArgsConstructor
public class PostCreateRequestDTO {

    private String title;
    private String content;

    // 이미지는 여러 장일 수 있으므로 리스트로 받음.
    // 프론트에서 formData.append("images", file) 로 보낸 이름과 같아야 함.
    private List<MultipartFile> images = new ArrayList<>();
}