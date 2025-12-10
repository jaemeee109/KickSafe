// 
import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Home from './pages/Home';
import Login from './pages/Login';
import OAuthCallback from './pages/OAuthCallback';
import Header from './components/Header';
import Footer from './components/Footer'; // [추가] Footer 임포트
import PostCreate from './pages/PostCreate';
import PostList from './pages/PostList';
import PostDetail from './pages/PostDetail';
import PostEdit from './pages/PostEdit';
import MemberUpdate from './pages/MemberUpdate';
import MyPage from './pages/MyPage';
import Statistics from './pages/Statistics';
import AdminMember from './pages/AdminMember';

function App() {
  return (
    <BrowserRouter>
      {/* [스타일 설정] 
        화면 전체 높이(minHeight: 100vh)를 잡고, 
        Flexbox로 내용을 위아래로 배치하여 Footer가 항상 바닥에 붙게 함
      */}
      <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
        
        {/* 헤더 */}
        <Header />
        
        {/* 본문 (남은 공간을 꽉 채움) */}
        <div style={{ flex: 1 }}>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/oauth/callback" element={<OAuthCallback />} />
            <Route path="/post/create" element={<PostCreate />} />
            <Route path="/post/list" element={<PostList />} />
            <Route path="/post/:id" element={<PostDetail />} />
            <Route path="/post/edit/:id" element={<PostEdit />} />
            <Route path="/member/update" element={<MemberUpdate />} />
            <Route path="/mypage" element={<MyPage />} />
            <Route path="/statistics" element={<Statistics />} />
            <Route path="/admin/members" element={<AdminMember />} />
          </Routes>
        </div>

        {/* 푸터 */}
        <Footer />
        
      </div>
    </BrowserRouter>
  );
}

export default App;