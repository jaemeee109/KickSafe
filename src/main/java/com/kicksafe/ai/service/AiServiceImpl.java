package com.kicksafe.ai.service;

import com.kicksafe.ai.dto.AiResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/*
 * [구현체: AI 서비스 로직 실체]
 * -----------------------------------------------------------
 * 역할: AiService 인터페이스의 내용을 실제로 수행하는 클래스입니다.
 * RestTemplate 을 사용해 파이썬(FastAPI) 서버와 통신합니다.
 * -----------------------------------------------------------
 */
@Slf4j // 로그를 찍기 위해 사용 (System.out.println 대신 사용 권장)
@Service // "나 서비스야"라고 스프링에게 알려줌 (빈 등록)
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    // application.properties 파일에 적어둔 파이썬 서버 주소를 가져옵니다.
    // 예: http://127.0.0.1:8000/api/v1/detect/image
    @Value("${ai.server.url}")
    private String aiServerUrl;

    // HTTP 요청을 보내는 도구 생성
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public AiResponseDTO analyzeImage(MultipartFile file) {
        log.info("AI 서버로 이미지 분석 요청 시작... 파일명: {}", file.getOriginalFilename());

        try {
            // 1. 헤더 만들기 (택배 송장 같은 역할)
            // "이 안에 파일 들어있어요(multipart/form-data)"라고 표시
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 2. 바디 만들기 (택배 상자)
            // MultiValueMap 은 폼(<form>) 태그처럼 데이터를 담는 구조입니다.
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // 3. 파일 담기 (중요!)
            // MultipartFile 을 바로 보내면 가끔 에러가 나서, ByteArrayResource 로 감싸줍니다.
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    // 파일 이름이 누락되지 않도록 강제로 넣어줍니다.
                    return file.getOriginalFilename();
                }
            };
            // 파이썬 쪽에서 받는 변수명인 "file"과 스펠링이 똑같아야 합니다!
            body.add("file", resource);

            // 4. 요청 엔티티 생성 (송장 + 상자 합체)
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 5. 파이썬 서버로 발사! (POST 요청)
            // postForEntity(주소, 보낼거, 받을타입)
            ResponseEntity<AiResponseDTO> response = restTemplate.postForEntity(
                    aiServerUrl,
                    requestEntity,
                    AiResponseDTO.class
            );

            // 6. 결과 받아서 꺼내기
            AiResponseDTO result = response.getBody();

            if (result != null && result.getRisk() != null) {
                log.info("AI 응답 완료! 위험 점수: {}점, 등급: {}",
                        result.getRisk().getRisk_score(),
                        result.getRisk().getRisk_level_kor());
            }

            return result;

        } catch (IOException e) {
            log.error("파일 변환 중 에러 발생", e);
            throw new RuntimeException("이미지 파일을 읽는 도중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("AI 서버 통신 실패. 주소: " + aiServerUrl, e);
            throw new RuntimeException("AI 서버와 연결할 수 없습니다. 서버가 켜져있는지 확인해주세요.");
        }
    }
}