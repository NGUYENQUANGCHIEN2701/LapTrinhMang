import { Routes, Route } from 'react-router-dom';
import MainMenu from './ui/screens/MainMenu';
import GameCanvas from './game/GameCanvas';
import LeaderboardScreen from './ui/screens/LeaderboardScreen';
import './App.css';

function App() {
  return (
    <Routes>
      <Route path="/" element={<MainMenu />} />
      <Route path="/game/campaign" element={<GameCanvas mode="campaign" />} />
      <Route path="/game/online" element={<GameCanvas mode="online" />} />
      <Route path="/leaderboard" element={<LeaderboardScreen />} />
    </Routes>
  );
}

export default App;
