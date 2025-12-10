package com.kicksafe.post.repository;

import com.kicksafe.post.domain.Post;
import com.kicksafe.post.dto.StatisticsResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // [수정됨] 통계 기준 변경: 게시글(Post) -> 이미지(PostMedia)
    // 설명: "게시글 수"가 아니라 "분석된 이미지 각각의 수"를 셉니다.
    // p.riskLevel 대신 pm.riskLevel 을 사용하고, 테이블도 PostMedia(pm)를 조회합니다.
    @Query("SELECT new com.kicksafe.post.dto.StatisticsResponseDTO(pm.riskLevel, COUNT(pm)) " +
            "FROM PostMedia pm " +
            "WHERE pm.isActive = true " +  // 삭제되지 않은 이미지만
            "GROUP BY pm.riskLevel")
    List<StatisticsResponseDTO> findRiskLevelStatistics();

    // [추가] 검색 쿼리 (제목 or 작성자)
    // :type 이 't'면 제목, 'w'면 작성자, 'tw' 면 둘 다 검사
    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND (" +
            "(:type = 't' AND p.title LIKE %:keyword%) OR " +
            "(:type = 'w' AND p.member.nickname LIKE %:keyword%) OR " +
            "(:type = 'tw' AND (p.title LIKE %:keyword% OR p.member.nickname LIKE %:keyword%))" +
            ")")
    Page<Post> searchPosts(@Param("type") String type, @Param("keyword") String keyword, Pageable pageable);

}
