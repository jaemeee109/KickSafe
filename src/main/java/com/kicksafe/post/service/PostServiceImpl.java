package com.kicksafe.post.service;

import com.kicksafe.global.common.image.FileStore;
import com.kicksafe.global.common.image.UploadFileDTO;
import com.kicksafe.global.common.paging.PageRequestDTO;
import com.kicksafe.global.common.paging.PageResponseDTO;
import com.kicksafe.member.domain.Member;
import com.kicksafe.member.repository.MemberRepository;
import com.kicksafe.post.constant.MediaType;
import com.kicksafe.post.domain.Post;
import com.kicksafe.post.domain.PostMedia;
import com.kicksafe.post.dto.PostCreateRequestDTO;
import com.kicksafe.post.dto.PostDetailResponseDTO;
import com.kicksafe.post.dto.PostResponseDTO;
import com.kicksafe.post.dto.PostUpdateRequestDTO;
import com.kicksafe.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    @Transactional // 쓰기 작업이므로 Transactional 필수
    public Long createPost(Long memberId, PostCreateRequestDTO requestDTO) {

        // 1. 작성자 찾기 (없으면 에러)
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 2. 게시글 엔티티 생성 (아직 저장 안 함)
        Post post = Post.builder()
                .member(member)             // 작성자 연결
                .title(requestDTO.getTitle())
                .content(requestDTO.getContent())
                .isDeleted(false)           // 삭제 안 됨
                .build();

        // 3. 파일 처리 (이미지가 있는 경우에만)
        List<MultipartFile> files = requestDTO.getImages();

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                try {
                    // (1) 실제 폴더에 파일 저장
                    UploadFileDTO uploadFile = fileStore.storeFile(file);

                    // [수정] 파일 타입 자동 감지 (이미지 vs 비디오)
                    MediaType mediaType = MediaType.IMAGE; // 기본값
                    String contentType = file.getContentType(); // 예: "video/mp4", "image/jpeg"

                    if (contentType != null && contentType.startsWith("video")) {
                        mediaType = MediaType.VIDEO;
                    }

                    // (2) DB에 저장할 미디어 정보 생성
                    PostMedia postMedia = PostMedia.builder()
                            .post(post)
                            .mediaType(mediaType) // [수정] 감지된 타입 사용
                            .fileUrl(uploadFile.getFileUrl())
                            .storedFileName(uploadFile.getStoredFileName())
                            .fileSizeBytes(file.getSize())
                            .fileOrder(post.getMedias().size() + 1)
                            .isActive(true)
                            .build();

                    // (3) 추가
                    post.getMedias().add(postMedia);

                } catch (IOException e) {
                    throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
                }
            }
        }

        // 4. DB에 저장 (Post 와 PostMedia 가 한 번에 저장됨)
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

        // 1. 정렬 기준: id 내림차순 (최신순)
        Pageable pageable = pageRequestDTO.getPageable("id");

        // 2. DB 조회
        Page<Post> result = postRepository.findAll(pageable);

        // 3. 변환 (Post -> PostResponseDTO)
        List<PostResponseDTO> dtoList = result.getContent().stream()
                .map(PostResponseDTO::from)
                .collect(Collectors.toList());

        // 4. 결과 반환
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
        // 2. 이미지 완전 삭제 (DB + 파일)
        // =================================================
        List<Long> deletedMediaIds = updateDTO.getDeletedMediaIds();

        if (deletedMediaIds != null && !deletedMediaIds.isEmpty()) {
            // (1) 지워야 할 이미지 객체들을 먼저 찾습니다.
            List<PostMedia> mediasToDelete = post.getMedias().stream()
                    .filter(media -> deletedMediaIds.contains(media.getId()))
                    .toList();

            // (2) 찾은 이미지들을 하나씩 처리합니다.
            for (PostMedia media : mediasToDelete) {
                // [파일 삭제] 하드디스크에서 파일 지우기
                if (media.getStoredFileName() != null) {
                    fileStore.deleteFile(media.getStoredFileName());
                }
            }

            // (3) [DB 삭제] 게시글의 이미지 리스트에서 제거해버립니다.
            // Post 엔티티에 'orphanRemoval = true' 가 걸려 있어서,
            // 리스트에서 빼는 순간 DB 에서도 DELETE 쿼리가 날아가서 사라집니다.
            post.getMedias().removeAll(mediasToDelete);
        }

        // =================================================
        // 3. 새 이미지 추가 (기존 로직 동일)
        // =================================================
        List<MultipartFile> newImages = updateDTO.getNewImages();
        if (newImages != null && !newImages.isEmpty()) {
            int currentMediaCount = post.getMedias().size();

            for (MultipartFile file : newImages) {
                if (file.isEmpty()) continue;
                try {
                    UploadFileDTO uploadFile = fileStore.storeFile(file);

                    // [수정] 파일 타입 자동 감지
                    MediaType mediaType = MediaType.IMAGE;
                    String contentType = file.getContentType();

                    if (contentType != null && contentType.startsWith("video")) {
                        mediaType = MediaType.VIDEO;
                    }

                    PostMedia postMedia = PostMedia.builder()
                            .post(post)
                            .mediaType(mediaType) // [수정] 감지된 타입 사용
                            .fileUrl(uploadFile.getFileUrl())
                            .storedFileName(uploadFile.getStoredFileName())
                            .fileSizeBytes(file.getSize())
                            .fileOrder(++currentMediaCount)
                            .isActive(true)
                            .build();
                    post.getMedias().add(postMedia);
                } catch (IOException e) {
                    throw new RuntimeException("파일 추가 중 오류 발생", e);
                }
            }
        }

        // 4. 내용 수정
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

        // 2. 본인 확인
        if (!post.getMember().getId().equals(memberId)) {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }

        // 3. [파일 삭제] 하드디스크에서 이미지 파일들 먼저 삭제
        // (DB 에서 지워지기 전에 파일명을 조회해서 지워야 함)
        for (PostMedia media : post.getMedias()) {
            if (media.getStoredFileName() != null) {
                fileStore.deleteFile(media.getStoredFileName());
            }
        }

        // 4. [DB 삭제] 게시글 삭제 (연관된 이미지 데이터도 Cascade 로 자동 삭제됨)
        postRepository.delete(post);
    }
}