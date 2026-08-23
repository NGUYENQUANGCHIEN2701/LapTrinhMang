import { useState } from 'react';
import { CampaignLevel } from '../types';

export const useCampaignProgress = () => {
  const [levels] = useState<CampaignLevel[]>([
    {
      id: 'map_1',
      levelNumber: 1,
      name: 'Màn 1: Căn Cứ Xuất Phát',
      description: 'Tiêu diệt 1 xe tăng AI cơ bản để làm quen thao tác.',
      isUnlocked: true,
      starsEarned: 3,
      rewardMoney: 100,
    },
    {
      id: 'map_2',
      levelNumber: 2,
      name: 'Màn 2: Thung Lũng Chướng Ngại',
      description: 'Phá hủy 2 xe tăng AI tuần tra trong khu vực có tường chắn.',
      isUnlocked: false,
      starsEarned: 0,
      rewardMoney: 250,
    },
    {
      id: 'map_3',
      levelNumber: 3,
      name: 'Màn 3: Pháo Đài Cuối Cùng',
      description: 'Chiến đấu với 3 xe tăng AI bọc thép tốc độ cao.',
      isUnlocked: false,
      starsEarned: 0,
      rewardMoney: 500,
    },
  ]);

  return { levels };
};
