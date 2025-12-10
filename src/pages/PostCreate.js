/*
 * ==================================================================================
 * [React 페이지: 게시글 작성 (PostCreate.js)]
 * ----------------------------------------------------------------------------------
 * 기능 설명 : 
 * 1. 제목, 내용, 미디어 파일(이미지/동영상)을 입력받아 서버로 전송합니다.
 * 2. [UX 개선] 업로드 진행률(Progress Bar)을 표시하여 사용자를 안심시킵니다.
 * 3. [UX 개선] 작성 완료 후, 목록이 아닌 '방금 작성한 글의 상세 페이지'로 이동합니다.
 * ==================================================================================
 */
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';

const PostCreate = () => {
  const navigate = useNavigate();

  // 입력 필드 상태 관리
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [files, setFiles] = useState([]);
  
  // 로딩 및 업로드 상태 관리
  const [isLoading, setIsLoading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0); // 0 ~ 100%

  // 파일 선택 핸들러
  const handleFileChange = (e) => {
    setFiles(Array.from(e.target.files));
  };

  // 폼 제출 핸들러
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isLoading) return; // 이미 전송 중이면 중복 클릭 방지

    setIsLoading(true);
    setUploadProgress(0); // 진행률 초기화

    // FormData 생성 (파일 전송 시 필수)
    const formData = new FormData();
    formData.append("title", title);
    formData.append("content", content);
    files.forEach((file) => {
      formData.append("images", file);
    });

    try {
      // 1. 백엔드 API 호출
      const response = await api.post('/posts', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        // [핵심 설정] 대용량 파일 업로드 시 타임아웃 방지 (무제한)
        timeout: 1800000, 
        
        // [UX] 업로드 진행률 계산 (Axios 제공 기능)
        onUploadProgress: (progressEvent) => {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          setUploadProgress(percentCompleted);
        },
      });

      // 2. [★수정됨] 성공 후 처리 로직 변경
      // 백엔드(PostController)가 생성된 게시글의 ID(Long)를 반환해줍니다.
      const newPostId = response.data; 

      alert("신고 접수가 완료되었습니다!");
      
      // 3. [★수정됨] 목록이 아니라, 방금 작성한 '상세 페이지'로 이동
      navigate(`/post/${newPostId}`);

    } catch (error) {
      console.error("글 작성 실패:", error);
      
      // 에러 메시지 사용자에게 알림
      if (error.code === 'ECONNABORTED') {
        alert("업로드 시간이 초과되었습니다. 네트워크 상태를 확인해주세요.");
      } else {
        alert("글 작성 중 오류가 발생했습니다.\n(서버 연결 문제 혹은 파일 용량 초과)");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '600px', margin: '50px auto', padding: '20px', border: '1px solid #ddd', borderRadius: '10px' }}>
      <h2 style={{ textAlign: 'center' }}>🚨 안전신고 작성</h2>
      
      <form onSubmit={handleSubmit}>
        {/* 제목 입력 */}
        <div style={{ marginBottom: '15px' }}>
          <label style={{ display: 'block', fontWeight: 'bold' }}>제목</label>
          <input 
            type="text" 
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            style={{ width: '100%', padding: '10px', boxSizing: 'border-box' }}
            required
            placeholder="위험 상황을 요약해주세요"
          />
        </div>

        {/* 내용 입력 */}
        <div style={{ marginBottom: '15px' }}>
          <label style={{ display: 'block', fontWeight: 'bold' }}>내용</label>
          <textarea 
            value={content}
            onChange={(e) => setContent(e.target.value)}
            style={{ width: '100%', height: '150px', padding: '10px', boxSizing: 'border-box' }}
            required
            placeholder="상세한 위치와 상황을 설명해주세요"
          />
        </div>

        {/* 파일 첨부 */}
        <div style={{ marginBottom: '20px' }}>
          <label style={{ display: 'block', fontWeight: 'bold' }}>파일 첨부 (이미지/동영상)</label>
          <input 
            type="file" 
            multiple 
            accept="image/*,video/*" 
            onChange={handleFileChange}
          />
          {files.length > 0 && <p style={{ fontSize: '12px', color: 'blue' }}>📸 {files.length}개 파일 선택됨</p>}
        </div>

        {/* [UX] 업로드 진행바 (로딩 중일 때만 보임) */}
        {isLoading && (
          <div style={{ marginBottom: '20px' }}>
            <div style={{ width: '100%', backgroundColor: '#e0e0e0', borderRadius: '5px', overflow: 'hidden' }}>
              <div style={{ 
                width: `${uploadProgress}%`, 
                backgroundColor: '#1890ff', 
                height: '10px', 
                transition: 'width 0.2s' 
              }}></div>
            </div>
            <p style={{ textAlign: 'center', fontSize: '12px', margin: '5px 0' }}>
              AI가 분석 중입니다... {uploadProgress}% (창을 닫지 마세요)
            </p>
          </div>
        )}

        {/* 제출 버튼 */}
        <button 
          type="submit"
          disabled={isLoading}
          style={{ 
            width: '100%', 
            padding: '15px', 
            backgroundColor: isLoading ? '#ccc' : '#ff4d4f', 
            color: 'white', 
            border: 'none', 
            borderRadius: '5px', 
            fontWeight: 'bold', 
            cursor: isLoading ? 'not-allowed' : 'pointer' 
          }}
        >
          {isLoading ? "분석 및 전송 중..." : "신고하기"}
        </button>
      </form>
    </div>
  );
};

export default PostCreate;