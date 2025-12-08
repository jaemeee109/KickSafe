package com.kicksafe.global.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true) // 모든 boolean 필드에 자동으로 적용되지 않도록 설정 (필요한 곳에만 @Convert 붙여서 사용)
public class BooleanToYNConverter implements AttributeConverter<Boolean, String> {

    // 1. 자바(Boolean) -> DB(String 'Y'/'N') 변환
    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        // null 이거나 true 면 "Y", false 면 "N"
        return (attribute != null && attribute) ? "Y" : "N";
    }

    // 2. DB(String 'Y'/'N') -> 자바(Boolean) 변환
    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        // "Y"면 true, 아니면 false
        return "Y".equalsIgnoreCase(dbData);
    }
}

// 자바와 데이터베이스(DB) 사이의 "통역사" 역할을 하기 위해 만든 파일

