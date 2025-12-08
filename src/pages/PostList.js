// 
import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig'; // 백엔드 통신 도구

const PostList = () => {
  // 1. 상태 관리
  const [postList, setPostList] = useState([]); 
  const [pageInfo, setPageInfo] = useState({}); 
  const [currentPage, setCurrentPage] = useState(1); 
  const navigate = useNavigate(); // 카드 클릭 시 이동용

  // 2. 데이터 가져오는 함수
  const fetchPosts = async (page) => {
    try {
      const response = await api.get(`/posts?page=${page}&size=10`);
      const { dtoList, start, end, prev, next } = response.data;
      setPostList(dtoList);
      setPageInfo({ start, end, prev, next });
    } catch (error) {
      console.error("게시글 목록 로딩 실패:", error);
      // alert("목록을 불러오는 중 오류가 발생했습니다.");
    }
  };

  useEffect(() => {
    fetchPosts(currentPage);
  }, [currentPage]);

  // 3. 페이지 번호 버튼 렌더링
  const renderPageNumbers = () => {
    const pages = [];
    if (!pageInfo.start || !pageInfo.end) return null; // 데이터 로딩 전 에러 방지

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
      
      {/* [수정됨] 상단 헤더 영역: 제목 + 글쓰기 버튼 */}
      <div style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        marginBottom: '20px', 
        borderBottom: '2px solid #333', 
        paddingBottom: '15px' 
      }}>
        <h2 style={{ margin: 0 }}>📋 안전신고 목록</h2>
        
        {/* ★ 여기가 추가된 버튼입니다! ★ */}
        <Link to="/post/create">
          <button style={{
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
        </Link>
      </div>

      {/* 게시글 카드 리스트 */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '20px', minHeight: '300px' }}>
        {postList.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '50px 0', color: '#888' }}>
            <p>등록된 게시글이 없습니다.</p>
            <p>첫 번째 신고를 작성해보세요!</p>
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
              {/* 썸네일 영역 수정 */}
              <div style={{ width: '120px', height: '120px', marginRight: '20px', flexShrink: 0, backgroundColor: '#f0f0f0', borderRadius: '5px', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
                
                {post.thumbnail ? (
                  // 썸네일 파일이 비디오인지 확인 (확장자로 단순 체크)
                  post.thumbnail.endsWith('.mp4') || post.thumbnail.endsWith('.avi') || post.thumbnail.endsWith('.mov') ? (
                    // 동영상이면 아이콘 표시
                    <span style={{ fontSize: '40px' }}>🎬</span>
                  ) : (
                    // 이미지면 사진 표시
                    <img 
                      src={`http://localhost:8080${post.thumbnail}`} 
                      alt="thumbnail" 
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  )
                ) : (
                  // 썸네일 없으면 기본 아이콘
                  <span style={{ fontSize: '30px', color: '#ccc' }}>📄</span>
                )}
                
              </div>

              {/* 글 정보 */}
              <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                <h3 style={{ margin: '0 0 10px 0', fontSize: '18px' }}>{post.title}</h3>
                <div style={{ fontSize: '13px', color: '#666' }}>
                  <span style={{ marginRight: '10px' }}>👤 {post.writer}</span>
                  <span>🕒 {new Date(post.createdAt).toLocaleDateString()}</span>
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