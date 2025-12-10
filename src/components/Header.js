/*
 * ==================================================================================
 * [React 컴포넌트: 헤더 (Header.js)]
 * ----------------------------------------------------------------------------------
 * 수정 내용 : 
 * 1. 로그인 시 내 정보(닉네임, 권한) 불러오기
 * 2. 상단에 "관리자 / 홍길동님" 표시
 * 3. 관리자일 경우 '⚙️ 관리' 버튼 표시
 * ==================================================================================
 */
import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig'; // 백엔드 통신 도구

const Header = () => {
  const navigate = useNavigate();
  
  // 1. 로그인 여부 체크
  const isLogin = !!localStorage.getItem('accessToken');
  
  // 2. 내 정보(닉네임, 권한)를 저장할 상태
  const [myInfo, setMyInfo] = useState(null);

  // 3. [추가] 로그인 상태라면, 백엔드에서 내 정보를 가져옴
  useEffect(() => {
    const fetchMyInfo = async () => {
      if (!isLogin) return; // 로그인 안 했으면 스킵

      try {
        // [수정] 오타 제거 후 정상 코드
        const response = await api.get('/members/readOne'); // 내 정보 조회 API
        setMyInfo(response.data);
      } catch (error) {
        console.error("내 정보 로딩 실패 (토큰 만료 등):", error);
        // 에러 나면 조용히 로그아웃 처리하거나 무시
      }
    };

    fetchMyInfo();
  }, [isLogin]);

  // 4. 로그아웃 핸들러
  const handleLogout = async () => {
    try {
      await api.post('/auth/logout'); 
    } catch (error) {
      console.error("로그아웃 에러 (무시)");
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      alert("로그아웃 되었습니다.");
      navigate('/');
      window.location.reload(); 
    }
  };

  // 권한을 한글로 변환하는 헬퍼 함수
  const getRoleName = (role) => {
    if (role === 'ADMIN') return '관리자';
    if (role === 'USER') return '회원';
    return '게스트';
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
      {/* 로고 */}
      <Link to="/" style={{ textDecoration: 'none', color: 'black', fontWeight: 'bold', fontSize: '20px' }}>
        🚔 KickSafe
      </Link>

      {/* 오른쪽 메뉴 영역 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
        
        {/* [추가] 로그인 정보 표시 (로그인 했을 때만 보임) */}
        {isLogin && myInfo && (
          <span style={{ fontSize: '14px', marginRight: '10px', color: '#555' }}>
            {myInfo.role === 'ADMIN' ? '👮‍♂️' : '👤'} 
            <span style={{ fontWeight: 'bold', color: myInfo.role === 'ADMIN' ? '#ff4d4f' : '#333' }}>
              {' '}{getRoleName(myInfo.role)}
            </span>
            {' / '}
            <span style={{ fontWeight: 'bold' }}>{myInfo.nickname}</span>님
          </span>
        )}

        {/* 통계 버튼 */}
        <Link to="/statistics" style={{ textDecoration: 'none', color: '#333', fontWeight: 'bold' }}>
          📊 통계
        </Link>

        {/* 게시판 버튼 */}
        <Link to="/post/list" style={{ textDecoration: 'none', color: '#333', fontWeight: 'bold' }}>
          게시판
        </Link>

        {/* 관리자 전용 버튼 (ADMIN일 때만 보임) */}
        {isLogin && myInfo && myInfo.role === 'ADMIN' && (
           <Link to="/admin/members" style={{ textDecoration: 'none', color: '#d9363e', fontWeight: 'bold' }}>
             ⚙️ 관리
           </Link>
        )}

        {/* 로그인 상태에 따른 버튼 노출 */}
        {isLogin ? (
          <>
            <Link to="/mypage">
              <button style={{ cursor: 'pointer', padding: '5px 10px' }}>내 정보</button>
            </Link>
            
            <button onClick={handleLogout} style={{ cursor: 'pointer', padding: '5px 10px' }}>로그아웃</button>
          </>
        ) : (
          <Link to="/login">
            <button style={{ padding: '5px 10px', backgroundColor: '#1890ff', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
              로그인
            </button>
          </Link>
        )}
      </div>
    </div>
  );
};

export default Header;