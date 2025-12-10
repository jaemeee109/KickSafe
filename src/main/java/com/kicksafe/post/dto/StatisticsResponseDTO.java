package com.kicksafe.post.dto;

import com.kicksafe.post.constant.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 통계 결과를 담아서 나를 택배 상자
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsResponseDTO {
    private RiskLevel riskLevel; // 위험 등급 (LOW, HIGH...)
    private Long count;       // 개수 (몇 개?)
}