/*
 * ==================================================================================
 * [React 페이지: 게시글 상세 (PostDetail.js)]
 * ----------------------------------------------------------------------------------
 * 기능 설명 : 
 * 1. 게시글 ID로 상세 정보를 조회하여 화면에 표시합니다.
 * 2. [AI 시각화] 이미지 위에 YOLO 감지 영역(빨간 박스)을 그려줍니다. (BoundingBoxImage)
 * 3. [권한 체크] 로그인한 사용자가 글 작성자일 때만 '수정', '삭제' 버튼을 보여줍니다.
 * 4. [점수 보정] 백엔드에서 평균 점수가 0점으로 올 경우, 프론트에서 다시 계산해서 보여줍니다.
 * ==================================================================================
 */

import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';

/*
 * ----------------------------------------------------------------------------------
 * [컴포넌트] BoundingBoxImage
 * 역할: AI가 분석한 좌표 데이터(JSON)를 기반으로 이미지 위에 SVG 박스를 그립니다.
 * ----------------------------------------------------------------------------------
 */
const BoundingBoxImage = ({ media }) => {
  const [detections, setDetections] = useState([]);
  const [imgSize, setImgSize] = useState({ w: 0, h: 0 });

  useEffect(() => {
    if (media.detectionInfo) {
      try {
        setDetections(JSON.parse(media.detectionInfo));
      } catch (e) {
        console.error("JSON 파싱 에러:", e);
      }
    }
  }, [media.detectionInfo]);

  const handleImageLoad = (e) => {
    setImgSize({
      w: e.target.naturalWidth,
      h: e.target.naturalHeight
    });
  };

  return (
    <div style={{ position: 'relative', width: '100%', marginBottom: '20px' }}>
      <img 
        src={`http://localhost:8020${media.url}`} 
        alt="ai-analyzed" 
        onLoad={handleImageLoad}
        style={{ width: '100%', display: 'block', borderRadius: '8px' }}
      />

      {imgSize.w > 0 && detections.length > 0 && (
        <svg 
          viewBox={`0 0 ${imgSize.w} ${imgSize.h}`} 
          style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', pointerEvents: 'none' }}
        >
          {detections.map((det, index) => {
            const points = `${det.x1},${det.y1} ${det.x2},${det.y2} ${det.x3},${det.y3} ${det.x4},${det.y4}`;
            
            const isDanger = ['no_helmet', 'two_riders', 'three_riders'].includes(det.label);
            const color = isDanger ? '#ff4d4f' : '#52c41a'; 
            
            // [1] 폰트 사이즈: 더 작게 조정 (이미지 폭의 1.3%)
            // 최소 12px, 최대 24px로 제한 (너무 커지지 않게)
            const baseSize = Math.max(12, Math.min(imgSize.w * 0.013, 24));
            const strokeSize = Math.max(2, imgSize.w * 0.003);
            
            // [2] 텍스트 자르기 (12글자 제한)
            let displayLabel = det.label;
            if (displayLabel.length > 12) { 
                displayLabel = displayLabel.substring(0, 10) + "..";
            }
            const labelText = `${displayLabel} ${Math.round(det.confidence * 100)}%`;
            
            // [★핵심 수정] 박스 너비 계산 (0.5배로 아주 타이트하게 줄임)
            // Arial 폰트 기준, 글자수 * 0.5가 대략 맞습니다.
            const labelWidth = (labelText.length * (baseSize * 0.5)) + (baseSize * 0.4);
            
            // 라벨 위치 계산
            const isTopEdge = det.y1 < baseSize * 2;
            const labelY = isTopEdge ? det.y1 + baseSize * 1.5 : det.y1 - (baseSize * 0.3);

            return (
              <g key={index}>
                <polygon 
                  points={points} 
                  fill={isDanger ? "rgba(255, 0, 0, 0.1)" : "rgba(0, 255, 0, 0.05)"} 
                  stroke={color} 
                  strokeWidth={strokeSize}
                  strokeLinejoin="round"
                />
                
                {/* 라벨 배경 */}
                <rect 
                  x={det.x1} 
                  y={labelY - baseSize} 
                  width={labelWidth} 
                  height={baseSize * 1.3} 
                  rx={baseSize / 5}
                  fill={color} 
                  opacity="0.9"
                />
                
                {/* 라벨 텍스트 */}
                <text 
                  x={det.x1 + (baseSize * 0.2)} // 왼쪽 여백 최소화
                  y={labelY} 
                  fill="white" 
                  fontSize={baseSize} 
                  // [★중요] 폰트 고정 (너비 예측을 위해)
                  fontFamily="Arial, sans-serif" 
                  fontWeight="bold"
                  style={{ textShadow: '1px 1px 1px rgba(0,0,0,0.3)' }}
                >
                  {labelText}
                </text>
              </g>
            );
          })}
        </svg>
      )}
    </div>
  );
};

/*
 * ----------------------------------------------------------------------------------
 * [컴포넌트] PostDetail (메인)
 * ----------------------------------------------------------------------------------
 */
