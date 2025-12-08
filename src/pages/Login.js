import React, { useState } from 'react';

const Login = () => {
  // 백엔드 주소
  const BACKEND_URL = "http://localhost:8080";

  // 호버 효과를 위한 상태 관리 (선택 사항: 더 디테일한 인터랙션을 위해 추가)
  const [googleHover, setGoogleHover] = useState(false);

  return (
    <div style={{ 
      display: 'flex', 
      flexDirection: 'column', 
      alignItems: 'center', 
      justifyContent: 'center', 
      height: '80vh',
      backgroundColor: '#f8f9fa' // 전체 배경을 아주 연한 회색으로 주면 버튼이 더 돋보입니다.
    }}>
      <h1 style={{ fontSize: '3rem', marginBottom: '10px', color: '#333' }}>🚔 KickSafe</h1>
      <p style={{ color: '#666', marginBottom: '40px' }}>간편하게 로그인하고 안전신고를 시작하세요!</p>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '15px', width: '100%', maxWidth: '320px' }}>
        
        {/* 1. [수정] 구글 로그인 (공식 스타일 적용) */}
        <a href={`${BACKEND_URL}/oauth2/authorization/google`} style={{ textDecoration: 'none' }}>
          <button 
            onMouseEnter={() => setGoogleHover(true)}
            onMouseLeave={() => setGoogleHover(false)}
            style={{
              width: '100%', 
              padding: '12px', // 패딩을 살짝 줄여서 세련되게
              backgroundColor: googleHover ? '#f8f9fa' : 'white', // 호버 시 아주 연한 회색 배경
              color: '#3c4043', // 구글 공식 텍스트 컬러 (진한 회색)
              border: '1px solid #dadce0', // 구글 공식 테두리 컬러 (연한 회색)
              borderRadius: '4px', // 둥근 모서리를 살짝 줄임 (구글 스타일)
              fontSize: '16px', 
              fontWeight: '500', // 너무 두껍지 않은 적당한 굵기
              cursor: 'pointer',
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center', 
              gap: '12px',
              boxShadow: googleHover ? '0 1px 3px rgba(60,64,67,0.3)' : '0 1px 2px rgba(60,64,67,0.3)', // 미세한 그림자 추가
              transition: 'all 0.2s ease-in-out' // 부드러운 전환 효과
          }}>
            {/* [중요] 공식 컬러 구글 'G' 로고 SVG */}
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="24px" height="24px">
              <path fill="#FFC107" d="M43.611,20.083H42V20H24v8h11.303c-1.649,4.657-6.08,8-11.303,8c-6.627,0-12-5.373-12-12c0-6.627,5.373-12,12-12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C12.955,4,4,12.955,4,24c0,11.045,8.955,20,20,20c11.045,0,20-8.955,20-20C44,22.659,43.862,21.35,43.611,20.083z"/>
              <path fill="#FF3D00" d="M6.306,14.691l6.571,4.819C14.655,15.108,18.961,12,24,12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C16.318,4,9.656,8.337,6.306,14.691z"/>
              <path fill="#4CAF50" d="M24,44c5.166,0,9.86-1.977,13.409-5.192l-6.19-5.238C29.211,35.091,26.715,36,24,36c-5.202,0-9.619-3.317-11.283-7.946l-6.522,5.025C9.505,39.556,16.227,44,24,44z"/>
              <path fill="#1976D2" d="M43.611,20.083H42V20H24v8h11.303c-0.792,2.237-2.231,4.166-4.087,5.571c0.001-0.001,0.002-0.001,0.003-0.002l6.19,5.238C36.971,39.205,44,34,44,24C44,22.659,43.862,21.35,43.611,20.083z"/>
            </svg>
            Google 계정으로 로그인
          </button>
        </a>

        {/* 2. 카카오 로그인 (기존 유지) */}
        <a href={`${BACKEND_URL}/oauth2/authorization/kakao`} style={{ textDecoration: 'none' }}>
          <button style={{
            width: '100%', padding: '15px',
            backgroundColor: '#FEE500', color: '#3c1e1e',
            border: 'none', borderRadius: '5px',
            fontWeight: 'bold', fontSize: '16px', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px'
          }}>
            <span style={{ fontSize: '18px' }}>💬</span> 카카오로 시작하기
          </button>
        </a>

        {/* 3. 네이버 로그인 (기존 유지) */}
        <a href={`${BACKEND_URL}/oauth2/authorization/naver`} style={{ textDecoration: 'none' }}>
          <button style={{
            width: '100%', padding: '15px',
            backgroundColor: '#03C75A', color: 'white',
            border: 'none', borderRadius: '5px',
            fontWeight: 'bold', fontSize: '16px', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px'
          }}>
            <span style={{ fontWeight: '900', fontSize: '18px' }}>N</span> 네이버로 시작하기
          </button>
        </a>

        {/* 4. [추가] 인스타그램 로그인 (미래 대비용) */}
        {/* 아직 인스타 개발자 설정을 안 했다면 눌러도 에러가 나겠지만 버튼은 미리 만들어둡니다. */}
        <a href={`${BACKEND_URL}/oauth2/authorization/instagram`} style={{ textDecoration: 'none' }}>
          <button style={{
            width: '100%', padding: '15px',
            // 인스타그램 그라데이션 배경
            background: 'linear-gradient(45deg, #f09433 0%, #e6683c 25%, #dc2743 50%, #cc2366 75%, #bc1888 100%)', 
            color: 'white',
            border: 'none', borderRadius: '5px',
            fontWeight: 'bold', fontSize: '16px', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px'
          }}>
            <span style={{ fontSize: '20px' }}>📷</span> 인스타그램 로그인
          </button>
        </a>

      </div>
    </div>
  );
};

export default Login;