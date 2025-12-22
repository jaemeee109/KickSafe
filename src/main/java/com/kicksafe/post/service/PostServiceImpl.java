package com.kicksafe.post.service;

import com.fasterxml.jackson.databind.ObjectMapper; // [필수] JSON 변환 라이브러리
import com.kicksafe.ai.dto.AiResponseDTO;
import com.kicksafe.ai.service.AiService;
import com.kicksafe.global.common.image.FileStore;
import com.kicksafe.global.common.image.UploadFileDTO;
import com.kicksafe.global.common.paging.PageRequestDTO;
import com.kicksafe.global.common.paging.PageResponseDTO;
import com.kicksafe.member.constant.MemberRole;
import com.kicksafe.member.domain.Member;
import com.kicksafe.member.repository.MemberRepository;
import com.kicksafe.post.constant.MediaType;
import com.kicksafe.post.constant.RiskLevel;
import com.kicksafe.post.domain.Post;
import com.kicksafe.post.domain.PostMedia;
import com.kicksafe.post.dto.*;
import com.kicksafe.post.repository.PostMediaRepository;
import com.kicksafe.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로는 조회 모드 (성능 최적화)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final FileStore fileStore; // 파일 저장을 도와주는 커스텀 클래스
    private final AiService aiService; // AI 서비스 주입
    private final PostMediaRepository postMediaRepository; // [★추가] 이미지 전용 레포지토리 주입

    // [★추가됨] 자바 객체(List)를 JSON 문자열로 바꿔주는 도구 주입
    private final ObjectMapper objectMapper;

    /**
     * [게시글 생성 (저장)]
     * =================================================================================
     * | 기능 | 제목, 내용, 이미지 파일을 받아 DB와 서버 디스크에 저장함 |
     * | 입력 | memberId(작성자), requestDTO(제목, 내용, 파일리스트) |
     * | 출력 | Long (저장된 게시글의 ID) |
     * =================================================================================
     * [로직 설명]
     * 1. 작성자 확인: DB에 존재하는 회원인지 검사합니다.
     * 2. 파일 저장: 업로드된 이미지가 있다면 'FileStore' 를 통해 디스크(C:/...)에 저장합니다.
     * 3. DB 저장: 게시글 정보(Post)와 이미지 정보(PostMedia)를 연결하여 한 번에 저장합니다.
     * - Cascade 옵션 덕분에 Post 만 저장해도 PostMedia 가 같이 저장됩니다.
     */
    @Override
    @Transactional
    public Long createPost(Long memberId, PostCreateRequestDTO requestDTO) {

        // 1. 작성자 찾기
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 2. 게시글 엔티티 생성
        Post post = Post.builder()
                .member(member)
                .title(requestDTO.getTitle())
                .content(requestDTO.getContent())
                .isDeleted(false)
                .build();

        // 3. 파일 처리
        List<MultipartFile> files = requestDTO.getImages();

        // 게시글 전체 평균 점수용 변수
        double totalRiskScore = 0.0;
        int imageCount = 0;
        RiskLevel highestRiskLevel = RiskLevel.LOW;

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                try {
                    // [순서 변경 1] 파일 타입 먼저 확인 (AI한테 보낼지 말지 결정해야 하니까)
                    MediaType mediaType = MediaType.IMAGE;
                    String contentType = file.getContentType();
                    if (contentType != null && contentType.startsWith("video")) {
                        mediaType = MediaType.VIDEO;
                    }

                    // [순서 변경 2] AI 분석을 ★저장하기 전에★ 먼저 수행! (중요)
                    // (저장을 먼저 해버리면 임시 파일이 이동되면서 사라져서 에러가 남)
                    Double riskScore = 0.0;
                    RiskLevel riskLevel = RiskLevel.LOW;

                    // [★추가] AI 감지 결과(좌표)를 저장할 변수 (JSON 문자열)
                    String detectionJson = null;

                    if (mediaType == MediaType.IMAGE) {
                        try {
                            // AI 서비스 호출 (이때는 아직 임시 파일이 존재함)
                            AiResponseDTO aiResult = aiService.analyzeImage(file);

                            if (aiResult != null && aiResult.getRisk() != null) {
                                riskScore = (double) aiResult.getRisk().getRisk_score();
                                try {
                                    riskLevel = RiskLevel.valueOf(aiResult.getRisk().getRisk_level());
                                } catch (IllegalArgumentException e) {
                                    riskLevel = RiskLevel.LOW;
                                }

                                // ========================================================
                                // [★핵심 로직] 감지된 객체 정보(List)를 JSON 문자열로 변환
                                // ========================================================
                                if (aiResult.getDetections() != null) {
                                    // 예: [{x1:10, y1:20, label:"no_helmet"...}] 형태로 변환됨
                                    detectionJson = objectMapper.writeValueAsString(aiResult.getDetections());
                                }

                                // 통계 합산
                                totalRiskScore += riskScore;
                                imageCount++;
                                if (riskLevel.ordinal() > highestRiskLevel.ordinal()) {
                                    highestRiskLevel = riskLevel;
                                }
                            }
                        } catch (Exception e) {
                            log.error("AI 분석 실패 (무시하고 저장 진행): {}", e.getMessage());
                        }
                    }

                    // [순서 변경 3] 이제 파일 저장 (여기서 임시 파일이 이동됨)
                    UploadFileDTO uploadFile = fileStore.storeFile(file);

                    // (4) DB 엔티티 생성 및 추가
                    PostMedia postMedia = PostMedia.builder()
                            .post(post)
                            .mediaType(mediaType)
                            .fileUrl(uploadFile.getFileUrl())
                            .storedFileName(uploadFile.getStoredFileName())
                            .fileSizeBytes(file.getSize())
                            .fileOrder(post.getMedias().size() + 1)
                            .isActive(true)
                            .analyzedAt(LocalDateTime.now())
                            .riskScore(riskScore) // 점수 저장
                            .riskLevel(riskLevel) // 등급 저장

                            // [★추가] JSON 데이터(좌표) 저장
                            .detectionInfo(detectionJson)
                            .build();

                    post.getMedias().add(postMedia);

                } catch (IOException e) {
                    throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
                }
            }
        }

        // 4. 게시글 전체 통계 업데이트
        if (imageCount > 0) {
            double avgScore = totalRiskScore / imageCount;
            post.updateRiskInfo(avgScore, highestRiskLevel);
        }

        // 5. 최종 저장
        Post savedPost = postRepository.save(post);
        log.info("게시글 저장 완료: ID {}", savedPost.getId());
        return savedPost.getId();
    }

    /**
     * [게시글 목록 조회 (페이징)]
     * =================================================================================
     * | 기능 | 요청된 페이지 번호에 맞는 게시글 10개를 최신순으로 조회함 |
     * | 입력 | PageRequestDTO (페이지 번호, 검색 조건 등) |
     * | 출력 | PageResponseDTO (데이터 목록 + 페이지네이션 버튼 정보) |
     * =================================================================================
     * [로직 설명]
     * 1. Pageable 생성: 요청된 페이지 번호를 스프링 포맷(0부터 시작)으로 변환하고, ID 역순으로 정렬합니다.
     * 2. DB 조회: findAll(pageable)을 통해 딱 필요한 개수만큼만 데이터를 가져옵니다.
     * 3. DTO 변환: 엔티티(Entity) 리스트를 화면용 객체(DTO) 리스트로 변환합니다.
     * 4. 결과 반환: 목록 데이터와 '이전/다음' 버튼 계산 정보를 합쳐서 반환합니다.
     */
    @Override
    public PageResponseDTO<PostResponseDTO> getPostList(PageRequestDTO pageRequestDTO) {

        Pageable pageable = pageRequestDTO.getPageable("id");
        String type = pageRequestDTO.getType();     // 검색 타입 (t, w, tw)
        String keyword = pageRequestDTO.getKeyword(); // 검색어

        Page<Post> result;

        // 1. 검색어가 있으면 -> 검색 쿼리 실행
        if (keyword != null && !keyword.trim().isEmpty() && type != null) {
            result = postRepository.searchPosts(type, keyword, pageable);
        }
        // 2. 검색어가 없으면 -> 전체 목록 조회
        else {
            result = postRepository.findAll(pageable); // (삭제된 거 제외하려면 여기도 쿼리 수정 필요하지만 일단 패스)
            // *참고: 원래 findAll 은 isDeleted=true 인 것도 가져옵니다.
            // 완벽하게 하려면 findAllByIsDeletedFalse(pageable) 메서드를 레포지토리에 추가해서 쓰는 게 좋습니다.
        }

        List<PostResponseDTO> dtoList = result.getContent().stream()
                .map(PostResponseDTO::from)
                .collect(Collectors.toList());

        return PageResponseDTO.<PostResponseDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .total((int) result.getTotalElements())
                .build();
    }

    /**
     * [게시글 상세 조회]
     * =================================================================================
     * | 기능 | 게시글 ID로 상세 내용과 포함된 이미지 리스트를 조회함 |
     * | 입력 | postId (게시글 번호) |
     * | 출력 | PostDetailResponseDTO (제목, 내용, 작성자, 이미지 정보 등) |
     * =================================================================================
     * [로직 설명]
     * 1. DB 조회: ID로 게시글을 찾습니다. 없으면 예외를 발생시킵니다.
     * 2. DTO 변환: 게시글 정보와 연결된 모든 이미지 정보를 DTO 에 담아 반환합니다.
     */
    @Override
    public PostDetailResponseDTO getPostDetail(Long postId) {
        // 1. DB 에서 ID로 게시글 찾기 (없으면 에러)
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("해당 게시글이 존재하지 않습니다. ID: " + postId));

        // 2. DTO 로 변환해서 반환
        return PostDetailResponseDTO.from(post);
    }

    /**
     * [게시글 수정 (이미지 포함 / Hard Delete)]
     * =================================================================================
     * | 기능 | 작성자 본인 확인 후, 내용 수정 및 이미지 삭제/추가를 처리함 |
     * | 입력 | memberId, postId, updateDTO(변경할 제목/내용, 삭제할 이미지 ID, 새 파일) |
     * | 출력 | 없음 (void) |
     * =================================================================================
     * [로직 설명]
     * 1. 권한 확인: 본인 글인지 확인합니다.
     * 2. 이미지 삭제: 요청된 이미지 ID가 있다면, 파일(디스크)과 DB 데이터를 영구 삭제합니다.
     * - orphanRemoval=true 로 인해 리스트에서 제거하면 DB 에서도 삭제됩니다.
     * 3. 이미지 추가: 새 파일이 있다면 디스크에 저장하고 DB 리스트에 추가합니다.
     * 4. 내용 수정: JPA Dirty Checking(변경 감지)으로 제목과 내용이 자동 업데이트됩니다.
     */
    @Override
    @Transactional
    public void updatePost(Long memberId, Long postId, PostUpdateRequestDTO updateDTO) {
        // 1. 조회 및 권한 체크
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.getMember().getId().equals(memberId)) {
            throw new RuntimeException("수정 권한이 없습니다.");
        }

        // =================================================
        // 2. 이미지 삭제 (기존 로직 동일)
        // =================================================
        List<Long> deletedMediaIds = updateDTO.getDeletedMediaIds();
        if (deletedMediaIds != null && !deletedMediaIds.isEmpty()) {
            List<PostMedia> mediasToDelete = post.getMedias().stream()
                    .filter(media -> deletedMediaIds.contains(media.getId()))
                    .toList();

            for (PostMedia media : mediasToDelete) {
                if (media.getStoredFileName() != null) {
                    fileStore.deleteFile(media.getStoredFileName());
                }
            }
            post.getMedias().removeAll(mediasToDelete);
        }

        // =================================================
        // 3. 새 이미지 추가 + AI 분석 로직
        // =================================================
        List<MultipartFile> newImages = updateDTO.getNewImages();
        if (newImages != null && !newImages.isEmpty()) {
            int currentMediaCount = post.getMedias().size();

            for (MultipartFile file : newImages) {
                if (file.isEmpty()) continue;

                try {
                    // (1) 파일 타입 확인
                    MediaType mediaType = MediaType.IMAGE;
                    String contentType = file.getContentType();
                    if (contentType != null && contentType.startsWith("video")) {
                        mediaType = MediaType.VIDEO;
                    }

                    // (2) AI 분석
                    Double riskScore = 0.0;
                    RiskLevel riskLevel = RiskLevel.LOW;

                    // [★추가 1] AI 감지 결과(좌표)를 저장할 변수
                    String detectionJson = null;

                    if (mediaType == MediaType.IMAGE) {
                        try {
                            // ★ 수정할 때도 AI한테 물어봅니다.
                            AiResponseDTO aiResult = aiService.analyzeImage(file);

                            if (aiResult != null && aiResult.getRisk() != null) {
                                riskScore = (double) aiResult.getRisk().getRisk_score();
                                try {
                                    riskLevel = RiskLevel.valueOf(aiResult.getRisk().getRisk_level());
                                } catch (IllegalArgumentException e) {
                                    riskLevel = RiskLevel.LOW;
                                }

                                // [★추가 2] JSON 변환 로직 (createPost와 동일하게 적용)
                                if (aiResult.getDetections() != null) {
                                    detectionJson = objectMapper.writeValueAsString(aiResult.getDetections());
                                }
                            }
                        } catch (Exception e) {
                            log.error("수정 중 AI 분석 실패 (저장 계속 진행): {}", e.getMessage());
                        }
                    }

                    // (3) 파일 저장
                    UploadFileDTO uploadFile = fileStore.storeFile(file);

                    // (4) DB 추가
                    PostMedia postMedia = PostMedia.builder()
                            .post(post)
                            .mediaType(mediaType)
                            .fileUrl(uploadFile.getFileUrl())
                            .storedFileName(uploadFile.getStoredFileName())
                            .fileSizeBytes(file.getSize())
                            .fileOrder(++currentMediaCount)
                            .analyzedAt(LocalDateTime.now())
                            .isActive(true)
                            .riskScore(riskScore)
                            .riskLevel(riskLevel)

                            // [★추가 3] JSON 데이터 저장
                            .detectionInfo(detectionJson)
                            .build();

                    post.getMedias().add(postMedia);

                } catch (IOException e) {
                    throw new RuntimeException("파일 추가 중 오류 발생", e);
                }
            }
        }

        // =================================================
        // 4. [추가됨] 게시글 전체 평균 점수 & 등급 재계산
        // (사진을 지우거나 새로 추가했으니, 평균 점수가 바뀌어야 함!)
        // =================================================
        double totalScore = 0.0;
        int count = 0;
        RiskLevel highestLevel = RiskLevel.LOW;

        for (PostMedia media : post.getMedias()) {
            if (media.getRiskScore() != null) {
                totalScore += media.getRiskScore();
                count++;
            }
            if (media.getRiskLevel() != null && media.getRiskLevel().ordinal() > highestLevel.ordinal()) {
                highestLevel = media.getRiskLevel();
            }
        }

        if (count > 0) {
            post.updateRiskInfo(totalScore / count, highestLevel);
        } else {
            post.updateRiskInfo(0.0, RiskLevel.LOW); // 사진 다 지웠으면 0점
        }

        // 5. 텍스트 내용 수정
        post.setTitle(updateDTO.getTitle());
        post.setContent(updateDTO.getContent());
    }

    /**
     * [게시글 삭제 (Hard Delete)]
     * =================================================================================
     * | 기능 | 게시글과 연관된 모든 파일 및 DB 데이터를 영구 삭제함 |
     * | 입력 | memberId, postId |
     * | 출력 | 없음 (void) |
     * =================================================================================
     * [로직 설명]
     * 1. 권한 확인: 본인 글인지 확인합니다.
     * 2. 파일 삭제: 게시글에 포함된 모든 이미지 파일을 디스크에서 삭제합니다.
     * 3. DB 삭제: postRepository.delete()를 호출합니다.
     * - CascadeType.ALL 설정으로 인해 게시글을 지우면 이미지 데이터(PostMedia)도 자동 삭제됩니다.
     */
    @Override
    @Transactional
    public void deletePost(Long memberId, Long postId) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // [★수정됨] 삭제를 요청한 사람의 정보(Role)를 확인하기 위해 조회
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        // 2. 권한 확인 로직 변경
        // "작성자가 아니고" AND "관리자가 아니라면" -> 에러 발생
        boolean isWriter = post.getMember().getId().equals(memberId);
        boolean isAdmin = requester.getRole() == MemberRole.ADMIN;

        if (!isWriter && !isAdmin) {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }

        // 3. [파일 삭제] 하드디스크에서 이미지 파일들 먼저 삭제
        for (PostMedia media : post.getMedias()) {
            if (media.getStoredFileName() != null) {
                fileStore.deleteFile(media.getStoredFileName());
            }
        }

        // 4. [DB 삭제]
        postRepository.delete(post);
        log.info("게시글 삭제 완료 (요청자: {}, 게시글: {})", memberId, postId);
    }

    /**
     * [추가됨] 통계 조회 구현
     * 리포지토리의 커스텀 쿼리(GROUP BY)를 실행해서 결과를 가져옵니다.
     */
    @Override
    public List<StatisticsResponseDTO> getRiskStatistics() {
        return postRepository.findRiskLevelStatistics();
    }

    /**
     * [메인 홈 화면용 비교 데이터 조회]
     * 가장 위험한 사진 1장 vs 가장 안전한 사진 1장을 찾아서 반환합니다.
     */
    @Override
    public HomeComparisonDTO getHomeComparisonData() {
        // 1. 페이징 조건 생성 (딱 1개만 가져오기)
        Pageable limitOne = PageRequest.of(0, 1);

        // 2. 가장 위험한 사진 찾기
        List<PostMedia> dangerList = postMediaRepository.findMostDangerous(limitOne);
        PostMedia dangerous = dangerList.isEmpty() ? null : dangerList.get(0);

        // 3. 가장 안전한 사진 찾기
        List<PostMedia> safeList = postMediaRepository.findSafest(limitOne);
        PostMedia safe = safeList.isEmpty() ? null : safeList.get(0);

        // 4. 결과 DTO 에 담기 (사진이 없을 경우 null 처리)
        return HomeComparisonDTO.builder()
                .dangerousImage(dangerous != null ? HomeComparisonDTO.ImageSummary.from(dangerous) : null)
                .safeImage(safe != null ? HomeComparisonDTO.ImageSummary.from(safe) : null)
                .build();
    }
}