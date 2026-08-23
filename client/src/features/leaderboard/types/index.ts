export interface LeaderboardEntry {
  rank: number;
  playerName: string;
  score: number;
  victories: number;
  mode: 'campaign' | 'online';
  lastUpdated: string;
}
