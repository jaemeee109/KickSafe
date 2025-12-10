/*
 * ==================================================================================
 * [React 페이지: 관리자 회원 관리]
 * ----------------------------------------------------------------------------------
 * 수정 내용 : 
 * 1. [NEW] 회원 닉네임 검색 기능 추가 (fetchMembers 수정, 검색 UI 추가)
 * 2. 기존 정지/해제/승격 기능 유지
 * ==================================================================================
 */
import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';

const AdminMember = () => {
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // [추가] 검색어 상태 관리
  const [keyword, setKeyword] = useState('');
  
  const navigate = useNavigate();

  // [수정됨] 회원 목록 불러오기 (파라미터로 검색어를 받을 수 있게 변경)
  const fetchMembers = useCallback(async (searchQuery = '') => {
    try {
      setLoading(true);
      
      // 검색어가 있으면 ?keyword=... 붙이고, 없으면 전체 조회
      // (백엔드 AdminController에서 keyword 파라미터를 받도록 구현했어야 함)
      const url = searchQuery 
        ? `/admin/members?keyword=${searchQuery}` 
        : '/admin/members';

      const response = await api.get(url);
      setMembers(response.data.content);
    } catch (error) {
      console.error("로딩 실패:", error);
      alert("접근 권한이 없습니다.");
      navigate('/');
    } finally {
      setLoading(false);
    }
  }, [navigate]); 

  // 초기 로딩 (검색어 없이 전체 조회)
  useEffect(() => {
    fetchMembers();
  }, [fetchMembers]);

  // [추가] 검색 버튼 핸들러
  const handleSearch = () => {
    fetchMembers(keyword); // 현재 입력된 키워드로 재조회 요청
  };

  // [추가] 엔터키 입력 시 검색
  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  // 1. 강퇴(정지) 핸들러
  const handleBan = async (id, nickname) => {
    if (window.confirm(`⛔ 정말 '${nickname}' 회원을 정지시키겠습니까?`)) {
      try {
        await api.post(`/admin/members/ban/${id}`);
        alert("정지 처리되었습니다.");
        fetchMembers(keyword); // 현재 검색 상태 유지하며 새로고침
      } catch (error) {
        alert("오류가 발생했습니다.");
      }
    }
  };

  // 2. 정지 해제 핸들러
  const handleUnban = async (id, nickname) => {
    if (window.confirm(`✅ '${nickname}' 회원의 정지를 해제하시겠습니까?`)) {
      try {
        await api.post(`/admin/members/unban/${id}`);
        alert("정지가 해제되었습니다.");
        fetchMembers(keyword); // 현재 검색 상태 유지하며 새로고침
      } catch (error) {
        alert("오류가 발생했습니다.");
      }
    }
  };

  // 3. 승격 핸들러
  const handlePromote = async (id, nickname) => {
    if (window.confirm(`👑 '${nickname}' 회원에게 관리자 권한을 주시겠습니까?`)) {
      try {
        await api.post(`/admin/members/promote/${id}`);
        alert("관리자로 승격되었습니다.");
        fetchMembers(keyword); 
      } catch (error) {
        alert("오류가 발생했습니다.");
      }
    }
  };

  // 상태 뱃지 렌더링
  const renderStatusBadge = (status) => {
    switch (status) {
      case 'ACTIVE':
        return <span style={{ color: '#28a745', fontWeight: 'bold' }}>🟢 활동중</span>;
      case 'BLACKLISTED':
        return <span style={{ color: '#dc3545', fontWeight: 'bold' }}>⛔ 정지됨</span>;
      case 'WITHDRAWN':
        return <span style={{ color: '#6c757d', fontWeight: 'bold', textDecoration: 'line-through' }}>💀 탈퇴</span>;
      default:
        return <span style={{ color: '#333' }}>{status}</span>;
    }
  };

  return (
    <div style={{ maxWidth: '1000px', margin: '50px auto', padding: '20px' }}>
      <h2 style={{ borderBottom: '2px solid #333', paddingBottom: '15px' }}>👮‍♂️ 회원 관리 시스템</h2>
      
      {/* [추가] 검색바 UI */}
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '15px', marginTop: '20px', gap: '10px' }}>
        <input 
          type="text" 
          placeholder="닉네임으로 검색" 
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={handleKeyPress}
          style={{ padding: '8px', width: '250px', borderRadius: '4px', border: '1px solid #ccc' }}
        />
        <button 
          onClick={handleSearch}
          style={{ padding: '8px 15px', backgroundColor: '#333', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
        >
          🔍 검색
        </button>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', marginTop: '50px' }}>Loading...</div>
      ) : (
        <div style={{ overflowX: 'auto', boxShadow: '0 0 10px rgba(0,0,0,0.1)' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: '600px', backgroundColor: 'white' }}>
            <thead>
              <tr style={{ backgroundColor: '#343a40', color: 'white', textAlign: 'center' }}>
                <th style={{ padding: '15px' }}>ID</th>
                <th>닉네임</th>
                <th>이메일</th>
                <th>상태</th>
                <th>권한</th>
                <th>작업</th>
              </tr>
            </thead>
            <tbody>
              {members.length === 0 ? (
                <tr>
                  <td colSpan="6" style={{ padding: '20px', textAlign: 'center', color: '#888' }}>
                    검색 결과가 없습니다.
                  </td>
                </tr>
              ) : (
                members.map((member) => (
                  <tr key={member.id} style={{ borderBottom: '1px solid #ddd', textAlign: 'center', height: '60px' }}>
                    <td>{member.id}</td>
                    <td style={{ fontWeight: 'bold' }}>{member.nickname}</td>
                    <td style={{ color: '#666' }}>{member.email}</td>
                    
                    {/* 상태 뱃지 */}
                    <td>
                       {renderStatusBadge(member.status)}
                    </td>

                    {/* 권한 뱃지 */}
                    <td>
                      <span style={{ 
                        padding: '5px 10px', borderRadius: '15px', fontSize: '12px', fontWeight: 'bold',
                        backgroundColor: member.role === 'ADMIN' ? '#ff4d4f' : '#e6f7ff',
                        color: member.role === 'ADMIN' ? 'white' : '#1890ff'
                      }}>
                        {member.role}
                      </span>
                    </td>
                    
                    {/* 작업 버튼 그룹 */}
                    <td>
                      <div style={{ display: 'flex', justifyContent: 'center', gap: '10px' }}>
                        {/* 관리자는 건드리지 않음 */}
                        {member.role === 'ADMIN' ? (
                          <span style={{ color: '#ccc', fontSize: '13px' }}>- 관리자 -</span>
                        ) : (
                          <>
                            {/* 승격 버튼 */}
                            <button 
                              onClick={() => handlePromote(member.id, member.nickname)}
                              style={{ padding: '6px 12px', backgroundColor: '#28a745', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' }}
                            >
                              👑 승격
                            </button>

                            {/* 상태에 따라 정지 vs 해제 버튼 교체 */}
                            {member.status === 'BLACKLISTED' ? (
                              <button 
                                onClick={() => handleUnban(member.id, member.nickname)}
                                style={{ padding: '6px 12px', backgroundColor: '#17a2b8', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' }}
                              >
                                🔓 해제
                              </button>
                            ) : (
                              <button 
                                onClick={() => handleBan(member.id, member.nickname)}
                                style={{ padding: '6px 12px', backgroundColor: '#dc3545', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' }}
                              >
                                ⛔ 정지
                              </button>
                            )}
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default AdminMember;