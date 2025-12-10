/*
 * ==================================================================================
 * [React 페이지: 게시글 목록 (PostList.js)]
 * ----------------------------------------------------------------------------------
 * 수정 내용 : 
 * 1. [NEW] 게시글 검색 기능 추가 (제목, 작성자)
 * 2. 기존 페이징, 신고하기 버튼, 날짜 변환 기능 유지
 * ==================================================================================
 */

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig'; 

const PostList = () => {
  // 1. 상태 관리
  const [postList, setPostList] = useState([]); 
  const [pageInfo, setPageInfo] = useState({}); 
  const [currentPage, setCurrentPage] = useState(1); 
  
  // [추가] 검색 관련 상태
  const [searchType, setSearchType] = useState('t'); // 검색 타입 (t:제목, w:작성자, tw:제목+작성자)
  const [searchKeyword, setSearchKeyword] = useState(''); // 검색어

  const navigate = useNavigate(); 

  // 날짜 포맷팅 함수
  const formatDate = (dateString) => {
    if (!dateString) return "-"; 
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR', {
      year: 'numeric', month: 'numeric', day: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  };

  // 신고하기 버튼 핸들러
  const handleCreateClick = () => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      alert("로그인이 필요한 서비스입니다.");
      navigate('/login'); 
      return;
    }
    navigate('/post/create');
  };

  // 2. 데이터 가져오는 함수 (검색어 포함)
  const fetchPosts = async (page) => {
    try {
      // 기본 URL (페이징)
      let url = `/posts?page=${page}&size=10`;
      
      // [수정] 검색어가 있으면 URL에 파라미터 추가
      if (searchKeyword.trim()) {
        url += `&type=${searchType}&keyword=${encodeURIComponent(searchKeyword)}`;
      }

      const response = await api.get(url);
      const { dtoList, start, end, prev, next } = response.data;
      
      setPostList(dtoList);
      setPageInfo({ start, end, prev, next });
    } catch (error) {
      console.error("게시글 목록 로딩 실패:", error);
    }
  };

  // 페이지 번호가 바뀔 때마다 데이터 다시 로딩
  useEffect(() => {
    fetchPosts(currentPage);
    // eslint-disable-next-line
  }, [currentPage]);

  // [추가] 검색 버튼 핸들러
  const handleSearch = () => {
    // 검색 시 1페이지로 이동하고 데이터 조회
    if (currentPage === 1) {
        fetchPosts(1); // 이미 1페이지면 강제 조회
    } else {
        setCurrentPage(1); // 1페이지로 변경 -> useEffect가 실행됨
    }
  };

  // 3. 페이지 번호 버튼 렌더링
  const renderPageNumbers = () => {
    const pages = [];
    if (!pageInfo.start || !pageInfo.end) return null;

    for (let i = pageInfo.start; i <= pageInfo.end; i++) {
      pages.push(
        <button
          key={i}
          onClick={() => setCurrentPage(i)}
          style={{
            margin: '0 5px',
            padding: '5px 10px',
            backgroundColor: currentPage === i ? '#333' : '#fff',
            color: currentPage === i ? '#fff' : '#333',
            border: '1px solid #ddd',
            cursor: 'pointer',
            borderRadius: '3px'
          }}
        >
          {i}
        </button>
      );
    }
    return pages;
  };

  return (
    <div style={{ maxWidth: '800px', margin: '50px auto', padding: '20px' }}>
      
      {/* 상단 헤더 영역 */}
      <div style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        marginBottom: '20px', 
        borderBottom: '2px solid #333', 
        paddingBottom: '15px' 
      }}>
        <h2 style={{ margin: 0 }}>📋 안전신고 목록</h2>
        
        <button 
          onClick={handleCreateClick}
          style={{
            padding: '10px 20px',
            backgroundColor: '#ff4d4f', // 빨간색 포인트
            color: 'white',
            border: 'none',
            borderRadius: '5px',
            fontWeight: 'bold',
            fontSize: '16px',
            cursor: 'pointer',
            boxShadow: '0 2px 5px rgba(0,0,0,0.1)'
          }}>
          ✏️ 신고하기
        </button>
      </div>

      {/* [추가] 검색 필터 영역 (오른쪽 정렬) */}
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '20px', gap: '8px' }}>
        {/* 검색 타입 선택 */}
        <select 
          value={searchType} 
          onChange={(e) => setSearchType(e.target.value)}
          style={{ padding: '8px', borderRadius: '5px', border: '1px solid #ddd', outline: 'none' }}
        >
          <option value="t">제목</option>
          <option value="w">작성자</option>
          <option value="tw">제목+작성자</option>
        </select>

        {/* 검색어 입력창 */}
        <input 
          type="text" 
          placeholder="검색어를 입력하세요" 
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()} // 엔터키 지원
          style={{ padding: '8px', borderRadius: '5px', border: '1px solid #ddd', width: '200px', outline: 'none' }}
        />

        {/* 검색 버튼 */}
        <button 
          onClick={handleSearch}
          style={{ padding: '8px 15px', backgroundColor: '#333', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer' }}
        >
          🔍
        </button>
      </div>

      {/* 게시글 카드 리스트 */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '20px', minHeight: '300px' }}>
        {postList.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '50px 0', color: '#888' }}>
            <p>검색된 게시글이 없습니다.</p>
          </div>
        ) : (
          postList.map((post) => (
            <div 
              key={post.id} 
              onClick={() => navigate(`/post/${post.id}`)} 
              style={{ 
                display: 'flex', border: '1px solid #eee', borderRadius: '10px', 
                padding: '15px', boxShadow: '0 2px 5px rgba(0,0,0,0.05)',
                cursor: 'pointer', transition: 'transform 0.2s', backgroundColor: 'white'
              }}
              onMouseOver={(e) => e.currentTarget.style.transform = 'translateY(-2px)'}
              onMouseOut={(e) => e.currentTarget.style.transform = 'translateY(0)'}
            >
              {/* 썸네일 영역 */}
              <div style={{ width: '120px', height: '120px', marginRight: '20px', flexShrink: 0, backgroundColor: '#f0f0f0', borderRadius: '5px', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
                {post.thumbnail ? (
                  post.thumbnail.endsWith('.mp4') || post.thumbnail.endsWith('.avi') || post.thumbnail.endsWith('.mov') ? (
                    <span style={{ fontSize: '40px' }}>🎬</span>
                  ) : (
                    <img 
                      src={`http://localhost:8020${post.thumbnail}`} 
                      alt="thumbnail" 
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  )
                ) : (
                  <span style={{ fontSize: '30px', color: '#ccc' }}>📄</span>
                )}
              </div>

              {/* 글 정보 */}
              <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                <h3 style={{ margin: '0 0 10px 0', fontSize: '18px' }}>{post.title}</h3>
                <div style={{ fontSize: '13px', color: '#666' }}>
                  <span style={{ marginRight: '10px' }}>👤 {post.writer}</span>
                  <span style={{ color: '#1890ff', fontWeight: 'bold' }}>
                    🕒 {formatDate(post.analyzedAt || post.createdAt)}
                  </span>
                </div>
                <p style={{ 
                  color: '#888', fontSize: '14px', marginTop: '10px',
                  overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box',
                  WebkitLineClamp: 2, WebkitBoxOrient: 'vertical'
                }}>
                  {post.content}
                </p>
              </div>
            </div>
          ))
        )}
      </div>

      {/* 페이징 버튼 */}
      <div style={{ display: 'flex', justifyContent: 'center', marginTop: '40px' }}>
        {pageInfo.prev && (
          <button onClick={() => setCurrentPage(pageInfo.start - 1)} style={{ marginRight: '10px', padding: '5px 10px', cursor: 'pointer' }}>
            &lt; 이전
          </button>
        )}
        {renderPageNumbers()}
        {pageInfo.next && (
          <button onClick={() => setCurrentPage(pageInfo.end + 1)} style={{ marginLeft: '10px', padding: '5px 10px', cursor: 'pointer' }}>
            다음 &gt;
          </button>
        )}
      </div>
    </div>
  );
};

export default PostList;