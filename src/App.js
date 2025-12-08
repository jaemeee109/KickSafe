// 
import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Home from './pages/Home';
import Login from './pages/Login';
import OAuthCallback from './pages/OAuthCallback';
import Header from './components/Header';
import PostCreate from './pages/PostCreate';
import PostList from './pages/PostList';
import PostDetail from './pages/PostDetail';
import PostEdit from './pages/PostEdit';
import MemberUpdate from './pages/MemberUpdate';
import MyPage from './pages/MyPage';

function App() {
  return (
    <BrowserRouter>
      {/* 헤더는 Routes 바깥에 둬야 모든 페이지에서 항상 보입니다. */}
      <Header />
      
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
      </Routes>
    </BrowserRouter>
  );
}

export default App;