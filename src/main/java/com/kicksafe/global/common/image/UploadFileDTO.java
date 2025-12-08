package com.kicksafe.global.common.image;

import lombok.AllArgsConstructor;
import lombok.Data;

// 업로드된 파일의 정보(원본 이름, 저장된 이름, URL)를 담아서 옮겨주는 택배 상자.
@Data
@AllArgsConstructor
public class UploadFileDTO {

    private String uploadFileName;  // 고객이 올린 원래 파일명 (예: cat.jpg)
    private String storedFileName;   // 서버에 저장된 안 겹치는 파일명 (예: uuid_cat.jpg)
    private String fileUrl;         // 프론트에서 접근할 전체 URL
}
