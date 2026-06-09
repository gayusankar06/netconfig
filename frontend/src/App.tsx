import { useState, useEffect } from 'react';
import axios from 'axios';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { theme } from './theme';
import NetworkBackground from './components/NetworkBackground';
import AuthPage from './components/AuthPage';
import Dashboard from './components/Dashboard';
import ReviewDetailPage from './components/ReviewDetailPage';
import HistoryPage from './components/HistoryPage';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

type Page = 'dashboard' | 'review-detail' | 'history';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentPage, setCurrentPage] = useState<Page>('dashboard');
  const [activeReviewId, setActiveReviewId] = useState<string | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) setIsAuthenticated(true);

    const interceptor = axios.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response && error.response.status === 401) {
          localStorage.removeItem('token');
          setIsAuthenticated(false);
          setCurrentPage('dashboard');
          setActiveReviewId(null);
        }
        return Promise.reject(error);
      }
    );
    return () => axios.interceptors.response.eject(interceptor);
  }, []);

  const handleLogin = (token: string) => {
    localStorage.setItem('token', token);
    setIsAuthenticated(true);
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    setIsAuthenticated(false);
    setCurrentPage('dashboard');
    setActiveReviewId(null);
  };

  const navigateToReview = (reviewId: string) => {
    setActiveReviewId(reviewId);
    setCurrentPage('review-detail');
  };

  const navigateBack = () => {
    setCurrentPage('dashboard');
    setActiveReviewId(null);
  };

  const navigateToHistory = () => {
    setCurrentPage('history');
  };

  const renderPage = () => {
    if (currentPage === 'review-detail' && activeReviewId) {
      return <ReviewDetailPage reviewId={activeReviewId} onBack={navigateBack} />;
    }
    if (currentPage === 'history') {
      return <HistoryPage onBack={navigateBack} onViewReview={navigateToReview} />;
    }
    return (
      <Dashboard
        onLogout={handleLogout}
        onViewReview={navigateToReview}
        onNavigateHistory={navigateToHistory}
      />
    );
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <NetworkBackground />
      {isAuthenticated ? renderPage() : <AuthPage onLogin={handleLogin} />}
      <ToastContainer position="bottom-right" theme="dark" />
    </ThemeProvider>
  );
}

export default App;
