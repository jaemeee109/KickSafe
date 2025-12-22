/*
 * ==================================================================================
 * [React 페이지: 로그인 (Login.js)]
 * ----------------------------------------------------------------------------------
 * 수정 내용 : 
 * 1. 로그인 실패(정지 등) 시 에러 메시지(Alert) 출력 후
 * 2. [NEW] 메인 화면('/')으로 즉시 이동하도록 변경
 * ==================================================================================
 */
import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

const Login = () => {
  const BACKEND_URL = "http://34.50.13.223.nip.io:8020";
  const [googleHover, setGoogleHover] = useState(false);
  
  const location = useLocation();
  const navigate = useNavigate();

  // [페이지 로드 시 실행] 주소창 에러 메시지 확인
  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const error = searchParams.get('error');

    if (error) {
      // 1. 경고창 띄우기 (사용자가 '확인' 누를 때까지 대기)
      alert("로그인 실패: " + error);

      // 2. [수정됨] 경고창 닫으면 -> 메인 화면('/')으로 이동
      // replace: true 옵션은 "뒤로 가기" 눌렀을 때 다시 여기로 못 오게 기록을 덮어쓰는 것
      navigate('/', { replace: true });
    }
  }, [location, navigate]);

  return (
    <div style={{ 
      display: 'flex', 
      flexDirection: 'column', 
      alignItems: 'center', 
      justifyContent: 'center', 
      height: '80vh',
      backgroundColor: '#f8f9fa' 
    }}>
      <h1 style={{ fontSize: '3rem', marginBottom: '10px', color: '#333' }}>🚔 KickSafe</h1>
      <p style={{ color: '#666', marginBottom: '40px' }}>간편하게 로그인하고 안전신고를 시작하세요!</p>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '15px', width: '100%', maxWidth: '320px' }}>
        
        {/* 구글 로그인 */}
        <a href={`${BACKEND_URL}/oauth2/authorization/google`} style={{ textDecoration: 'none' }}>
          <button 
            onMouseEnter={() => setGoogleHover(true)}
            onMouseLeave={() => setGoogleHover(false)}
            style={{
              width: '100%', 
              padding: '12px', 
              backgroundColor: googleHover ? '#f8f9fa' : 'white', 
              color: '#3c4043', 
              border: '1px solid #dadce0', 
              borderRadius: '4px', 
              fontSize: '16px', 
              fontWeight: '500', 
              cursor: 'pointer',
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center', 
              gap: '12px',
              boxShadow: googleHover ? '0 1px 3px rgba(60,64,67,0.3)' : '0 1px 2px rgba(60,64,67,0.3)',
              transition: 'all 0.2s ease-in-out'
          }}>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="24px" height="24px">
              <path fill="#FFC107" d="M43.611,20.083H42V20H24v8h11.303c-1.649,4.657-6.08,8-11.303,8c-6.627,0-12-5.373-12-12c0-6.627,5.373-12,12-12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C12.955,4,4,12.955,4,24c0,11.045,8.955,20,20,20c11.045,0,20-8.955,20-20C44,22.659,43.862,21.35,43.611,20.083z"/>
              <path fill="#FF3D00" d="M6.306,14.691l6.571,4.819C14.655,15.108,18.961,12,24,12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C16.318,4,9.656,8.337,6.306,14.691z"/>
              <path fill="#4CAF50" d="M24,44c5.166,0,9.86-1.977,13.409-5.192l-6.19-5.238C29.211,35.091,26.715,36,24,36c-5.202,0-9.619-3.317-11.283-7.946l-6.522,5.025C9.505,39.556,16.227,44,24,44z"/>
              <path fill="#1976D2" d="M43.611,20.083H42V20H24v8h11.303c-0.792,2.237-2.231,4.166-4.087,5.571c0.001-0.001,0.002-0.001,0.003-0.002l6.19,5.238C36.971,39.205,44,34,44,24C44,22.659,43.862,21.35,43.611,20.083z"/>
            </svg>
            Google 계정으로 로그인
          </button>
        </a>

        {/* 카카오 로그인 */}
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

        {/* 네이버 로그인 */}
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

        {/* 인스타그램 로그인 */}
        <a href={`${BACKEND_URL}/oauth2/authorization/instagram`} style={{ textDecoration: 'none' }}>
          <button style={{
            width: '100%', padding: '15px',
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