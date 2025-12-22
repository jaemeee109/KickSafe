package com.kicksafe.post.repository;

import com.kicksafe.post.domain.PostMedia;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * [이미지 데이터 전용 저장소]
 * 게시글(Post)이 아니라 개별 사진(PostMedia) 단위로 데이터를 찾을 때 사용합니다.
 */
public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {

    /**
     * [가장 위험한 이미지 찾기]
     * 1. isActive = true (삭제되지 않은 사진만)
     * 2. riskScore 내림차순 정렬 (점수 높은 순)
     * 3. Pageable 을 사용해 상위 1개만 가져옴
     */
    @Query("SELECT pm FROM PostMedia pm JOIN FETCH pm.post WHERE pm.isActive = true ORDER BY pm.riskScore DESC")
    List<PostMedia> findMostDangerous(Pageable pageable);

    /**
     * [가장 안전한 이미지 찾기]
     * 1. isActive = true
     * 2. riskScore 오름차순 정렬 (점수 낮은 순)
     * 3. Pageable 을 사용해 상위 1개만 가져옴
     */
    @Query("SELECT pm FROM PostMedia pm JOIN FETCH pm.post WHERE pm.isActive = true ORDER BY pm.riskScore ASC")
    List<PostMedia> findSafest(Pageable pageable);
}