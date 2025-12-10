/*
 * ==================================================================================
 * [React 컴포넌트: Footer (바닥글)]
 * ----------------------------------------------------------------------------------
 * 목적: 사이트 하단에 저작권 정보, 링크, 연락처 등을 표시
 * 특징: 
 * - 모든 페이지의 맨 아래에 고정적으로 보여짐
 * - 어두운 배경으로 안정감 부여
 * ==================================================================================
 */
import React from 'react';

const Footer = () => {
  return (
    <footer style={{
      backgroundColor: '#343a40', // 짙은 회색 배경
      color: '#f8f9fa',           // 밝은 글씨
      padding: '40px 20px',
      marginTop: 'auto',          // 내용이 적어도 바닥에 붙게 하기 위한 설정 (App.css와 연동 필요)
      textAlign: 'center',
      fontSize: '14px'
    }}>
      
      {/* 상단: 서비스 이름 및 간단 소개 */}
      <div style={{ marginBottom: '20px' }}>
        <h3 style={{ margin: '0 0 10px 0', fontSize: '18px' }}>🚔 KickSafe</h3>
        <p style={{ margin: 0, color: '#adb5bd' }}>
          안전한 킥보드 문화를 만들어가는 AI 기반 신고 플랫폼
        </p>
      </div>

      {/* 중간: 링크 모음 (실제 기능은 없지만 있어 보이게) */}
      <div style={{ marginBottom: '20px' }}>
        <span style={{ cursor: 'pointer', margin: '0 10px' }}>이용약관</span>
        |
        <span style={{ cursor: 'pointer', margin: '0 10px' }}>개인정보처리방침</span>
        |
        <span style={{ cursor: 'pointer', margin: '0 10px' }}>오시는 길</span>
      </div>

      {/* 하단: 저작권 및 연락처 */}
      <div style={{ color: '#6c757d', fontSize: '12px' }}>
        <p style={{ margin: '5px 0' }}>
          Developer: Your Name | Contact: kicksafe@example.com
        </p>
        <p style={{ margin: 0 }}>
          Copyright © 2025 KickSafe. All rights reserved.
        </p>
      </div>

    </footer>
  );
};

export default Footer;