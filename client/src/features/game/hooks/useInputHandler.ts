import { useEffect, useRef } from 'react';
import { InputState } from '../types';
import { Direction } from '../../../shared/types/game';

interface InputCallbacks {
  onMove: (direction: Direction, moving: boolean) => void;
  onShoot: () => void;
}

export const useInputHandler = ({ onMove, onShoot }: InputCallbacks) => {
  const inputRef = useRef<InputState>({
    up: false,
    down: false,
    left: false,
    right: false,
    shoot: false,
  });

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.repeat) return;

      switch (e.code) {
        case 'KeyW':
        case 'ArrowUp':
          inputRef.current.up = true;
          onMove('UP', true);
          break;
        case 'KeyS':
        case 'ArrowDown':
          inputRef.current.down = true;
          onMove('DOWN', true);
          break;
        case 'KeyA':
        case 'ArrowLeft':
          inputRef.current.left = true;
          onMove('LEFT', true);
          break;
        case 'KeyD':
        case 'ArrowRight':
          inputRef.current.right = true;
          onMove('RIGHT', true);
          break;
        case 'Space':
          inputRef.current.shoot = true;
          onShoot();
          break;
      }
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      switch (e.code) {
        case 'KeyW':
        case 'ArrowUp':
          inputRef.current.up = false;
          if (!inputRef.current.down) onMove('UP', false);
          break;
        case 'KeyS':
        case 'ArrowDown':
          inputRef.current.down = false;
          if (!inputRef.current.up) onMove('DOWN', false);
          break;
        case 'KeyA':
        case 'ArrowLeft':
          inputRef.current.left = false;
          if (!inputRef.current.right) onMove('LEFT', false);
          break;
        case 'KeyD':
        case 'ArrowRight':
          inputRef.current.right = false;
          if (!inputRef.current.left) onMove('RIGHT', false);
          break;
        case 'Space':
          inputRef.current.shoot = false;
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, [onMove, onShoot]);

  return inputRef;
};
