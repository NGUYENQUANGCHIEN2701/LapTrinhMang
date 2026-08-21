import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';

function GameCanvas({ mode }) {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#1a1a1a';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    ctx.fillStyle = '#00ff00';
    ctx.font = '24px sans-serif';
    ctx.fillText(`Game mode: ${mode}`, 20, 40);
  }, [mode]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
      <canvas
        ref={canvasRef}
        width={800}
        height={600}
        style={{ border: '2px solid #444', background: '#000' }}
      />
      <Link to="/">Quay lại menu</Link>
    </div>
  );
}

export default GameCanvas;
