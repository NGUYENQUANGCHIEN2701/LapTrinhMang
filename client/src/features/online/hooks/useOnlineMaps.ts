import { useState } from 'react';
import { OnlineMap } from '../types';

export const useOnlineMaps = () => {
  const [onlineMaps] = useState<OnlineMap[]>([
    {
      id: 'online_map_1',
      name: 'Chiến Trường Mở',
      difficulty: 'Dễ',
      description: 'Bản đồ thoáng, ít chướng ngại vật thích hợp đấu tập.',
      aiCount: 1,
    },
    {
      id: 'online_map_2',
      name: 'Mê Cung Bọc Thép',
      difficulty: 'Trung Bình',
      description: 'Nhiều tường chắn phức tạp đòi hỏi di chuyển chiến thuật.',
      aiCount: 2,
    },
    {
      id: 'online_map_3',
      name: 'Vùng Đất Tử Thần',
      difficulty: 'Khó',
      description: 'Độ khó cao nhất với lực lượng AI bao vây liên tục.',
      aiCount: 3,
    },
  ]);

  return { onlineMaps };
};
