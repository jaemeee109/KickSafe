// 
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8020',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
  // [★추가★] 타임아웃 시간 설정 (밀리초 단위)
  // 10분 (600초 * 1000) = 600000
  // 동영상 업로드는 시간이 오래 걸리므로 넉넉하게 잡아야 합니다.
  timeout: 600000,
});

// [추가된 부분] 요청 인터셉터 (Request Interceptor)
// 요청을 보내기 직전에 "잠깐! 토큰 있는지 확인해볼게" 하고 가로채는 녀석입니다.
api.interceptors.request.use(
  (config) => {
    // 1. 로컬 스토리지에서 토큰을 꺼냅니다.
    const token = localStorage.getItem('accessToken');

    // 2. 토큰이 있다면 헤더에 "Bearer 토큰값" 형태로 붙입니다.
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default api;