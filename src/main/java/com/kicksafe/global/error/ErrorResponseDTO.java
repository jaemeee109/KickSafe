package com.kicksafe.global.error;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

/**
 * [에러 응답 공통 규격 (포장 박스)]
 * 서버에서 에러가 났을 때, 프론트엔드에게 항상 "똑같은 모양"으로 데이터를 주기 위해 만든 클래스.
 * 에러마다 모양이 다르면 프론트엔드 개발자가 처리하기 힘듦
 * 그래서 항상 { 시간, 상태코드, 에러이름, 메시지 } 이 4가지를 담아서 보내기로 약속.
 */
@Getter
@Builder
public class ErrorResponseDTO {

    // 1. 에러가 발생한 시간 (예: 2024-12-03T12:00:00)
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    // 2. HTTP 상태 코드 (예: 400, 401, 404, 500)
    private final int status;

    // 3. 에러 이름 (예: Bad Request, Unauthorized)
    private final String error;

    // 4. 우리가 정한 커스텀 에러 코드 (선택 사항)
    private final String code;

    // 5. "아이디가 틀렸습니다.", "중복된 닉네임입니다."
    private final String message;

    /**
     * [편의 메서드: toResponseEntity]
     * 매번 ErrorResponseDTO.builder()... 하고 ResponseEntity.status()... 하려니 코드가 너무 길어짐
     * 그래서 "상태코드, 에러이름, 메시지"만 던져주면 알아서 완성된 응답(ResponseEntity)을 만들어주는 도우미 메서드입니다.
     */
    public static ResponseEntity<ErrorResponseDTO> toResponseEntity(int status, String error, String message) {
        return ResponseEntity
                .status(status) // HTTP 상태 코드 설정 (헤더)
                .body(ErrorResponseDTO.builder() // 실제 내용물 (바디)
                        .status(status)
                        .error(error)
                        .code("ERROR") // 기본값으로 그냥 "ERROR"라고 넣음 (나중에 필요하면 수정)
                        .message(message)
                        .build()
                );
    }
}