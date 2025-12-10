package com.kicksafe.ai.service;

import com.kicksafe.ai.dto.AiResponseDTO;
import org.springframework.web.multipart.MultipartFile;

/*
 * [명세표: AI 서비스 인터페이스]
 * -----------------------------------------------------------
 * 역할: AI 기능의 설계도입니다.
 * "이미지를 주면 -> 분석 결과(DTO)를 반환한다"는 약속만 정의합니다.
 * -----------------------------------------------------------
 */
public interface AiService {

    /**
     * 킥보드 이미지를 AI 서버로 보내서 분석 결과를 받아옵니다.
     * @param file 사용자가 업로드한 이미지 파일
     * @return 분석 결과 (위험 점수, 탐지된 객체 등)
     */
    AiResponseDTO analyzeImage(MultipartFile file);

}