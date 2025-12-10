package com.kicksafe.ai.dto;

import lombok.Data;

import java.util.List;

/*
 * [명세표: AI 응답 데이터 구조]
 * -----------------------------------------------------------
 * 역할: 파이썬(FastAPI)이 분석해서 보내준 JSON 데이터를
 * 스프링부트가 이해할 수 있는 자바 객체로 담는 그릇
 * -----------------------------------------------------------
 */
@Data
public class AiResponseDTO {

    // 1. 객체 탐지 목록 (헬멧 미착용, 킥보드, 사람 등)
    private List<Detection> detections;

    // 2. 위험도 상세 정보 (점수, 등급 등 핵심 정보)
    private Risk risk;

    // --- 내부 클래스: 파이썬이 보내주는 구조와 똑같이 생겨야 함 ---

    @Data
    public static class Detection {
        private String label;      // 감지된 것 이름 (예: "no_helmet")
        private float confidence;  // AI의 확신 정도 (0.0 ~ 1.0)

        // [★추가됨] 좌표 정보 (YOLO OBB 는 4개의 점을 반환합니다)
        // Python 의 schemas.py에 정의된 필드명과 정확히 일치해야 합니다.
        private float x1;
        private float y1;
        private float x2;
        private float y2;
        private float x3;
        private float y3;
        private float x4;
        private float y4;
    }

    @Data
    public static class Risk {
        private float risk_score;      // 위험 점수 (0 ~ 100점)
        private String risk_level;     // 영어 등급 (HIGH, LOW...)
        private String risk_level_kor; // 한글 등급 (위험, 안전...)
        private List<String> risk_factors; // 왜 위험한지 설명
    }
}