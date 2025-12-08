// 
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';

const PostCreate = () => {
  const navigate = useNavigate();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [files, setFiles] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  // [추가] 업로드 진행률 상태 (0 ~ 100)
  const [uploadProgress, setUploadProgress] = useState(0);

  const handleFileChange = (e) => {
    setFiles(Array.from(e.target.files));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isLoading) return;

    setIsLoading(true);
    setUploadProgress(0); // 진행률 초기화

    const formData = new FormData();
    formData.append("title", title);
    formData.append("content", content);
    files.forEach((file) => {
      formData.append("images", file);
    });

    try {
      // 4. 백엔드 전송
      await api.post('/posts', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        // [★핵심 해결책★] 타임아웃 무제한 설정 (0)
        // 동영상은 크기에 따라 10분이 넘을 수도 있으므로 제한을 풉니다.
        timeout: 1800000,
        
        // [추가] 업로드 진행률 계산 (Axios 기능)
        onUploadProgress: (progressEvent) => {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          setUploadProgress(percentCompleted);
        },
      });

      alert("신고 접수가 완료되었습니다!");
      navigate("/post/list");

    } catch (error) {
      console.error("글 작성 실패:", error);
      // 에러 메시지 구체화
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
        {/* 제목 */}
        <div style={{ marginBottom: '15px' }}>
          <label style={{ display: 'block', fontWeight: 'bold' }}>제목</label>
          <input 
            type="text" 
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            style={{ width: '100%', padding: '10px', boxSizing: 'border-box' }}
            required
          />
        </div>

        {/* 내용 */}
        <div style={{ marginBottom: '15px' }}>
          <label style={{ display: 'block', fontWeight: 'bold' }}>내용</label>
          <textarea 
            value={content}
            onChange={(e) => setContent(e.target.value)}
            style={{ width: '100%', height: '150px', padding: '10px', boxSizing: 'border-box' }}
            required
          />
        </div>

        {/* 파일 */}
        <div style={{ marginBottom: '20px' }}>
          <label style={{ display: 'block', fontWeight: 'bold' }}>파일 첨부</label>
          <input 
            type="file" 
            multiple 
            accept="image/*,video/*" 
            onChange={handleFileChange}
          />
          {files.length > 0 && <p style={{ fontSize: '12px', color: 'blue' }}>📸 {files.length}개 선택됨</p>}
        </div>

        {/* [추가] 업로드 진행바 (로딩 중일 때만 보임) */}
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
              업로드 중... {uploadProgress}% (창을 닫지 마세요)
            </p>
          </div>
        )}

        {/* 버튼 */}
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
          {isLoading ? "전송 중..." : "신고하기"}
        </button>
      </form>
    </div>
  );
};

export default PostCreate;