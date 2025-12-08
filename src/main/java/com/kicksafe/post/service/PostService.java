package com.kicksafe.post.service;

import com.kicksafe.global.common.paging.PageRequestDTO;
import com.kicksafe.global.common.paging.PageResponseDTO;
import com.kicksafe.post.dto.PostCreateRequestDTO;
import com.kicksafe.post.dto.PostDetailResponseDTO;
import com.kicksafe.post.dto.PostResponseDTO;
import com.kicksafe.post.dto.PostUpdateRequestDTO;

public interface PostService {

    /**
     * 게시글 생성 (저장)
     *
     * @param memberId   작성자 ID (누가 썼는지)
     * @param requestDTO 제목, 내용, 파일 데이터
     * @return 저장된 게시글의 ID
     */
    Long createPost(Long memberId, PostCreateRequestDTO requestDTO);

    /**
     * [게시글 목록 조회]
     * 페이지 번호에 맞는 게시글 목록(10개)을 반환합니다.
     */
    PageResponseDTO<PostResponseDTO> getPostList(PageRequestDTO pageRequestDTO);

    /**
     * [게시글 상세 조회]
     * ID로 게시글 하나를 찾아서 상세 정보를 반환합니다.
     */
    PostDetailResponseDTO getPostDetail(Long postId);

    /**
     * [게시글 수정]
     * 작성자 본인인지 확인하고, 제목/내용/이미지를 수정합니다.
     */
    void updatePost(Long memberId, Long postId, PostUpdateRequestDTO updateDTO);

    /**
     * [게시글 삭제]
     * 작성자 본인인지 확인하고, 게시글과 연관된 이미지를 모두 삭제합니다.
     */
    void deletePost(Long memberId, Long postId);
}