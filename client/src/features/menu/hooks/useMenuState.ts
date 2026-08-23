import { useState } from 'react';
import { MenuItem } from '../types';

export const useMenuState = () => {
  const [menuItems] = useState<MenuItem[]>([
    { id: 'campaign', label: 'Chế độ Chiến dịch (Campaign)', path: '/game/campaign', description: 'Vượt 3 màn chơi thử thách với AI đối thủ' },
    { id: 'online', label: 'Chế độ Trực tuyến (Online)', path: '/game/online', description: 'Chọn bản đồ độc lập thi đấu cố định' },
    { id: 'leaderboard', label: 'Bảng Xếp Hạng', path: '/leaderboard', description: 'Xem vị trí của bạn trên hệ thống' },
  ]);

  return { menuItems };
};
