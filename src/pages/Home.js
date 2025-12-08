import React from 'react';
import { Link } from 'react-router-dom';

const Home = () => {
  // 현재 로그인 상태인지 확인 (토큰 유무 체크)
  const isLogin = !!localStorage.getItem('accessToken');

  return (
    <div style={{ textAlign: 'center', marginTop: '100px' }}>
      <h1 style={{ fontSize: '3rem', marginBottom: '10px' }}>🚔 KickSafe</h1>
      <p style={{ color: '#666', fontSize: '1.2rem', marginBottom: '40px' }}>
        안전한 킥보드 문화를 위한 신고 및 관리 시스템
      </p>

      <div>
        {isLogin ? (
          // [로그인 O] -> 바로 신고 작성 페이지로 이동
          <Link to="/post/create">
            <button style={{
              padding: '15px 40px',
              fontSize: '20px',
              backgroundColor: '#ff4d4f', // 빨간색 (강조)
              color: 'white',
              border: 'none',
              borderRadius: '10px',
              cursor: 'pointer',
              fontWeight: 'bold',
              boxShadow: '0 4px 15px rgba(255, 77, 79, 0.4)',
              transition: 'transform 0.2s'
            }}>
              🚨 신고하러 가기
            </button>
          </Link>
        ) : (
          // [로그인 X] -> 로그인 페이지로 이동
          <Link to="/login">
            <button style={{
              padding: '15px 40px',
              fontSize: '18px',
              backgroundColor: '#1890ff', // 파란색
              color: 'white',
              border: 'none',
              borderRadius: '10px',
              cursor: 'pointer',
              fontWeight: 'bold',
              boxShadow: '0 4px 15px rgba(24, 144, 255, 0.3)'
            }}>
              로그인 하러 가기
            </button>
          </Link>
        )}
      </div>
    </div>
  );
};

export default Home;