const PostDetail = () => {
  const { id } = useParams(); // URL에서 글 번호 가져오기
  const navigate = useNavigate();
  
  const [post, setPost] = useState(null);
  const [loading, setLoading] = useState(true);
  
  // 현재 로그인한 사용자의 ID (수정/삭제 권한 확인용)
  const [currentUserId, setCurrentUserId] = useState(null);

  useEffect(() => {
    // 1. 게시글 상세 정보 조회 API 호출
    const fetchPost = async () => {
      try {
        const response = await api.get(`/posts/${id}`);
        setPost(response.data);
      } catch (error) {
        console.error("상세 조회 실패:", error);
        alert("게시글을 불러올 수 없습니다.");
        navigate('/post/list');
      } finally {
        setLoading(false);
      }
    };

    // 2. 내 정보(ID) 조회 API 호출 (권한 체크용)
    const fetchMyInfo = async () => {
      const token = localStorage.getItem('accessToken');
      if (!token) return; // 비로그인 상태면 패스

      try {
        const response = await api.get('/members/readOne');
        setCurrentUserId(response.data.id); // 내 ID 저장
      } catch (error) {
        console.error("내 정보 로딩 실패:", error);
      }
    };

    fetchPost();
    fetchMyInfo();
  }, [id, navigate]);

  // 삭제 버튼 핸들러
  const handleDelete = async () => {
    if (window.confirm("정말 삭제하시겠습니까? (복구할 수 없습니다)")) {
      try {
        await api.delete(`/posts/${id}`);
        alert("삭제되었습니다.");
        navigate('/post/list');
      } catch (error) {
        alert("삭제 권한이 없거나 오류가 발생했습니다.");
      }
    }
  };

  if (loading) return <div style={{ textAlign: 'center', marginTop: '50px' }}>Loading...</div>;
  if (!post) return null;

  // [점수 보정 로직] 백엔드 점수가 0점이면, 이미지들의 평균 점수를 직접 계산해서 보여줌
  let displayScore = post.riskScore || 0;
  if (displayScore === 0 && post.images && post.images.length > 0) {
    const total = post.images.reduce((sum, img) => sum + (img.riskScore || 0), 0);
    displayScore = total / post.images.length;
  }

  return (
    <div style={{ maxWidth: '800px', margin: '50px auto', padding: '20px' }}>
      
      {/* 1. 헤더 영역 (제목, 작성자, 작성일, 위험등급) */}
      <div style={{ borderBottom: '1px solid #ddd', paddingBottom: '20px', marginBottom: '30px' }}>
        <h1 style={{ fontSize: '28px', marginBottom: '15px' }}>{post.title}</h1>
        <div style={{ display: 'flex', justifyContent: 'space-between', color: '#666', fontSize: '14px' }}>
          <div>
            <span style={{ fontWeight: 'bold', marginRight: '10px' }}>👤 {post.writer}</span>
            <span>🕒 {new Date(post.createdAt).toLocaleString()}</span>
          </div>
          <div>
             <span style={{ 
                padding: '5px 10px', 
                borderRadius: '15px', 
                backgroundColor: post.riskLevel === 'HIGH' || post.riskLevel === 'CRITICAL' ? '#ff4d4f' : '#52c41a',
                color: 'white',
                fontWeight: 'bold'
             }}>
               {/* 계산된 displayScore 사용 */}
               {post.riskLevel} (평균 위험도: {displayScore.toFixed(1)}점)
             </span>
          </div>
        </div>
      </div>

      {/* 2. 미디어 영역 (이미지/동영상 + AI 분석 결과) */}
      <div style={{ marginBottom: '30px' }}>
        {post.images && post.images.map((media) => (
          <div key={media.id}>
            {media.type === 'VIDEO' ? (
              <video controls src={`http://localhost:8020${media.url}`} style={{ width: '100%', borderRadius: '8px' }} />
            ) : (
              // AI 박스 그리기 컴포넌트 사용
              <BoundingBoxImage media={media} />
            )}
            
            {/* 분석 점수 요약 박스 */}
            <div style={{ 
                backgroundColor: '#f8f9fa', padding: '15px', borderRadius: '8px', 
                marginTop: '-15px', marginBottom: '30px', border: '1px solid #eee' 
            }}>
                <p style={{ margin: 0, fontWeight: 'bold', color: '#555' }}>
                   🤖 AI 분석 결과: <span style={{ color: '#1890ff' }}>{media.riskLevel}</span> 등급 
                   (상세 점수: {media.riskScore}점)
                </p>
            </div>
          </div>
        ))}
      </div>

      {/* 3. 본문 내용 */}
      <div style={{ minHeight: '200px', fontSize: '16px', lineHeight: '1.6', color: '#333' }}>
        {post.content}
      </div>

      {/* 4. 하단 버튼 영역 */}
      <div style={{ marginTop: '50px', borderTop: '1px solid #ddd', paddingTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
        <button 
          onClick={() => navigate('/post/list')}
          style={{ padding: '10px 20px', backgroundColor: '#555', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer' }}
        >
          목록으로
        </button>
        
        {/* [권한 체크] 현재 로그인한 사람(currentUserId)이 작성자(post.writerId)일 때만 버튼 표시 */}
        {currentUserId && post.writerId === currentUserId && (
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