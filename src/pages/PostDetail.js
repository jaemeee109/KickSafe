/*
 * ==================================================================================
 * [React 페이지: 게시글 상세 (PostDetail.js)]
 * ----------------------------------------------------------------------------------
 * 기능 설명 : 
 * 1. AI 라벨(영어) 그대로 표시 + 위험도에 따른 색상 구분 (빨강/초록/노랑).
 * 2. '장소'와 '일시'가 포함된 본문을 줄바꿈을 살려 그대로 보여줍니다.
 * 3. 작성자 본인/관리자 권한 체크 후 수정/삭제 버튼 표시.
 * ==================================================================================
 */
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';

/* AI 박스 그리기 컴포넌트 */
const BoundingBoxImage = ({ media }) => {
  const [detections, setDetections] = useState([]);
  const [imgSize, setImgSize] = useState({ w: 0, h: 0 });

  useEffect(() => {
    if (media.detectionInfo) {
      try {
        const parsed = JSON.parse(media.detectionInfo);
        if (Array.isArray(parsed)) setDetections(parsed);
      } catch (e) { console.error("JSON Error", e); }
    }
  }, [media.detectionInfo]);

  const handleImageLoad = (e) => {
    setImgSize({ w: e.target.naturalWidth, h: e.target.naturalHeight });
  };

  // 라벨 스타일 결정 (색상 로직)
  const getLabelStyle = (label) => {
    if (!label) return { text: '', color: '#d9d9d9' };
    const lower = label.toLowerCase();
    let color = '#d9d9d9';

    if (lower.includes('no_helmet') || ['two', 'three', 'multi', '2', '3'].some(k => lower.includes(k))) {
      color = '#ff4d4f'; // 위험 (빨강)
    } else if (lower.includes('helmet')) {
      color = '#1890ff'; // 안전 (파랑)
    } else if (['scooter', 'kickboard', 'patin'].some(k => lower.includes(k))) {
      color = '#faad14'; // 객체 (노랑)
    } else if (['rider', 'person', 'human'].some(k => lower.includes(k))) {
      // 전체 위험도가 높으면 사람도 빨강
      color = (media.riskLevel === 'HIGH' || media.riskLevel === 'CRITICAL') ? '#ff4d4f' : '#52c41a';
    }
    return { text: label, color };
  };

  return (
    <div style={{ position: 'relative', width: '100%', marginBottom: '20px' }}>
      <img 
        src={`http://34.50.13.223.nip.io:8020${media.url}`} 
        alt="ai" onLoad={handleImageLoad}
        style={{ width: '100%', display: 'block', borderRadius: '8px' }}
      />
      {imgSize.w > 0 && detections.length > 0 && (
        <svg viewBox={`0 0 ${imgSize.w} ${imgSize.h}`} style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', pointerEvents: 'none' }}>
          {detections.map((det, index) => {
            const style = getLabelStyle(det.label);
            const points = `${det.x1},${det.y1} ${det.x2},${det.y2} ${det.x3},${det.y3} ${det.x4},${det.y4}`;
            const minX = Math.min(det.x1, det.x2, det.x3, det.x4);
            const minY = Math.min(det.y1, det.y2, det.y3, det.y4);
            const textSize = Math.max(14, imgSize.w * 0.025);
            const textBgWidth = style.text.length * textSize * 0.6 + 20;
            const textBgHeight = textSize * 1.6;

            return (
              <g key={index}>
                <polygon points={points} fill="none" stroke={style.color} strokeWidth={3} strokeLinejoin="round"/>
                <rect x={minX} y={minY - textBgHeight > 0 ? minY - textBgHeight : minY} width={textBgWidth} height={textBgHeight} fill={style.color} rx="4" ry="4"/>
                <text x={minX + textBgWidth / 2} y={(minY - textBgHeight > 0 ? minY - textBgHeight : minY) + textBgHeight / 2} fill="white" fontSize={textSize} fontWeight="bold" textAnchor="middle" dominantBaseline="central">{style.text}</text>
              </g>
            );
          })}
        </svg>
      )}
    </div>
  );
};

