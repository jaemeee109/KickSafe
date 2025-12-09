// 
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';

const PostEdit = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [existingImages, setExistingImages] = useState([]); 
  const [deletedMediaIds, setDeletedMediaIds] = useState([]); 
  const [newFiles, setNewFiles] = useState([]); 

  useEffect(() => {
    const fetchPostAndCheckPermission = async () => {
      try {
        // 1. 게시글 정보 조회
        const postResponse = await api.get(`/posts/${id}`);
        const post = postResponse.data;

        // 2. 내 정보 조회
        const userResponse = await api.get('/members/readOne');
        const myId = userResponse.data.id;

        // ★ 핵심 방어 로직: ID 불일치 시 강제 퇴장
        if (post.writerId !== myId) {
          alert("수정 권한이 없습니다. (본인의 글만 수정 가능합니다)");
          navigate(-1); // 뒤로 가기
          return;       // 함수 즉시 종료
        }

        // 3. 권한 있으면 데이터 채우기
        setTitle(post.title);
        setContent(post.content);
        setExistingImages(post.images || []); 

      } catch (error) {
        console.error("데이터 로딩 실패:", error);
        alert("접근 권한이 없거나 오류가 발생했습니다.");
        navigate('/post/list');
      }
    };

    fetchPostAndCheckPermission();
  }, [id, navigate]);

  // ... (아래 삭제/파일추가/제출 로직은 기존과 동일)
  const handleDeleteExistingImage = (imageId) => {
    setExistingImages(existingImages.filter(img => img.id !== imageId));
    setDeletedMediaIds([...deletedMediaIds, imageId]);
  };

  const handleFileChange = (e) => {
    setNewFiles(Array.from(e.target.files));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const formData = new FormData();
    formData.append("title", title);
    formData.append("content", content);
    deletedMediaIds.forEach(id => formData.append("deletedMediaIds", id));
    newFiles.forEach(file => formData.append("newImages", file));

    try {
      await api.put(`/posts/${id}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      alert("수정이 완료되었습니다!");
      navigate(`/post/${id}`);
    } catch (error) {
      console.error("수정 실패:", error);
      alert("글 수정에 실패했습니다.");
    }
  };

  return (
    <div style={{ maxWidth: '600px', margin: '50px auto', padding: '20px', border: '1px solid #ddd', borderRadius: '10px' }}>
      <h2 style={{ textAlign: 'center' }}>✏️ 게시글 수정</h2>
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: '15px' }}>
          <label style={{ display: 'block', fontWeight: 'bold' }}>제목</label>
          <input type="text" value={title} onChange={(e) => setTitle(e.target.value)} style={{ width: '100%', padding: '10px' }} required />
        </div>
        <div style={{ marginBottom: '15px' }}>
          <label style={{ display: 'block', fontWeight: 'bold' }}>내용</label>
          <textarea value={content} onChange={(e) => setContent(e.target.value)} style={{ width: '100%', height: '150px', padding: '10px' }} required />
        </div>
        {/* 기존 이미지 영역 */}
        <div style={{ marginBottom: '20px' }}>
          <label style={{ display: 'block', fontWeight: 'bold' }}>기존 사진</label>
          <div style={{ display: 'flex', gap: '10px' }}>
            {existingImages.map((img) => (
              <div key={img.id} style={{ position: 'relative', width: '100px', height: '100px' }}>
                <img src={`http://localhost:8020${img.url}`} alt="thumb" style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '5px' }} />
                <button type="button" onClick={() => handleDeleteExistingImage(img.id)} style={{ position: 'absolute', top: 0, right: 0, background: 'red', color: 'white', border: 'none', cursor: 'pointer' }}>X</button>
              </div>
            ))}
          </div>
        </div>
        {/* 새 파일 영역 */}
        <div style={{ marginBottom: '20px' }}>
          <label style={{ display: 'block', fontWeight: 'bold' }}>새 사진 추가</label>
          <input type="file" multiple accept="image/*" onChange={handleFileChange} />
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button type="submit" style={{ flex: 1, padding: '15px', background: '#1890ff', color: 'white', border: 'none', borderRadius: '5px' }}>수정 완료</button>
          <button type="button" onClick={() => navigate(-1)} style={{ flex: 1, padding: '15px', background: '#ccc', border: 'none', borderRadius: '5px' }}>취소</button>
        </div>
      </form>
    </div>
  );
};

export default PostEdit;