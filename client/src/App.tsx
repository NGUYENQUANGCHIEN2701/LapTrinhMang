import React from 'react';
import { ConnectionStatusUI } from './core/network';
import './App.css';

export const App: React.FC = () => {
  return (
    <div style={{
      width: '100%',
      height: '100%',
      minHeight: '100vh',
      backgroundColor: '#121216',
      position: 'relative'
    }}>
      <ConnectionStatusUI />
    </div>
  );
};

export default App;
