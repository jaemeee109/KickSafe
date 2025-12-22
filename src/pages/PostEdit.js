/*
 * ==================================================================================
 * [React 페이지: 게시글 수정 (PostEdit.js)]
 * ----------------------------------------------------------------------------------
 * 기능 설명 : 
 * 1. 기존 게시글 정보(제목, 내용, 이미지)를 불러와 표시합니다.
 * 2. [이미지 삭제] 기존 이미지의 'X' 버튼 클릭 시 삭제 목록에 추가합니다.
 * 3. [이미지 추가] 새 파일을 첨부할 수 있습니다.
 * 4. [내용 수정] 장소/일시가 포함된 전체 텍스트를 수정합니다.
 * 5. [완료 후 이동] 수정이 성공하면 '상세 페이지(/post/글번호)'로 이동합니다.
 * ==================================================================================
 */
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';

const PostEdit = () => {
  const { id } = useParams(); // URL에서 글 번호 가져오기
  const navigate = useNavigate();

  // 입력값 상태
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  
  // 이미지 관리 상태
  const [existingImages, setExistingImages] = useState([]); // 화면에 보여줄 기존 이미지
  const [deletedMediaIds, setDeletedMediaIds] = useState([]); // 삭제할 이미지 ID 목록
  const [newFiles, setNewFiles] = useState([]); // 새로 추가할 파일 객체들

  // 1. 초기 데이터 로딩
  useEffect(() => {
    const fetchPost = async () => {
      try {
        const response = await api.get('/posts/' + id); // 문자열 연결 사용
        const data = response.data;
        
        setTitle(data.title);
        setContent(data.content);

        // 기존 이미지가 있다면 상태에 저장
        if (data.images && Array.isArray(data.images)) {
          setExistingImages(data.images);
        }
      } catch (error) {
        console.error("데이터 로딩 실패:", error);
        alert("게시글 정보를 불러올 수 없습니다.");
        navigate('/post/list');
      }
    };
    fetchPost();
  }, [id, navigate]);

  // 2. 기존 이미지 삭제 핸들러
  const handleDeleteExisting = (imageId) => {
    if (!window.confirm("이 이미지를 삭제하시겠습니까? (수정 완료 시 반영됩니다)")) return;

    // 삭제할 ID 목록에 추가
    setDeletedMediaIds([...deletedMediaIds, imageId]);
    
    // 화면 목록에서 즉시 제거 (사용자 경험용)
    setExistingImages(existingImages.filter(img => img.id !== imageId));
  };

  // 3. 새 파일 선택 핸들러
  const handleFileChange = (e) => {
    setNewFiles(Array.from(e.target.files));
  };

  // 4. 수정 완료 제출 핸들러
  const handleSubmit = async (e) => {
    e.preventDefault();

    const formData = new FormData();

    // 4-1. 텍스트 데이터 (백엔드 @ModelAttribute 대응)
    formData.append("title", title);
    formData.append("content", content);

    // 4-2. 삭제할 이미지 ID (여러 개일 경우 반복해서 append)
    deletedMediaIds.forEach((delId) => {
      formData.append("deletedMediaIds", delId);
    });

    // 4-3. 새 이미지 파일 (여러 개일 경우 반복해서 append)
    // 주의: 백엔드에서 받는 이름이 'newImages' 여야 합니다.
    newFiles.forEach((file) => {
      formData.append("newImages", file);
    });

    try {
      // PUT 요청 전송
      await api.put('/posts/' + id, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      
      alert("수정되었습니다!");

      // [중요] 상세 페이지로 이동
      // 문자열 연결(+)을 사용하여 경로 오류 방지
      navigate('/post/' + id);

    } catch (error) {
      console.error("수정 실패:", error);
      alert("수정 중 오류가 발생했습니다.");
    }
  };

  return (
    <div style={{ maxWidth: '600px', margin: '50px auto', padding: '20px' }}>
      <h1>게시글 수정</h1>
      <form onSubmit={handleSubmit}>
        
        {/* 제목 입력 */}
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

        {/* 내용 입력 */}
        <div style={{ marginBottom: '15px' }}>
          <label style={{display:'block', marginBottom:'5px', fontWeight:'bold'}}>내용</label>
          <textarea 
            value={content} 
            onChange={(e) => setContent(e.target.value)} 
            style={{ width: '100%', height: '300px', padding: '10px', borderRadius:'5px', border:'1px solid #ddd', fontFamily: 'monospace', whiteSpace: 'pre-wrap' }}
            required
          />
          <p style={{fontSize:'12px', color:'#888', marginTop:'5px'}}>
            * 장소와 일시 정보는 위 내용 칸에서 직접 수정해주세요.
          </p>
        </div>

        {/* 기존 이미지 목록 (삭제 기능 포함) */}
        {existingImages.length > 0 && (
          <div style={{ marginBottom: '20px', padding: '10px', backgroundColor: '#f9f9f9', borderRadius: '8px' }}>
            <label style={{display:'block', marginBottom:'10px', fontWeight:'bold'}}>기존 이미지 삭제</label>
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
              {existingImages.map((img) => (
                <div key={img.id} style={{ position: 'relative', width: '100px' }}>
                  <img 
                    src={`http://34.50.13.223.nip.io:8020${img.url}`} 
                    alt="existing" 
                    style={{ width: '100%', height: '100px', objectFit: 'cover', borderRadius: '5px' }}
                  />
                  {/* 삭제 버튼 */}
                  <button
                    type="button"
                    onClick={() => handleDeleteExisting(img.id)}
                    style={{
                      position: 'absolute', top: '-5px', right: '-5px',
                      backgroundColor: 'red', color: 'white', border: 'none',
                      borderRadius: '50%', width: '22px', height: '22px', cursor: 'pointer',
                      fontSize: '14px', fontWeight: 'bold', display: 'flex', alignItems: 'center', justifyContent: 'center'
                    }}
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* 새 이미지 추가 */}
        <div style={{ marginBottom: '20px' }}>
          <label style={{display:'block', marginBottom:'5px', fontWeight:'bold'}}>새 이미지 추가</label>
          <input type="file" multiple onChange={handleFileChange} />
        </div>

        {/* 버튼 영역 */}
        <div style={{ display: 'flex', gap: '10px' }}>
            <button type="submit" style={{ flex: 1, padding: '15px', backgroundColor: '#1890ff', color: 'white', border: 'none', borderRadius:'5px', fontSize:'16px', fontWeight:'bold', cursor:'pointer' }}>
              수정 완료
            </button>
            <button type="button" onClick={() => navigate('/post/' + id)} style={{ flex: 1, padding: '15px', backgroundColor: '#ddd', color: '#333', border: 'none', borderRadius:'5px', fontSize:'16px', fontWeight:'bold', cursor:'pointer' }}>
              취소
            </button>
        </div>
      </form>
    </div>
  );
};

export default PostEdit;
