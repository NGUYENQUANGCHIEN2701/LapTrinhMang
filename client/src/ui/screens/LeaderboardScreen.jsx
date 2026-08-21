import { Link } from 'react-router-dom';

function LeaderboardScreen() {
  return (
    <div style={{ textAlign: 'center', paddingTop: '120px' }}>
      <h1>Bảng xếp hạng</h1>
      <p>Chưa có dữ liệu.</p>
      <Link to="/">Quay lại menu</Link>
    </div>
  );
}

export default LeaderboardScreen;
