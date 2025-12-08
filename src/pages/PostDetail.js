// 
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';

const PostDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  
  const [post, setPost] = useState(null);
  const [currentUserId, setCurrentUserId] = useState(null); // 내 ID 저장소

  useEffect(() => {
    // 1. 게시글 정보 가져오기
    const fetchPost = async () => {
      try {
        const response = await api.get(`/posts/${id}`);
        setPost(response.data);
      } catch (error) {
        console.error("게시글 로딩 실패:", error);
        alert("게시글을 불러올 수 없습니다.");
        navigate('/post/list');
      }
    };

    // 2. 로그인한 내 정보(ID) 가져오기
    const fetchMyInfo = async () => {
      try {
        // 토큰이 있을 때만 요청
        const token = localStorage.getItem('accessToken');
        if (token) {
          const response = await api.get('/members/readOne');
          setCurrentUserId(response.data.id);
        }
      } catch (error) {
        console.error("내 정보 로딩 실패:", error);
      }
    };

    fetchPost();
    fetchMyInfo();
  }, [id, navigate]);

  // 삭제 핸들러
  const handleDelete = async () => {
    if (window.confirm("정말 이 글을 삭제하시겠습니까? (복구 불가)")) {
      try {
        await api.delete(`/posts/${id}`);
        alert("삭제되었습니다.");
        navigate('/post/list');
      } catch (error) {
        console.error("삭제 실패:", error);
        alert("삭제 권한이 없습니다.");
      }
    }
  };

  if (!post) return <div style={{ textAlign: 'center', marginTop: '100px' }}>Loading...</div>;

  // ★ 핵심: 내 ID와 글쓴이 ID가 같은지 확인
  const isMyPost = currentUserId === post.writerId;

  return (
    <div style={{ maxWidth: '800px', margin: '50px auto', padding: '20px', border: '1px solid #ddd', borderRadius: '10px' }}>
      
      {/* 제목 및 정보 */}
      <div style={{ borderBottom: '1px solid #eee', paddingBottom: '15px', marginBottom: '20px' }}>
        <h1 style={{ margin: '0 0 10px 0' }}>{post.title}</h1>
        <div style={{ color: '#666', fontSize: '14px', display: 'flex', justifyContent: 'space-between' }}>
          <span>작성자: {post.writer}</span>
          <span>{new Date(post.createdAt).toLocaleString()}</span>
        </div>
      </div>

      {/* 이미지/동영상 갤러리 */}
      {post.images && post.images.length > 0 && (
        <div style={{ marginBottom: '30px' }}>
          {post.images.map((media) => (
            <div key={media.id} style={{ marginBottom: '20px' }}>
              {/* [수정] 타입에 따라 다르게 보여주기 */}
              {media.type === 'VIDEO' ? (
                // 동영상인 경우: video 태그 사용
                <video 
                  src={`http://localhost:8080${media.url}`} 
                  controls // 재생/일시정지 바 표시
                  width="100%" 
                  style={{ borderRadius: '5px', maxHeight: '500px', backgroundColor: 'black' }}
                />
              ) : (
                // 이미지인 경우: img 태그 사용 (기존 코드)
                <img 
                  src={`http://localhost:8080${media.url}`} 
                  alt={`img-${media.id}`} 
                  style={{ width: '100%', maxWidth: '100%', borderRadius: '5px' }}
                />
              )}
            </div>
          ))}
        </div>
      )}

      {/* 본문 */}
      <div style={{ minHeight: '200px', fontSize: '16px', lineHeight: '1.6', whiteSpace: 'pre-wrap' }}>
        {post.content}
      </div>

      {/* 버튼 그룹 */}
      <div style={{ marginTop: '30px', textAlign: 'center', display: 'flex', justifyContent: 'center', gap: '10px' }}>
        <button 
          onClick={() => navigate('/post/list')}
          style={{ padding: '10px 20px', backgroundColor: '#555', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer' }}
        >
          목록으로
        </button>

        {/* ★ 방어 로직: isMyPost가 true일 때만 수정/삭제 버튼 렌더링 ★ */}
        {isMyPost && (
          <>
            <button 
              onClick={() => navigate(`/post/edit/${id}`)}
              style={{ padding: '10px 20px', backgroundColor: '#1890ff', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer' }}
            >
              수정
            </button>

            <button 
              onClick={handleDelete}
              style={{ padding: '10px 20px', backgroundColor: '#ff4d4f', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer' }}
            >
              삭제
            </button>
          </>
        )}
      </div>
    </div>
  );
};

export default PostDetail;