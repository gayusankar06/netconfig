import React, { useState, useEffect } from 'react';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { theme } from './theme';
import NetworkBackground from './components/NetworkBackground';
import AuthPage from './components/AuthPage';
import Dashboard from './components/Dashboard';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import keycloak from './keycloak';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    setIsAuthenticated(keycloak.authenticated || false);
  }, []);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <NetworkBackground />
      
      {isAuthenticated ? (
        <Dashboard onLogout={() => keycloak.logout()} />
      ) : (
        <AuthPage />
      )}
      
      <ToastContainer position="bottom-right" theme="dark" />
    </ThemeProvider>
  );
}

export default App;
