/*
 * ==================================================================================
 * [React 페이지: 통계 대시보드 (Statistics.js)]
 * ----------------------------------------------------------------------------------
 * 목적      : 백엔드 API(/posts/statistics)를 호출하여 위험 등급별 신고 현황 시각화
 * 특징      : 라이브러리 없이 CSS만으로 막대 그래프 구현 (가볍고 빠름)
 * ==================================================================================
 */
import React, { useEffect, useState } from 'react';
import api from '../api/axiosConfig';

const Statistics = () => {
  const [stats, setStats] = useState([]);
  const [loading, setLoading] = useState(true);

  // 등급별 한글 명칭과 색상 설정
  const riskConfig = {
    LOW: { label: '안전 (LOW)', color: '#52c41a' },       // 초록
    MEDIUM: { label: '주의 (MEDIUM)', color: '#faad14' }, // 주황
    HIGH: { label: '위험 (HIGH)', color: '#ff4d4f' },     // 연한 빨강
    CRITICAL: { label: '심각 (CRITICAL)', color: '#ff0000' } // 진한 빨강
  };

  // 1. 통계 데이터 가져오기
  useEffect(() => {
    const fetchStatistics = async () => {
      try {
        const response = await api.get('/posts/statistics');
        setStats(response.data);
      } catch (error) {
        console.error("통계 로딩 실패:", error);
      } finally {
        setLoading(false);
      }
    };
    fetchStatistics();
  }, []);

  // 2. 총 신고 건수 계산 (비율 계산용)
  const totalCount = stats.reduce((sum, item) => sum + item.count, 0);

  if (loading) return <div style={{ textAlign: 'center', marginTop: '100px' }}>Loading...</div>;

  return (
    <div style={{ maxWidth: '800px', margin: '50px auto', padding: '20px' }}>
      {/* 헤더 타이틀 */}
      <h2 style={{ borderBottom: '2px solid #333', paddingBottom: '15px', marginBottom: '30px' }}>
        📊 안전신고 위험도 통계
      </h2>

      <div style={{ backgroundColor: 'white', padding: '30px', borderRadius: '10px', boxShadow: '0 2px 10px rgba(0,0,0,0.05)', border: '1px solid #eee' }}>
        <h3 style={{ textAlign: 'center', marginBottom: '40px', fontSize: '20px' }}>
          총 분석 이미지 수: <span style={{ color: '#1890ff', fontSize: '24px' }}>{totalCount}</span>건
        </h3>

        {totalCount === 0 ? (
          <p style={{ textAlign: 'center', color: '#888', padding: '20px' }}>아직 등록된 신고 데이터가 없습니다.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '25px' }}>
            {/* 위험 등급 순서대로 출력 (LOW -> MEDIUM -> HIGH -> CRITICAL) */}
            {Object.keys(riskConfig).map((level) => {
              // 해당 등급의 통계 데이터 찾기 (없으면 0개)
              const dataItem = stats.find(item => item.riskLevel === level);
              const count = dataItem ? dataItem.count : 0;
              // 비율 계산 (0으로 나누기 방지)
              const percentage = totalCount > 0 ? (count / totalCount) * 100 : 0;
              const config = riskConfig[level];

              return (
                <div key={level}>
                  {/* 라벨과 건수/비율 표시 */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontWeight: 'bold', fontSize: '15px' }}>
                    <span style={{ color: config.color }}>{config.label}</span>
                    <span style={{ color: '#555' }}>{count}건 ({percentage.toFixed(1)}%)</span>
                  </div>
                  
                  {/* 막대 그래프 배경 (회색 트랙) */}
                  <div style={{ width: '100%', backgroundColor: '#f5f5f5', borderRadius: '10px', height: '20px', overflow: 'hidden' }}>
                    {/* 실제 비율만큼 채워지는 막대 (컬러) */}
                    <div style={{
                      width: `${percentage}%`,
                      backgroundColor: config.color,
                      height: '100%',
                      transition: 'width 1s ease-in-out', // 애니메이션 효과
                      borderRadius: '10px'
                    }}></div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
      
      {/* 목록으로 돌아가기 버튼 */}
      <div style={{ textAlign: 'center', marginTop: '40px' }}>
        <a href="/post/list" style={{ textDecoration: 'none' }}>
           <button style={{ padding: '10px 20px', backgroundColor: '#555', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer' }}>
             목록으로 돌아가기
           </button>
        </a>
      </div>
    </div>
  );
};

export default Statistics;