import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useOnlineMaps } from '../hooks/useOnlineMaps';

export const OnlineScreen: React.FC = () => {
  const { onlineMaps } = useOnlineMaps();
  const navigate = useNavigate();

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      minHeight: '100vh',
      backgroundColor: '#121216',
      color: '#ffffff',
      fontFamily: 'Inter, system-ui, sans-serif',
      padding: '40px 20px'
    }}>
      <div style={{ maxWidth: '700px', width: '100%' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '30px' }}>
          <h1 style={{
            margin: 0,
            fontSize: '2rem',
            background: 'linear-gradient(135deg, #00b0ff, #00e676)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}>
            Chế độ Trực tuyến (Online Maps)
          </h1>
          <Link to="/" style={{ color: '#00b0ff', textDecoration: 'none', fontSize: '0.95rem' }}>
            ← Quay lại Menu
          </Link>
        </div>

        <p style={{ color: '#aaaabb', marginBottom: '24px' }}>
          Ở chế độ này, các bản đồ hoàn toàn độc lập và không cần mở khóa theo thứ tự. Mỗi bản đồ có độ khó cố định.
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {onlineMaps.map((map) => (
            <div
              key={map.id}
              style={{
                background: '#1e1e24',
                borderRadius: '12px',
                padding: '24px',
                border: '1px solid #33333f',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
              }}
            >
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
                  <h3 style={{ margin: 0, fontSize: '1.2rem', color: '#ffffff' }}>
                    {map.name}
                  </h3>
                  <span style={{
                    padding: '2px 10px',
                    borderRadius: '12px',
                    fontSize: '0.75rem',
                    fontWeight: 'bold',
                    background: map.difficulty === 'Dễ' ? '#00e67622' : map.difficulty === 'Trung Bình' ? '#ffeb3b22' : '#ff525222',
                    color: map.difficulty === 'Dễ' ? '#00e676' : map.difficulty === 'Trung Bình' ? '#ffeb3b' : '#ff5252',
                    border: `1px solid ${map.difficulty === 'Dễ' ? '#00e676' : map.difficulty === 'Trung Bình' ? '#ffeb3b' : '#ff5252'}`
                  }}>
                    Độ khó: {map.difficulty}
                  </span>
                </div>
                <p style={{ margin: '0 0 12px 0', fontSize: '0.9rem', color: '#aaaabb' }}>
                  {map.description}
                </p>
                <div style={{ fontSize: '0.85rem', color: '#888899' }}>
                  🤖 Số lượng AI đối thủ: {map.aiCount}
                </div>
              </div>

              <button
                onClick={() => navigate(`/game/online?map=${map.id}`)}
                style={{
                  padding: '12px 24px',
                  borderRadius: '8px',
                  border: 'none',
                  background: 'linear-gradient(135deg, #00b0ff, #00e676)',
                  color: '#000000',
                  fontWeight: 'bold',
                  cursor: 'pointer',
                  fontSize: '0.95rem'
                }}
              >
                Vào Trận Ngay
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default OnlineScreen;
