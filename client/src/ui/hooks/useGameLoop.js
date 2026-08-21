import { useEffect } from 'react';

export function useGameLoop(callback) {
  useEffect(() => {
    let animationId;
    function loop() {
      callback();
      animationId = requestAnimationFrame(loop);
    }
    animationId = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(animationId);
  }, [callback]);
}
