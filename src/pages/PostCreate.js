/*
 * ==================================================================================
 * [React 페이지: 게시글 등록 (PostCreate.js)]
 * ----------------------------------------------------------------------------------
 * [수정 완료] 백엔드 @ModelAttribute 방식에 맞춰 FormData 전송 방식 변경
 * 1. JSON으로 묶지 않고, formData.append('title', ...), formData.append('content', ...) 로 직접 넣음.
 * 2. 장소/일시 정보를 'content' 문자열 맨 앞에 합쳐서 전송.
 * ==================================================================================
 */
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';

const PostCreate = () => {
  const navigate = useNavigate();
  
  // 상태 관리
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [location, setLocation] = useState(''); // 장소
  const [takenAt, setTakenAt] = useState('');   // 일시
  const [files, setFiles] = useState([]);

  // 파일 선택
  const handleFileChange = (e) => {
    setFiles(Array.from(e.target.files));
  };

  // 제출 핸들러
  const handleSubmit = async (e) => {
    e.preventDefault();

    // 1. [꼼수] 장소와 일시를 본문(content)과 합치기
    let finalContent = content;
    if (location || takenAt) {
        const infoPrefix = `📍 촬영 장소: ${location || '미입력'}\n📅 촬영 일시: ${takenAt || '미입력'}\n\n--------------------------------\n\n`;
        finalContent = infoPrefix + content;
    }

    // 2. FormData 생성
    const formData = new FormData();

    // [중요] @ModelAttribute는 데이터를 각각 따로 넣어줘야 합니다!
    // 백엔드 DTO 필드명과 정확히 일치해야 함 (title, content, images)
    formData.append("title", title);
    formData.append("content", finalContent); 

    // 이미지는 여러 장일 수 있으므로 반복문으로 추가
    files.forEach((file) => {
      formData.append("images", file);
    });

    try {
      // 3. 전송
      await api.post('/posts', formData, {
        headers: { 
            // Axios가 FormData를 감지하면 자동으로 Content-Type을 설정하지만,
            // 명시적으로 적어주어도 무방합니다.
            'Content-Type': 'multipart/form-data' 
        }
      });
      alert("등록되었습니다!");
      navigate('/post/list');
    } catch (error) {
      console.error("등록 실패:", error);
      alert("글 등록 중 오류가 발생했습니다.");
    }
  };

  return (
    <div style={{ maxWidth: '600px', margin: '50px auto', padding: '20px' }}>
      <h1>새 게시글 작성</h1>
      <form onSubmit={handleSubmit}>
        
        {/* 제목 */}
        <div style={{ marginBottom: '15px' }}>
          <label style={{display:'block', marginBottom:'5px', fontWeight:'bold'}}>제목</label>
          <input 
            type="text" 
            value={title} 
            onChange={(e) => setTitle(e.target.value)} 
            style={{ width: '100%', padding: '10px', borderRadius:'5px', border:'1px solid #ddd' }}
            required 
          />
        </div>

        {/* 장소 입력 (선택) */}
        <div style={{ marginBottom: '15px', padding:'15px', backgroundColor:'#f9f9f9', borderRadius:'8px' }}>
          <label style={{display:'block', marginBottom:'5px', fontWeight:'bold'}}>📍 촬영 장소 (선택)</label>
          <input 
            type="text" 
            value={location} 
            onChange={(e) => setLocation(e.target.value)} 
            placeholder="예: 강남역 1번 출구"
            style={{ width: '100%', padding: '10px', borderRadius:'5px', border:'1px solid #ddd' }}
          />
        </div>

        {/* 일시 입력 (선택) */}
        <div style={{ marginBottom: '15px', padding:'15px', backgroundColor:'#f9f9f9', borderRadius:'8px' }}>
          <label style={{display:'block', marginBottom:'5px', fontWeight:'bold'}}>📅 촬영 일시 (선택)</label>
          <input 
            type="datetime-local" 
            value={takenAt} 
            onChange={(e) => setTakenAt(e.target.value)} 
            style={{ width: '100%', padding: '10px', borderRadius:'5px', border:'1px solid #ddd' }}
          />
        </div>

        {/* 내용 */}
        <div style={{ marginBottom: '15px' }}>
          <label style={{display:'block', marginBottom:'5px', fontWeight:'bold'}}>내용</label>
          <textarea 
            value={content} 
            onChange={(e) => setContent(e.target.value)} 
            placeholder="내용을 입력하세요..."
            style={{ width: '100%', height: '200px', padding: '10px', borderRadius:'5px', border:'1px solid #ddd' }}
            required
          />
        </div>

        {/* 파일 업로드 */}
        <div style={{ marginBottom: '20px' }}>
          <label style={{display:'block', marginBottom:'5px', fontWeight:'bold'}}>사진 첨부</label>
          <input type="file" multiple onChange={handleFileChange} />
        </div>

        <button type="submit" style={{ width:'100%', padding: '15px', backgroundColor: '#1890ff', color: 'white', border: 'none', borderRadius:'5px', fontSize:'16px', fontWeight:'bold', cursor:'pointer' }}>
          등록하기
        </button>
      </form>
    </div>
  );
};

export default PostCreate;
