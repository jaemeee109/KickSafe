package com.kicksafe.global.common.paging;

import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestDTO {

    @Builder.Default
    private int page = 1;   // 페이지 번호 (기본값: 1페이지)

    @Builder.Default
    private int size = 10;  // 한 페이지에 보여줄 개수 (기본값: 10개)

    private String type;    // 검색 종류 (t:제목, c:내용, w:작성자 -> "tc"면 제목+내용 검색)
    private String keyword; // 검색어

    /**
     * 사용자가 보는 페이지는 1페이지부터 시작하지만,
     * 스프링(JPA)은 0페이지부터 시작.
     * 그래서 "요청한 페이지 - 1"을 해주는 로직이 필수.
     */
    public Pageable getPageable(String sortProperties) {
        // page 가 0보다 작으면 1로 고정 (안전장치)
        int pageNum = this.page < 0 ? 1 : this.page;

        // 크기가 너무 크면 100개로 제한 (공격 방지 안전장치)
        int sizeNum = this.size > 100 ? 100 : this.size;

        // Sort.by(sortProperties).descending(): 최신순(내림차순) 정렬
        return PageRequest.of(pageNum - 1, sizeNum, Sort.by(sortProperties).descending());
    }

    /**
     * 검색 조건이 "tw" (제목+작성자) 같이 올 때,
     * 이걸 ["t", "w"] 문자열 배열로 쪼개주는 도구.
     * 나중에 QueryDSL 쓸 때 아주 유용하게 쓰임.
     */
    public String[] getTypes() {
        if (type == null || type.trim().length() == 0) {
            return null;
        }
        return type.split(""); // 한 글자씩 자름
    }
}
