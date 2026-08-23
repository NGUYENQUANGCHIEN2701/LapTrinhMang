import React from 'react';
import { Link } from 'react-router-dom';
import { useLeaderboard } from '../hooks/useLeaderboard';

export const LeaderboardScreen: React.FC = () => {
  const { entries, isLoading } = useLeaderboard();

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
      <div style={{ maxWidth: '800px', width: '100%' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '30px' }}>
          <h1 style={{
            margin: 0,
            fontSize: '2rem',
            background: 'linear-gradient(135deg, #ffeb3b, #ff9800)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}>
            🏆 Bảng Xếp Hạng Server
          </h1>
          <Link to="/" style={{ color: '#ffeb3b', textDecoration: 'none', fontSize: '0.95rem' }}>
            ← Quay lại Menu
          </Link>
        </div>

        {isLoading ? (
          <div style={{ textAlign: 'center', color: '#888899', padding: '40px' }}>
            Đang tải dữ liệu bảng xếp hạng từ server...
          </div>
        ) : (
          <div style={{
            background: '#1e1e24',
            borderRadius: '12px',
            overflow: 'hidden',
            border: '1px solid #33333f',
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.4)'
          }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead>
                <tr style={{ background: '#22222a', borderBottom: '1px solid #33333f', color: '#aaaabb', fontSize: '0.85rem' }}>
                  <th style={{ padding: '16px 20px' }}>THỨ HẠNG</th>
                  <th style={{ padding: '16px 20px' }}>NGƯỜI CHƠI</th>
                  <th style={{ padding: '16px 20px' }}>ĐIỂM SỐ</th>
                  <th style={{ padding: '16px 20px' }}>TRẬN THẮNG</th>
                  <th style={{ padding: '16px 20px' }}>CẬP NHẬT</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((entry) => (
                  <tr
                    key={entry.rank}
                    style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.05)', fontSize: '0.95rem' }}
                  >
                    <td style={{ padding: '16px 20px', fontWeight: 'bold' }}>
                      {entry.rank === 1 && '🥇 1'}
                      {entry.rank === 2 && '🥈 2'}
                      {entry.rank === 3 && '🥉 3'}
                      {entry.rank > 3 && `#${entry.rank}`}
                    </td>
                    <td style={{ padding: '16px 20px', fontWeight: 600, color: '#00e676' }}>
                      {entry.playerName}
                    </td>
                    <td style={{ padding: '16px 20px', color: '#ffeb3b', fontWeight: 'bold' }}>
                      {entry.score.toLocaleString()}
                    </td>
                    <td style={{ padding: '16px 20px' }}>
                      {entry.victories}
                    </td>
                    <td style={{ padding: '16px 20px', color: '#888899', fontSize: '0.85rem' }}>
                      {entry.lastUpdated}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default LeaderboardScreen;
