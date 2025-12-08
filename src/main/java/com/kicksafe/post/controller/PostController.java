package com.kicksafe.post.controller;

import com.kicksafe.global.common.paging.PageRequestDTO;
import com.kicksafe.global.common.paging.PageResponseDTO;
import com.kicksafe.global.security.UserPrincipal;
import com.kicksafe.post.dto.PostCreateRequestDTO;
import com.kicksafe.post.dto.PostDetailResponseDTO;
import com.kicksafe.post.dto.PostResponseDTO;
import com.kicksafe.post.dto.PostUpdateRequestDTO;
import com.kicksafe.post.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * [게시글 작성 API]
     * URL: POST /posts
     * Content-Type: multipart/form-data
     * 파라미터: title, content, images(파일 여러개)
     * 설명: 제목, 내용, 이미지 파일을 받아 게시글을 등록합니다.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createPost(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @ModelAttribute PostCreateRequestDTO requestDTO) {

        log.info("게시글 작성 요청 - 사용자 ID: {}", userPrincipal.getId());
        Long postId = postService.createPost(userPrincipal.getId(), requestDTO);

        return ResponseEntity.ok("게시글이 성공적으로 등록되었습니다. ID: " + postId);
    }

    /**
     * [게시글 목록 조회 API]
     * URL: GET /posts?page=1&size=10
     * 파라미터: page(페이지번호), size(개수), type(검색조건), keyword(검색어)
     * 설명: 페이징 처리된 게시글 목록을 반환합니다.
     */
    @GetMapping
    public ResponseEntity<PageResponseDTO<PostResponseDTO>> getList(
            @ModelAttribute PageRequestDTO pageRequestDTO) {

        log.info("게시글 목록 조회 요청: 페이지 {}", pageRequestDTO.getPage());
        return ResponseEntity.ok(postService.getPostList(pageRequestDTO));
    }

    /**
     * [게시글 상세 조회 API]
     * URL: GET /posts/{postId}
     * 설명: 특정 게시글의 상세 내용과 이미지 리스트를 반환합니다.
     */
    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponseDTO> getPostDetail(@PathVariable Long postId) {

        log.info("게시글 상세 조회 요청: ID {}", postId);
        return ResponseEntity.ok(postService.getPostDetail(postId));
    }

    /**
     * [게시글 수정 API]
     * URL: PUT /posts/{postId}
     * Content-Type: multipart/form-data
     * 파라미터: title, content, deletedMediaIds(삭제할 이미지ID 들), newImages(새 파일들)
     * 설명: 본인 확인 후 게시글 내용 수정 및 이미지 삭제/추가를 수행합니다.
     */
    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updatePost(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long postId,
            @ModelAttribute PostUpdateRequestDTO updateDTO) { // 파일이 있으므로 ModelAttribute 사용

        log.info("게시글 수정 요청 - ID: {}, 사용자: {}", postId, userPrincipal.getId());
        postService.updatePost(userPrincipal.getId(), postId, updateDTO);

        return ResponseEntity.ok("게시글이 수정되었습니다.");
    }

    /**
     * [게시글 삭제 API]
     * URL: DELETE /posts/{postId}
     * 설명: 본인 확인 후 게시글과 첨부파일을 완전히 삭제(Hard Delete)합니다.
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePost(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long postId) {

        log.info("게시글 삭제 요청 - ID: {}, 사용자: {}", postId, userPrincipal.getId());
        postService.deletePost(userPrincipal.getId(), postId);

        return ResponseEntity.ok("게시글이 완전히 삭제되었습니다.");
    }
}