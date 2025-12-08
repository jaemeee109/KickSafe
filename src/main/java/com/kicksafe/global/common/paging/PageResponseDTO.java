package com.kicksafe.global.common.paging;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class PageResponseDTO<E> { // // <E>는 제네릭 (회원 목록이든 게시물 목록이든 다 담을 수 있음)

    // 1. 실제 데이터 목록 (예: 게시물 10개)
    private List<E> dtoList;

    // 2. 페이징 처리를 위한 정보들
    private int page;       // 현재 페이지 번호
    private int size;       // 페이지당 개수
    private int total;      // 전체 데이터 개수 (게시물이 총 123개다!)

    // 3. 화면 아래 페이지 버튼 계산용 (예: [11] ... [20])
    private int start;      // 시작 페이지 번호 (11)
    private int end;        // 끝 페이지 번호 (20)
    private boolean prev;   // [이전] 버튼 표시 여부
    private boolean next;   // [다음] 버튼 표시 여부

    @Builder(builderMethodName = "withAll")
    public PageResponseDTO(PageRequestDTO pageRequestDTO, List<E> dtoList, int total) {

        this.dtoList = dtoList;
        this.page = pageRequestDTO.getPage();
        this.size = pageRequestDTO.getSize();
        this.total = total;

        // [핵심 로직] 페이징 바 끝 번호 계산
        // 공식: Math.ceil(현재페이지 / 10.0) * 10
        // 예: 1페이지 -> ceil(0.1) * 10 = 10 (1~10페이지 보여줌)
        // 예: 13페이지 -> ceil(1.3) * 10 = 20 (11~20페이지 보여줌)
        this.end = (int) (Math.ceil(this.page / 10.0)) * 10;

        // 시작 번호 계산 (끝 번호 - 9)
        // 예: 끝이 20이면 시작은 11
        this.start = this.end - 9;

        // [진짜 끝 번호 보정]
        // 데이터가 적어서 20페이지까지 갈 필요가 없고 15페이지가 끝이라면?
        // end 를 20에서 15로 바꿔야 함.
        int realEnd = (int) (Math.ceil((double) total / this.size));

        if (realEnd < this.end) {
            this.end = realEnd;
        }

        // [이전] 버튼: 시작 페이지가 1보다 크면 존재 (예: 11페이지부터는 이전 버튼 필요)
        this.prev = this.start > 1;

        // [다음] 버튼: 현재 보여주는 끝 페이지(end)보다 진짜 끝(realEnd)이 더 크면 존재
        this.next = this.total > this.end * this.size;
    }
}
