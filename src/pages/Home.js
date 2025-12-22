import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';

const Home = () => {
  const isLogin = !!localStorage.getItem('accessToken');
  const navigate = useNavigate();

  // 비교 데이터 상태 관리
  const [comparisonData, setComparisonData] = useState(null);
  const [loading, setLoading] = useState(true);

  // 데이터 불러오기
  useEffect(() => {
    const fetchComparison = async () => {
      try {
        const response = await api.get('/posts/home/comparison');
        setComparisonData(response.data);
      } catch (error) {
        console.error("홈 데이터 로딩 실패:", error);
      } finally {
        setLoading(false);
      }
    };
    fetchComparison();
  }, []);

  // 이미지 클릭 시 상세 페이지로 이동
  const goToDetail = (postId) => {
    navigate(`/post/${postId}`);
  };

  return (
    <div style={{ textAlign: 'center', marginTop: '60px', paddingBottom: '50px' }}>
      
      {/* ======================================================== */}
      {/* 1. 메인 타이틀 (수정됨) */}
      {/* ======================================================== */}
      <div style={{ marginBottom: '40px', padding: '0 20px' }}>
        <h1 style={{ 
          fontSize: '2.5rem', 
          fontWeight: '800', 
          color: '#333',
          letterSpacing: '-1px',
          margin: 0,
          wordBreak: 'keep-all'
        }}>
          스마트한 감지로 안전한 라이딩 🛴
        </h1>
      </div>

      {/* ======================================================== */}
      {/* 2. Best vs Worst 사진 섹션 (중앙) */}
      {/* ======================================================== */}
      <div style={{ maxWidth: '900px', margin: '0 auto 20px auto', padding: '0 20px' }}>
        
        {loading ? (
          <div style={{ color: '#888', padding: '50px' }}>데이터 분석 중...</div>
        ) : (
          <div style={{ display: 'flex', justifyContent: 'center', gap: '40px', flexWrap: 'wrap' }}>
            
            {/* (1) 가장 위험한 이미지 카드 */}
            {comparisonData?.dangerousImage ? (
              <div 
                onClick={() => goToDetail(comparisonData.dangerousImage.postId)}
                style={{ 
                  border: '2px solid #ff4d4f', borderRadius: '15px', overflow: 'hidden', 
                  width: '320px', cursor: 'pointer', boxShadow: '0 10px 25px rgba(255, 77, 79, 0.15)',
                  transition: 'transform 0.2s', backgroundColor: 'white'
                }}
                onMouseOver={(e) => e.currentTarget.style.transform = 'translateY(-5px)'}
                onMouseOut={(e) => e.currentTarget.style.transform = 'translateY(0)'}
              >
                <div style={{ backgroundColor: '#ff4d4f', color: 'white', padding: '12px', fontWeight: 'bold', fontSize: '18px' }}>
                  🚫 WORST: 가장 위험한 순간
                </div>
                <div style={{ height: '220px', overflow: 'hidden', backgroundColor: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <img 
                    src={`http://34.50.13.223.nip.io:8020${comparisonData.dangerousImage.imageUrl}`} 
                    alt="Dangerous" 
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                </div>
                <div style={{ padding: '20px' }}>
                  <p style={{ margin: '0 0 5px 0', fontSize: '20px', fontWeight: '800', color: '#ff4d4f' }}>
                    위험도 {Math.round(comparisonData.dangerousImage.riskScore)}점
                  </p>
                  <span style={{ 
                    display: 'inline-block', padding: '4px 12px', borderRadius: '20px', 
                    fontSize: '13px', fontWeight: 'bold', backgroundColor: '#fff1f0', color: '#cf1322' 
                  }}>
                    {comparisonData.dangerousImage.riskLevel} 등급
                  </span>
                </div>
              </div>
            ) : (
              <div style={{ width: '320px', padding: '80px 0', border: '2px dashed #eee', borderRadius: '15px', color: '#aaa' }}>
                분석된 위험 데이터가 없습니다.
              </div>
            )}

            {/* (2) 가장 안전한 이미지 카드 */}
            {comparisonData?.safeImage ? (
              <div 
                onClick={() => goToDetail(comparisonData.safeImage.postId)}
                style={{ 
                  border: '2px solid #52c41a', borderRadius: '15px', overflow: 'hidden', 
                  width: '320px', cursor: 'pointer', boxShadow: '0 10px 25px rgba(82, 196, 26, 0.15)',
                  transition: 'transform 0.2s', backgroundColor: 'white'
                }}
                onMouseOver={(e) => e.currentTarget.style.transform = 'translateY(-5px)'}
                onMouseOut={(e) => e.currentTarget.style.transform = 'translateY(0)'}
              >
                <div style={{ backgroundColor: '#52c41a', color: 'white', padding: '12px', fontWeight: 'bold', fontSize: '18px' }}>
                  🛡️ BEST: 가장 안전한 순간
                </div>
                <div style={{ height: '220px', overflow: 'hidden', backgroundColor: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <img 
                    src={`http://34.50.13.223.nip.io:8020${comparisonData.safeImage.imageUrl}`} 
                    alt="Safe" 
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                </div>
                <div style={{ padding: '20px' }}>
                  <p style={{ margin: '0 0 5px 0', fontSize: '20px', fontWeight: '800', color: '#52c41a' }}>
                    위험도 {Math.round(comparisonData.safeImage.riskScore)}점
                  </p>
                  <span style={{ 
                    display: 'inline-block', padding: '4px 12px', borderRadius: '20px', 
                    fontSize: '13px', fontWeight: 'bold', backgroundColor: '#f6ffed', color: '#389e0d' 
                  }}>
                    {comparisonData.safeImage.riskLevel} 등급
                  </span>
                </div>
              </div>
            ) : (
              <div style={{ width: '320px', padding: '80px 0', border: '2px dashed #eee', borderRadius: '15px', color: '#aaa' }}>
                분석된 안전 데이터가 없습니다.
              </div>
            )}

          </div>
        )}
      </div>

      {/* ======================================================== */}
      {/* 3. 설명 문구 (사진 바로 아래) */}
      {/* ======================================================== */}
      <div style={{ marginBottom: '50px' }}>
        <p style={{ 
          fontSize: '1.2rem', 
          color: '#666', 
          fontWeight: '500',
          margin: 0
        }}>
          📊 <span style={{ color: '#1890ff', fontWeight: 'bold' }}>KickSafe Insight</span> : AI가 분석한 가장 위험한 순간 vs 가장 안전한 순간
        </p>
      </div>

      {/* ======================================================== */}
      {/* 4. 로그인/신고하기 버튼 (가장 아래) */}
      {/* ======================================================== */}
      <div>
        {isLogin ? (
          <Link to="/post/create">
            <button style={{
              padding: '18px 50px',
              fontSize: '20px',
              backgroundColor: '#ff4d4f',
              color: 'white',
              border: 'none',
              borderRadius: '50px',
              cursor: 'pointer',
              fontWeight: '800',
              boxShadow: '0 4px 15px rgba(255, 77, 79, 0.4)',
              transition: 'all 0.2s ease-in-out'
            }}
            onMouseOver={(e) => { e.currentTarget.style.transform = 'scale(1.05)'; e.currentTarget.style.boxShadow = '0 6px 20px rgba(255, 77, 79, 0.6)'; }}
            onMouseOut={(e) => { e.currentTarget.style.transform = 'scale(1)'; e.currentTarget.style.boxShadow = '0 4px 15px rgba(255, 77, 79, 0.4)'; }}
            >
              🚨 위험 신고하기
            </button>
          </Link>
        ) : (
          <Link to="/login">
            <button style={{
              padding: '18px 50px',
              fontSize: '18px',
              backgroundColor: '#1890ff',
              color: 'white',
              border: 'none',
              borderRadius: '50px',
              cursor: 'pointer',
              fontWeight: '800',
              boxShadow: '0 4px 15px rgba(24, 144, 255, 0.3)',
              transition: 'all 0.2s ease-in-out'
            }}
            onMouseOver={(e) => { e.currentTarget.style.transform = 'scale(1.05)'; e.currentTarget.style.boxShadow = '0 6px 20px rgba(24, 144, 255, 0.5)'; }}
            onMouseOut={(e) => { e.currentTarget.style.transform = 'scale(1)'; e.currentTarget.style.boxShadow = '0 4px 15px rgba(24, 144, 255, 0.3)'; }}
            >
              로그인하고 시작하기
            </button>
          </Link>
        )}
      </div>

    </div>
  );
};

export default Home;