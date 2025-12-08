// 
import React, { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig'; // API 호출을 위해 추가

const OAuthCallback = () => {
  const navigate = useNavigate();
  const isProcessed = useRef(false);

  useEffect(() => {
    if (isProcessed.current) return;

    const searchParams = new URLSearchParams(window.location.search);
    const accessToken = searchParams.get("accessToken");
    const refreshToken = searchParams.get("refreshToken");
    const error = searchParams.get("error");

    const processLogin = async () => {
      if (accessToken && refreshToken) {
        isProcessed.current = true;
        
        localStorage.setItem("accessToken", accessToken);
        localStorage.setItem("refreshToken", refreshToken);
        window.history.replaceState({}, null, "/");

        try {
          // [추가] 로그인 후 내 정보(권한) 확인하기
          const response = await api.get('/members/readOne');
          const role = response.data.role; // "GUEST" or "USER"

          if (role === 'GUEST') {
            // 손님(첫방문)이면 -> 닉네임 변경 페이지로 납치
            alert("환영합니다! 원활한 활동을 위해 닉네임을 설정해주세요.");
            navigate("/member/update");
          } else {
            // 정회원이면 -> 게시판으로 직행
            alert("로그인되었습니다.");
            navigate("/post/list");
          }

        } catch (err) {
          console.error("회원정보 조회 실패:", err);
          navigate("/"); // 에러 나면 그냥 홈으로
        }

      } else if (error) {
        isProcessed.current = true;
        alert("로그인 실패: " + error);
        navigate("/login");
      } else {
        navigate("/");
      }
    };

    processLogin();
  }, [navigate]);

  return (
    <div style={{ textAlign: 'center', marginTop: '200px' }}>
      <h2>로그인 처리 중입니다...</h2>
    </div>
  );
};

export default OAuthCallback;