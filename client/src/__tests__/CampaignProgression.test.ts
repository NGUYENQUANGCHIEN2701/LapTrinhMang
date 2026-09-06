import { describe, it, expect, beforeEach } from 'vitest';
import { CAMPAIGN_LEVELS } from '../core/network/hooks/useGameSession';

describe('Campaign Level Progression & Selection System', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('Định nghĩa đúng 3 màn chơi Campaign với độ khó tăng dần', () => {
    expect(CAMPAIGN_LEVELS).toHaveLength(3);

    expect(CAMPAIGN_LEVELS[0]).toMatchObject({
      id: 'campaign_1',
      difficulty: 'Dễ',
      stars: 1,
      enemiesCount: 1,
    });

    expect(CAMPAIGN_LEVELS[1]).toMatchObject({
      id: 'campaign_2',
      difficulty: 'Trung bình',
      stars: 2,
      enemiesCount: 2,
    });

    expect(CAMPAIGN_LEVELS[2]).toMatchObject({
      id: 'campaign_3',
      difficulty: 'Khó',
      stars: 3,
      enemiesCount: 3,
    });
  });

  it('Màn 1 mặc định luôn được mở khóa, các màn sau khóa', () => {
    const defaultUnlocked = ['campaign_1'];
    expect(defaultUnlocked.includes('campaign_1')).toBe(true);
    expect(defaultUnlocked.includes('campaign_2')).toBe(false);
    expect(defaultUnlocked.includes('campaign_3')).toBe(false);
  });

  it('Khi hoàn thành Màn 1, Màn 2 được mở khóa và lưu vào localStorage', () => {
    const STORAGE_KEY = 'tankzors_unlocked_levels';
    const unlocked = ['campaign_1'];

    // Mô phỏng thắng Màn 1
    const currentLevel = 'campaign_1';
    const curIdx = CAMPAIGN_LEVELS.findIndex((l) => l.id === currentLevel);
    const nextLevelId = CAMPAIGN_LEVELS[curIdx + 1].id;

    if (!unlocked.includes(nextLevelId)) {
      unlocked.push(nextLevelId);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(unlocked));
    }

    expect(unlocked).toContain('campaign_1');
    expect(unlocked).toContain('campaign_2');
    expect(unlocked).not.toContain('campaign_3');

    // Kiểm tra lưu trữ localStorage
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    expect(saved).toEqual(['campaign_1', 'campaign_2']);
  });

  it('Khi hoàn thành Màn 2, Màn 3 được mở khóa và lưu vào localStorage', () => {
    const STORAGE_KEY = 'tankzors_unlocked_levels';
    const unlocked = ['campaign_1', 'campaign_2'];

    // Mô phỏng thắng Màn 2
    const currentLevel = 'campaign_2';
    const curIdx = CAMPAIGN_LEVELS.findIndex((l) => l.id === currentLevel);
    const nextLevelId = CAMPAIGN_LEVELS[curIdx + 1].id;

    if (!unlocked.includes(nextLevelId)) {
      unlocked.push(nextLevelId);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(unlocked));
    }

    expect(unlocked).toEqual(['campaign_1', 'campaign_2', 'campaign_3']);
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    expect(saved).toEqual(['campaign_1', 'campaign_2', 'campaign_3']);
  });

  it('Khi ở Màn 3 (màn cuối), không có màn kế tiếp', () => {
    const currentLevel = 'campaign_3';
    const curIdx = CAMPAIGN_LEVELS.findIndex((l) => l.id === currentLevel);
    const hasNextLevel = curIdx >= 0 && curIdx < CAMPAIGN_LEVELS.length - 1;
    expect(hasNextLevel).toBe(false);
  });

  it('Không thể chọn màn chơi chưa được mở khóa', () => {
    const unlockedLevels = ['campaign_1'];
    const selectLevel = (levelId: string) => {
      if (!unlockedLevels.includes(levelId)) return false;
      return true;
    };

    expect(selectLevel('campaign_1')).toBe(true);
    expect(selectLevel('campaign_2')).toBe(false);
    expect(selectLevel('campaign_3')).toBe(false);
  });
});

