import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { MainMenuScreen } from './features/menu';
import { GameCanvas } from './features/game';
import { CampaignScreen } from './features/campaign';
import { OnlineScreen } from './features/online';
import { LeaderboardScreen } from './features/leaderboard';
import './App.css';

export const App: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<MainMenuScreen />} />
      <Route path="/campaign" element={<CampaignScreen />} />
      <Route path="/online" element={<OnlineScreen />} />
      <Route path="/game/campaign" element={<GameCanvas mode="campaign" />} />
      <Route path="/game/online" element={<GameCanvas mode="online" />} />
      <Route path="/leaderboard" element={<LeaderboardScreen />} />
    </Routes>
  );
};

export default App;
