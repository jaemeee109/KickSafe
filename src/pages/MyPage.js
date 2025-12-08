import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';

const MyPage = () => {
  const navigate = useNavigate();
  const [member, setMember] = useState(null); // 내 정보 저장소
  const [isEditing, setIsEditing] = useState(false); // 수정 모드인지 확인
  const [newNickname, setNewNickname] = useState(''); // 변경할 닉네임 입력값

  // 1. 내 정보 불러오기
  useEffect(() => {
    const fetchMyInfo = async () => {
      try {
        const response = await api.get('/members/readOne');
        setMember(response.data);
        setNewNickname(response.data.nickname); // 수정할 때 쓸 기본값 설정
      } catch (error) {
        console.error("내 정보 로딩 실패:", error);
        alert("로그인 정보가 유효하지 않습니다.");
        navigate('/login');
      }
    };
    fetchMyInfo();
  }, [navigate]);

  // 2. 닉네임 수정 요청
  const handleUpdate = async () => {
    if (!newNickname.trim()) {
      alert("닉네임을 입력해주세요.");
      return;
    }

    try {
      const response = await api.put('/members/update', {
        nickname: newNickname
      });
      
      setMember(response.data); // 변경된 정보로 화면 업데이트
      setIsEditing(false); // 수정 모드 종료
      alert("닉네임이 변경되었습니다.");
    } catch (error) {
      console.error("수정 실패:", error);
      // 백엔드에서 중복 닉네임 에러를 던지면 여기서 잡힘
      alert("수정 실패: " + (error.response?.data?.message || "오류가 발생했습니다."));
    }
  };

  // 3. 회원 탈퇴 요청
  const handleWithdraw = async () => {
    if (window.confirm("정말로 탈퇴하시겠습니까? \n탈퇴 시 작성한 게시글은 유지되지만 로그인할 수 없습니다.")) {
      try {
        await api.delete('/members/delete');
        
        // 토큰 삭제 (로그아웃 처리)
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        
        alert("탈퇴가 완료되었습니다. 이용해 주셔서 감사합니다.");
        navigate('/');
        window.location.reload(); // 헤더 상태 변경을 위해 새로고침
        
      } catch (error) {
        console.error("탈퇴 실패:", error);
        alert("탈퇴 처리 중 오류가 발생했습니다.");
      }
    }
  };

  if (!member) return <div style={{ textAlign: 'center', marginTop: '100px' }}>Loading...</div>;

  return (
    <div style={{ maxWidth: '600px', margin: '50px auto', padding: '30px', border: '1px solid #ddd', borderRadius: '10px' }}>
      <h2 style={{ textAlign: 'center', marginBottom: '30px' }}>👤 내 정보 관리</h2>

      {/* 이메일 (수정 불가) */}
      <div style={{ marginBottom: '20px' }}>
        <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '5px' }}>이메일 (ID)</label>
        <input 
          type="text" 
          value={member.email} 
          disabled 
          style={{ width: '100%', padding: '10px', backgroundColor: '#f0f0f0', border: '1px solid #ccc', borderRadius: '5px' }} 
        />
      </div>

      {/* 닉네임 (수정 가능) */}
      <div style={{ marginBottom: '20px' }}>
        <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '5px' }}>닉네임</label>
        <div style={{ display: 'flex', gap: '10px' }}>
          {isEditing ? (
            // 수정 모드일 때
            <>
              <input 
                type="text" 
                value={newNickname} 
                onChange={(e) => setNewNickname(e.target.value)}
                style={{ flex: 1, padding: '10px', borderRadius: '5px', border: '1px solid #333' }}
              />
              <button 
                onClick={handleUpdate}
                style={{ padding: '10px 15px', backgroundColor: '#1890ff', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer' }}
              >
                저장
              </button>
              <button 
                onClick={() => { setIsEditing(false); setNewNickname(member.nickname); }}
                style={{ padding: '10px 15px', backgroundColor: '#ccc', border: 'none', borderRadius: '5px', cursor: 'pointer' }}
              >
                취소
              </button>
            </>
          ) : (
            // 보기 모드일 때
            <>
              <input 
                type="text" 
                value={member.nickname} 
                disabled 
                style={{ flex: 1, padding: '10px', backgroundColor: 'white', border: '1px solid #ccc', borderRadius: '5px' }} 
              />
              <button 
                onClick={() => setIsEditing(true)}
                style={{ padding: '10px 15px', backgroundColor: '#555', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer' }}
              >
                수정
              </button>
            </>
          )}
        </div>
      </div>

      {/* 회원 등급 */}
      <div style={{ marginBottom: '40px' }}>
        <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '5px' }}>회원 등급</label>
        <span style={{ 
          padding: '5px 10px', borderRadius: '15px', fontSize: '14px', fontWeight: 'bold',
          backgroundColor: member.role === 'ADMIN' ? '#ff4d4f' : '#e6f7ff',
          color: member.role === 'ADMIN' ? 'white' : '#1890ff'
        }}>
          {member.role}
        </span>
      </div>

      <hr style={{ margin: '30px 0', border: 'none', borderTop: '1px solid #eee' }} />

      {/* 회원 탈퇴 영역 */}
      <div style={{ textAlign: 'right' }}>
        <button 
          onClick={handleWithdraw}
          style={{ padding: '10px 20px', backgroundColor: 'transparent', color: '#ff4d4f', border: '1px solid #ff4d4f', borderRadius: '5px', cursor: 'pointer', fontSize: '14px' }}
        >
          회원 탈퇴
        </button>
      </div>
    </div>
  );
};

export default MyPage;