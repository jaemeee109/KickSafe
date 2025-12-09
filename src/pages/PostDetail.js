import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';

const PostDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [post, setPost] = useState(null);
  const [currentUserId, setCurrentUserId] = useState(null);

  // 1. 게시글 및 내 정보 가져오기
  useEffect(() => {
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

    const fetchMyInfo = async () => {
      try {
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

  // [추가] 위험 등급별 색상 결정 함수
  const getRiskColor = (level) => {
    switch (level) {
      case 'CRITICAL': return '#ff0000'; // 매우 위험 (빨강)
      case 'HIGH': return '#ff4d4f';     // 위험 (연한 빨강)
      case 'MEDIUM': return '#faad14';   // 주의 (주황)
      default: return '#52c41a';         // 안전 (초록)
    }
  };

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

  const isMyPost = currentUserId === post.writerId;

  return (
    <div style={{ maxWidth: '800px', margin: '50px auto', padding: '20px', border: '1px solid #ddd', borderRadius: '10px' }}>
      
      {/* 제목 및 정보 영역 */}
      <div style={{ borderBottom: '1px solid #eee', paddingBottom: '15px', marginBottom: '20px' }}>
        
        {/* [추가] 위험 등급 뱃지와 제목을 나란히 배치 */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
           <span style={{ 
             backgroundColor: getRiskColor(post.riskLevel), 
             color: 'white', 
             padding: '5px 12px', 
             borderRadius: '20px', 
             fontWeight: 'bold', 
             fontSize: '14px' 
           }}>
             {post.riskLevel || '분석 대기'}
           </span>
           <h1 style={{ margin: 0, fontSize: '24px' }}>{post.title}</h1>
        </div>

        <div style={{ color: '#666', fontSize: '14px', display: 'flex', justifyContent: 'space-between' }}>
          <span>작성자: {post.writer}</span>
          <span>{new Date(post.createdAt).toLocaleString()}</span>
        </div>
      </div>

      {/* 이미지/동영상 갤러리 */}
      {post.images && post.images.length > 0 && (
        <div style={{ marginBottom: '30px' }}>
          {post.images.map((media) => (
            <div key={media.id} style={{ marginBottom: '30px', border: '1px solid #f0f0f0', padding: '15px', borderRadius: '10px', backgroundColor: '#fafafa' }}>
              
              {/* [추가] 사진별 AI 점수 표시 영역 */}
              <div style={{ 
                marginBottom: '10px', 
                fontWeight: 'bold', 
                color: getRiskColor(media.riskLevel),
                display: 'flex',
                alignItems: 'center',
                gap: '5px'
              }}>
                🤖 AI 분석 결과: {media.riskScore ? media.riskScore.toFixed(1) : 0}점 
                <span style={{ fontSize: '12px', color: '#666', fontWeight: 'normal' }}>
                  ({media.riskLevel || 'LOW'})
                </span>
              </div>

              {media.type === 'VIDEO' ? (
                <video 
                  src={`http://localhost:8020${media.url}`} 
                  controls 
                  width="100%" 
                  style={{ borderRadius: '5px', maxHeight: '500px', backgroundColor: 'black' }}
                />
              ) : (
                <img 
                  src={`http://localhost:8020${media.url}`} 
                  alt={`img-${media.id}`} 
                  style={{ width: '100%', maxWidth: '100%', borderRadius: '5px' }}
                />
              )}
            </div>
          ))}
        </div>
      )}

      {/* 본문 내용 */}
      <div style={{ minHeight: '200px', fontSize: '16px', lineHeight: '1.6', whiteSpace: 'pre-wrap' }}>
        {post.content}
      </div>

      {/* 하단 버튼 그룹 */}
      <div style={{ marginTop: '30px', textAlign: 'center', display: 'flex', justifyContent: 'center', gap: '10px' }}>
        <button 
          onClick={() => navigate('/post/list')}
          style={{ padding: '10px 20px', backgroundColor: '#555', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer' }}
        >
          목록으로
        </button>

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