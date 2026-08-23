import { useEffect, useState } from 'react';
import { LeaderboardEntry } from '../types';

export const useLeaderboard = () => {
  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    // Simulated fetching from WebSocket Server / API
    const timer = setTimeout(() => {
      setEntries([
        { rank: 1, playerName: 'TankMaster_VN', score: 3500, victories: 12, mode: 'campaign', lastUpdated: '2026-08-23' },
        { rank: 2, playerName: 'ProShooter_99', score: 2900, victories: 9, mode: 'online', lastUpdated: '2026-08-23' },
        { rank: 3, playerName: 'AlphaTank', score: 2450, victories: 7, mode: 'campaign', lastUpdated: '2026-08-22' },
        { rank: 4, playerName: 'SteelDefender', score: 1800, victories: 5, mode: 'online', lastUpdated: '2026-08-21' },
      ]);
      setIsLoading(false);
    }, 400);

    return () => clearTimeout(timer);
  }, []);

  return { entries, isLoading };
};
