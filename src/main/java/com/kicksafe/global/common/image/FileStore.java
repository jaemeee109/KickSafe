package com.kicksafe.global.common.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * [FileStore: 파일 저장소 관리자]
 * 사용자가 업로드한 파일(이미지, 영상 등)을 받아서
 * 실제 서버의 하드디스크(폴더)에 저장하거나 삭제하는 역할을 수행하는 '창고지기'
 */
@Component // 스프링에게 "이 클래스를 빈(Bean)으로 등록해서 관리해줘"라고 알림
public class FileStore {

    // application.yml 파일에 설정한 "file.dir" 값을 가져와서 변수에 넣음
    // 예: C:/kicksafe-files/
    @Value("${file.dir}")
    private String fileDir;

    /**
     * [전체 경로 만들기]
     * 파일 이름만 주면, 저장될 폴더 경로를 붙여서 전체 경로를 만들어줌
     * 예: 입력 "cat.jpg" -> 반환 "C:/kicksafe-files/cat.jpg"
     */
    public String getFullPath(String filename) {
        return fileDir + filename;
    }

    /**
     * [1. 여러 파일 저장하기]
     * 게시물에 사진이 여러 장일 때 사용함
     * 반복문을 돌면서 하나씩 storeFile() 메서드를 호출해 저장
     */
    public List<UploadFileDTO> storeFiles(List<MultipartFile> multipartFiles) throws IOException {
        List<UploadFileDTO> storeFileResult = new ArrayList<>();

        // 들어온 파일 리스트를 하나씩 꺼내서 저장 시도
        for (MultipartFile multipartFile : multipartFiles) {
            if (!multipartFile.isEmpty()) {
                // 하나 저장하고 그 결과(위치표)를 리스트에 추가
                storeFileResult.add(storeFile(multipartFile));
            }
        }
        return storeFileResult;
    }

    /**
     * [2. 파일 하나 저장하기 (핵심!)]
     * 사용자가 올린 파일을 서버만의 이름(UUID)으로 바꿔서 실제로 저장
     * @param multipartFile 사용자가 업로드한 파일 데이터 (스프링이 제공하는 객체)
     * @return UploadFileDTO (원래 이름, 저장된 이름, 웹에서 접근할 URL 을 담은 쪽지)
     */
    public UploadFileDTO storeFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) {
            return null; // 파일이 비어있으면 아무것도 안 함
        }

        // 1. 사용자가 올린 원래 파일명 가져오기 (예: "우리집강아지.png")
        String originalFilename = multipartFile.getOriginalFilename();

        // 2. 서버에 저장할 때 쓸 '유니크한 파일명' 만들기 (예: "5123-abc-123.png")
        // 왜? -> 서로 다른 사용자가 둘 다 "a.png"를 올리면 덮어씌워지니까, 이름이 안 겹치게 바꿔야 함!
        String storeFileName = createStoreFileName(originalFilename);

        // 3. [진짜 저장] 파일을 실제 경로(C:/...)에 전송(Transfer)해서 저장함
        // 이 한 줄이 실행되면 폴더에 파일이 짠! 하고 생깁니다.
        multipartFile.transferTo(new File(getFullPath(storeFileName)));

        // 4. 저장이 잘 끝났으면, 파일 정보를 담은 DTO(쪽지)를 만들어서 호출한 곳(Service)에 돌려줌
        // Service 는 이 정보를 받아서 DB에 저장하게 됩니다.
        return new UploadFileDTO(originalFilename, storeFileName, "/images/" + storeFileName);
    }

    /**
     * [3. 파일 삭제하기 (하드 딜리트)]
     * 게시물이 삭제되거나 수정될 때, 필요 없어진 파일을 하드디스크에서도 지움
     * * @param storeFileName 서버에 저장된 파일명 (예: "5123-abc-123.png")
     */
    public void deleteFile(String storeFileName) {
        // 지울 파일의 전체 경로를 찾음
        File file = new File(getFullPath(storeFileName));

        // 파일이 실제로 존재하면?
        if (file.exists()) {
            file.delete(); // 펑! 삭제 (복구 불가)
        }
    }

    /**
     * [내부 기술 1: 저장용 파일명 생성기]
     * "uuid" + "." + "확장자" 형태로 안 겹치는 이름을 만듦
     * 결과 예시: "b1c2-3d4e-5f6g.png"
     */
    private String createStoreFileName(String originalFilename) {
        // 1. 확장자 추출 (.png, .jpg 등)
        String ext = extractExt(originalFilename);

        // 2. UUID(전 세계에서 유일한 식별자) 생성
        String uuid = UUID.randomUUID().toString();

        // 3. 합치기
        return uuid + "." + ext;
    }

    /**
     * [내부 기술 2: 확장자 추출기]
     * 파일명 뒤에서부터 점(.)을 찾아서 그 뒤에 있는 글자(확장자)를 잘라냄
     * 예: "image.png" -> "png" 반환
     */
    private String extractExt(String originalFilename) {
        int pos = originalFilename.lastIndexOf("."); // 마지막 점(.)의 위치를 찾음
        return originalFilename.substring(pos + 1); // 점 다음 글자부터 끝까지 자름
    }
}

// MultipartFile 이 뭔가요?
//스프링에서 "업로드된 파일"을 다룰 때 쓰는 전용 포장지예요. 파일의 이름, 크기, 내용물(byte)을 다 가지고 있어요.

//왜 이름을 바꾸나요? (createStoreFileName)
//충돌 방지: 유저 A도 cat.jpg 를 올리고, 유저 B도 cat.jpg 를 올리면
// 파일이 덮어씌워져서 A의 사진이 사라져요.
// 그래서 **UUID(랜덤 문자열)**를 써서 세상에 하나뿐인 이름으로 바꿔서 저장하는 거예요.

//transferTo가 뭔가요?
//메모리에 둥둥 떠있던 파일 데이터를 진짜 하드디스크 파일로 옮겨 적는(저장하는) 명령어예요.
// 이 줄이 실행되어야 폴더에 파일 아이콘이 생겨요.