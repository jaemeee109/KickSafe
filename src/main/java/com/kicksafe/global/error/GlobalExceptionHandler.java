package com.kicksafe.global.error;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;

/**
 * [전역 예외 처리기]
 * 프로젝트 전역에서 발생하는 모든 에러를 여기서 가로채서 처리.
 */
@Slf4j // 로그 출력을 위해 사용
@RestControllerAdvice // 컨트롤러의 에러를 담당하는 조언자
public class GlobalExceptionHandler {

    /**
     * [1. 로그인 실패 / 인증 실패]
     * 아이디가 없거나 비밀번호가 틀렸을 때 발생
     */
    @ExceptionHandler({UsernameNotFoundException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(Exception e) {
        log.error("인증 실패: {}", e.getMessage());
        return ErrorResponseDTO.toResponseEntity(
                HttpStatus.UNAUTHORIZED.value(), // 401
                "Unauthorized",
                "아이디 또는 비밀번호가 일치하지 않습니다."
        );
    }

    /**
     * [2. 권한 없음]
     * 일반 유저가 관리자 페이지에 접근하려 할 때 발생
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(AccessDeniedException e) {
        log.error("권한 없음: {}", e.getMessage());
        return ErrorResponseDTO.toResponseEntity(
                HttpStatus.FORBIDDEN.value(), // 403
                "Forbidden",
                "해당 리소스에 접근할 권한이 없습니다."
        );
    }

    /**
     * [3. 잘못된 요청 데이터]
     * 예: 숫자를 넣어야 하는데 문자를 넣었거나, 필수 값이 빠졌을 때 (IllegalArgumentException)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("잘못된 요청: {}", e.getMessage());
        return ErrorResponseDTO.toResponseEntity(
                HttpStatus.BAD_REQUEST.value(), // 400
                "Bad Request",
                e.getMessage() // 개발자가 throw 할 때 적은 메시지를 그대로 보여줌
        );
    }

    /**
     * [4. 그 외 모든 예외 (최후의 보루)]
     * 우리가 예상하지 못한 에러가 터졌을 때 처리 (NullPointerException 등)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneralException(Exception e) {
        log.error("알 수 없는 오류 발생: ", e); // 서버 로그에는 자세히 남김
        return ErrorResponseDTO.toResponseEntity(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), // 500
                "Internal Server Error",
                "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요."
        );
    }

    /**
     * [추가됨] ClientAbortException 처리
     * 원인: 동영상 스트리밍 중 사용자가 재생을 멈추거나 페이지를 이탈하면 발생
     * 해결: 에러가 아니라 자연스러운 현상이므로, 로그만 남기고 무시(return null)합니다.
     */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e) {
        // 클라이언트가 연결을 끊었으므로 응답을 보낼 필요가 없음
        log.debug("사용자가 연결을 중단했습니다 (동영상/이미지 로딩 중단): {}", e.getMessage());
    }
}