import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';

const MemberUpdate = () => {
  const navigate = useNavigate();

  const [currentNickname, setCurrentNickname] = useState(''); // 현재 닉네임
  const [newNickname, setNewNickname] = useState('');       // 바꿀 닉네임
  const [isChecked, setIsChecked] = useState(false);        // 중복확인 완료 여부
  const [checkMsg, setCheckMsg] = useState('');             // 중복확인 결과 메시지

  // 1. 들어오자마자 현재 내 정보 가져오기
  useEffect(() => {
    const fetchMyInfo = async () => {
      try {
        const response = await api.get('/members/readOne');
        setCurrentNickname(response.data.nickname);
        setNewNickname(response.data.nickname); // 일단 입력창에 현재 닉네임 넣어둠
      } catch (error) {
        console.error("내 정보 로딩 실패:", error);
        alert("정보를 불러올 수 없습니다.");
        navigate('/');
      }
    };
    fetchMyInfo();
  }, [navigate]);

  // 2. 닉네임 입력 시 중복확인 초기화 (다시 확인받게 함)
  const handleNicknameChange = (e) => {
    setNewNickname(e.target.value);
    setIsChecked(false);
    setCheckMsg('');
  };

  // 3. 중복 확인 버튼 클릭
  const handleCheckDuplicate = async () => {
    if (!newNickname.trim()) {
      alert("닉네임을 입력해주세요.");
      return;
    }
    
    // 현재 닉네임과 같으면 중복확인 필요 없음
    if (newNickname === currentNickname) {
      setCheckMsg("현재 사용 중인 닉네임입니다.");
      setIsChecked(true);
      return;
    }

    try {
      // 백엔드 중복확인 API 호출
      // 리턴값: true(중복), false(사용가능)
      const response = await api.get(`/members/check-nickname?nickname=${newNickname}`);
      const isDuplicate = response.data;

      if (isDuplicate) {
        setCheckMsg("❌ 이미 사용 중인 닉네임입니다.");
        setIsChecked(false);
      } else {
        setCheckMsg("✅ 사용 가능한 닉네임입니다.");
        setIsChecked(true);
      }
    } catch (error) {
      console.error("중복 확인 실패:", error);
      alert("중복 확인 중 오류가 발생했습니다.");
    }
  };

  // 4. 수정 완료 버튼 클릭
  const handleSubmit = async () => {
    if (!isChecked) {
      alert("닉네임 중복 확인을 해주세요.");
      return;
    }

    try {
      // 정보 수정 API 호출
      await api.put('/members/update', {
        nickname: newNickname
      });

      alert("닉네임 설정이 완료되었습니다! 게시판으로 이동합니다.");
      navigate('/post/list'); // 수정 끝나면 게시판으로!

    } catch (error) {
      console.error("수정 실패:", error);
      alert("정보 수정에 실패했습니다.");
    }
  };

  return (
    <div style={{ maxWidth: '500px', margin: '50px auto', padding: '30px', border: '1px solid #ddd', borderRadius: '10px', textAlign: 'center' }}>
      <h2>👤 닉네임 설정</h2>
      <p style={{ color: '#666', marginBottom: '30px' }}>
        커뮤니티 활동을 위해<br/>멋진 닉네임을 설정해주세요!
      </p>

      <div style={{ marginBottom: '20px', textAlign: 'left' }}>
        <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '5px' }}>닉네임</label>
        <div style={{ display: 'flex', gap: '10px' }}>
          <input 
            type="text" 
            value={newNickname}
            onChange={handleNicknameChange}
            placeholder="닉네임 입력"
            style={{ flex: 1, padding: '10px', borderRadius: '5px', border: '1px solid #ccc' }}
          />
          <button 
            onClick={handleCheckDuplicate}
            style={{ padding: '10px 15px', backgroundColor: '#555', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer' }}
          >
            중복확인
          </button>
        </div>
        {/* 확인 메시지 출력 */}
        <p style={{ fontSize: '12px', marginTop: '5px', color: checkMsg.includes('✅') ? 'green' : 'red' }}>
          {checkMsg}
        </p>
      </div>

      <button 
        onClick={handleSubmit}
        style={{ width: '100%', padding: '15px', backgroundColor: '#1890ff', color: 'white', border: 'none', borderRadius: '5px', fontSize: '16px', fontWeight: 'bold', cursor: 'pointer' }}
      >
        설정 완료 및 시작하기
      </button>

      {/* 이미 설정한 사람은 그냥 넘어갈 수 있게 */}
      <div style={{ marginTop: '15px' }}>
         <button 
            onClick={() => navigate('/post/list')}
            style={{ background: 'none', border: 'none', textDecoration: 'underline', cursor: 'pointer', color: '#888' }}
         >
           다음에 변경하기 (건너뛰기)
         </button>
      </div>
    </div>
  );
};

export default MemberUpdate;