/* 메인 컴포넌트 */
const PostDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [post, setPost] = useState(null);
  const [loading, setLoading] = useState(true);
  const [currentUserId, setCurrentUserId] = useState(null);
  const [currentUserRole, setCurrentUserRole] = useState(null);

  useEffect(() => {
    const fetchPost = async () => {
      try {
        const res = await api.get(`/posts/${id}`);
        setPost(res.data);
      } catch (e) { alert("조회 실패"); navigate('/post/list'); } 
      finally { setLoading(false); }
    };
    const fetchMe = async () => {
      if(!localStorage.getItem('accessToken')) return;
      try {
        const res = await api.get('/members/readOne');
        setCurrentUserId(res.data.id);
        setCurrentUserRole(res.data.role);
      } catch(e) {}
    };
    fetchPost(); fetchMe();
  }, [id, navigate]);

  const handleDelete = async () => {
    if(window.confirm("삭제하시겠습니까?")) {
      try { await api.delete(`/posts/${id}`); alert("삭제됨"); navigate('/post/list'); }
      catch(e) { alert("삭제 실패"); }
    }
  };

  if(loading) return <div>Loading...</div>;
  if(!post) return null;

  let displayScore = post.riskScore || 0;
  if(displayScore === 0 && post.images?.length > 0) {
    displayScore = post.images.reduce((sum, img) => sum + (img.riskScore||0), 0) / post.images.length;
  }
  const badgeColor = (post.riskLevel === 'HIGH' || post.riskLevel === 'CRITICAL') ? '#ff4d4f' : '#52c41a';

  return (
    <div style={{ maxWidth: '800px', margin: '50px auto', padding: '20px' }}>
      <div style={{ borderBottom: '1px solid #ddd', paddingBottom: '20px', marginBottom: '30px' }}>
        <h1 style={{ fontSize: '28px', marginBottom: '15px' }}>{post.title}</h1>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '14px', color: '#666' }}>
          <div><span style={{fontWeight:'bold'}}>👤 {post.writer}</span> &nbsp;|&nbsp; <span>🕒 {new Date(post.createdAt).toLocaleDateString()}</span></div>
          <span style={{ padding: '5px 10px', borderRadius: '15px', backgroundColor: badgeColor, color: 'white', fontWeight: 'bold' }}>{post.riskLevel} ({displayScore.toFixed(1)}점)</span>
        </div>
      </div>

      <div style={{ marginBottom: '30px' }}>
        {post.images && post.images.map((media) => (
          <div key={media.id}>
             <BoundingBoxImage media={media} />
             <div style={{ backgroundColor: '#f8f9fa', padding: '15px', borderRadius: '8px', marginTop: '-15px', marginBottom: '30px', border: '1px solid #eee' }}>
                <p style={{ margin: 0, fontWeight: 'bold', color: '#555' }}>🤖 AI 분석: <span style={{ color: (media.riskLevel==='HIGH'||media.riskLevel==='CRITICAL')?'#ff4d4f':'#1890ff' }}>{media.riskLevel}</span> (위험도: {media.riskScore}점)</p>
             </div>
          </div>
        ))}
      </div>

      {/* 본문 (줄바꿈 처리) */}
      <div style={{ minHeight: '200px', fontSize: '16px', lineHeight: '1.6', color: '#333', whiteSpace: 'pre-wrap', backgroundColor: '#fff', padding: '10px' }}>
        {post.content}
      </div>

      <div style={{ marginTop: '50px', borderTop: '1px solid #ddd', paddingTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
        <button onClick={() => navigate('/post/list')} style={{ padding: '10px 20px', backgroundColor: '#555', color: 'white', border: 'none', borderRadius: '5px', cursor:'pointer' }}>목록으로</button>
        {currentUserId && (post.writerId === currentUserId || currentUserRole === 'ADMIN') && (
          <>
            {post.writerId === currentUserId && <button onClick={() => navigate(`/post/edit/${id}`)} style={{ padding: '10px 20px', backgroundColor: '#1890ff', color: 'white', border: 'none', borderRadius: '5px', cursor:'pointer' }}>수정</button>}
            <button onClick={handleDelete} style={{ padding: '10px 20px', backgroundColor: '#ff4d4f', color: 'white', border: 'none', borderRadius: '5px', cursor:'pointer' }}>삭제</button>
          </>
        )}
      </div>
    </div>
  );
};
export default PostDetail;
