import { useNavigate } from 'react-router-dom';

function MainMenu() {
  const navigate = useNavigate();

  return (
    <div style={{ textAlign: 'center', paddingTop: '120px' }}>
      <h1>Tankzors</h1>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', alignItems: 'center' }}>
        <button onClick={() => navigate('/game/campaign')}>
          Campaign
        </button>
        <button onClick={() => navigate('/game/online')}>
          Online
        </button>
        <button onClick={() => navigate('/leaderboard')}>
          Bảng xếp hạng
        </button>
      </div>
    </div>
  );
}

export default MainMenu;
