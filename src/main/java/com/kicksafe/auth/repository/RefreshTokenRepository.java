package com.kicksafe.auth.repository;

import com.kicksafe.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    /**
     * [강제 삭제 메서드]
     * deleteById는 내부적으로 조회(Select) 후 삭제(Delete)를 하는데,
     * 가끔 조회가 꼬이면 삭제가 안 되는 경우가 있습니다.
     * 그래서 @Query 를 써서 "그냥 이 ID 가진 놈 바로 지워!"라고 DB에 직접 명령합니다.
     */
    @Modifying // "DB 내용을 바꾸는(삭제/수정) 쿼리입니다"라고 알려줌 (필수!)
    @Query("DELETE FROM RefreshToken r WHERE r.key = :key")
    void deleteByKey(@Param("key") Long key);
}
