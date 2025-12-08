// 
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig'; // 우리가 만든 백엔드 통신 도구

const Header = () => {
  const navigate = useNavigate();
  
  // 1. 현재 브라우저에 토큰이 있는지 확인 (로그인 여부 체크)
  const isLogin = !!localStorage.getItem('accessToken');

  // 2. 로그아웃 버튼을 눌렀을 때 실행될 함수
  const handleLogout = async () => {
    try {
      // (1) 백엔드에 로그아웃 요청
      await api.post('/auth/logout'); 
    } catch (error) {
      console.error("로그아웃 처리 중 에러 발생 (무시하고 진행)");
    } finally {
      // (2) 프론트엔드에서 토큰 삭제
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      
      alert("로그아웃 되었습니다.");
      
      // (3) 메인 화면으로 이동하면서 새로고침
      navigate('/');
      window.location.reload(); 
    }
  };

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '10px 20px',
      backgroundColor: '#f8f9fa',
      borderBottom: '1px solid #ddd'
    }}>
      {/* 왼쪽: 로고 (누르면 홈으로) */}
      <Link to="/" style={{ textDecoration: 'none', color: 'black', fontWeight: 'bold', fontSize: '20px' }}>
        🚔 KickSafe
      </Link>

      {/* 오른쪽: 메뉴 버튼들 */}
      <div>
        {/* [추가됨] 게시판 바로가기 버튼 (항상 보임) */}
        <Link to="/post/list" style={{ marginRight: '15px', textDecoration: 'none', color: '#333', fontWeight: 'bold' }}>
          게시판
        </Link>

        {isLogin ? (
          <>
            {/* [수정] 내 정보 버튼을 Link로 감싸거나 onClick으로 이동 */}
            <Link to="/mypage">
              <button style={{ marginRight: '10px', cursor: 'pointer' }}>내 정보</button>
            </Link>
            
            <button onClick={handleLogout} style={{ cursor: 'pointer' }}>로그아웃</button>
          </>
        ) : (
          // 로그인 안 했을 때 보여줄 버튼
          <Link to="/login">
            <button>로그인</button>
          </Link>
        )}
      </div>
    </div>
  );
};

export default Header;