// @ts-nocheck
import { useState, useEffect } from 'react';
import axios from 'axios';
import { Box, CssBaseline } from '@mui/material';
import { ThemeProvider } from '@mui/material/styles';
import { theme } from './theme';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import NetworkBackground from './components/NetworkBackground';
import AuthPage from './components/AuthPage';
import Sidebar, { AppPage } from './components/Sidebar';
import TopBar from './components/TopBar';
import Dashboard from './components/Dashboard';
import UploadPage from './components/UploadPage';
import HistoryPage from './components/HistoryPage';
import ReviewDetailPage from './components/ReviewDetailPage';
import CompliancePage from './components/CompliancePage';
import AuditLogPage from './components/AuditLogPage';
import HelpPage from './components/HelpPage';
import OnboardingTour from './components/OnboardingTour';

const getParsedToken = () => {
  try {
    const token = localStorage.getItem('token');
    if (!token) return null;
    return JSON.parse(atob(token.split('.')[1]));
  } catch { return null; }
};

const SIDEBAR_EXPANDED = 240;
const SIDEBAR_COLLAPSED = 68;

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentPage, setCurrentPage] = useState<AppPage>('dashboard');
  const [activeReviewId, setActiveReviewId] = useState<string | null>(null);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [showTour, setShowTour] = useState(false);
  const [stats, setStats] = useState({ pending_approvals: 0 });

  const sidebarWidth = sidebarCollapsed ? SIDEBAR_COLLAPSED : SIDEBAR_EXPANDED;

  const tokenParsed = getParsedToken();
  const userName = tokenParsed?.email?.split('@')[0] || 'Enterprise User';
  const userRole = tokenParsed?.role || 'engineer';

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      setIsAuthenticated(true);
      // Check if new user needs tour
      const tourDone = localStorage.getItem('tour_completed');
      if (!tourDone) {
        setTimeout(() => setShowTour(true), 1500);
      }
    }

    // Global 401 interceptor — auto-logout on expired token
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

  // Fetch pending count for notification badge
  useEffect(() => {
    if (!isAuthenticated) return;
    const fetchStats = async () => {
      try {
        const token = localStorage.getItem('token');
        const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000';
        const res = await axios.get(`${API_BASE}/api/v1/dashboard`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        setStats(res.data);
      } catch {}
    };
    fetchStats();
    const interval = setInterval(fetchStats, 30000);
    return () => clearInterval(interval);
  }, [isAuthenticated]);

  const handleLogin = (token: string) => {
    localStorage.setItem('token', token);
    setIsAuthenticated(true);
    const tourDone = localStorage.getItem('tour_completed');
    if (!tourDone) {
      setTimeout(() => setShowTour(true), 1500);
    }
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

  const handleNavigate = (page: AppPage) => {
    if (page !== 'review-detail') setActiveReviewId(null);
    setCurrentPage(page);
  };

  const renderPage = () => {
    switch (currentPage) {
      case 'dashboard':
        return <Dashboard onViewReview={navigateToReview} onNavigate={handleNavigate} />;
      case 'upload':
        return <UploadPage onViewReview={navigateToReview} onBack={() => handleNavigate('dashboard')} />;
      case 'history':
        return <HistoryPage onBack={() => handleNavigate('dashboard')} onViewReview={navigateToReview} />;
      case 'review-detail':
        return activeReviewId
          ? <ReviewDetailPage reviewId={activeReviewId} onBack={() => handleNavigate('history')} />
          : null;
      case 'compliance':
        return <CompliancePage onViewReview={navigateToReview} />;
      case 'audit-log':
        return <AuditLogPage />;
      case 'help':
        return <HelpPage onStartTour={() => setShowTour(true)} />;
      case 'settings':
        return <HelpPage onStartTour={() => setShowTour(true)} />;
      default:
        return <Dashboard onViewReview={navigateToReview} onNavigate={handleNavigate} />;
    }
  };

  if (!isAuthenticated) {
    return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <NetworkBackground />
        <AuthPage onLogin={handleLogin} />
        <ToastContainer position="bottom-right" theme="dark" />
      </ThemeProvider>
    );
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <NetworkBackground />

      {/* Sidebar */}
      <Sidebar
        currentPage={currentPage}
        onNavigate={handleNavigate}
        onLogout={handleLogout}
        userName={userName}
        userRole={userRole}
        pendingCount={stats.pending_approvals}
      />

      {/* Top Bar */}
      <TopBar
        currentPage={currentPage}
        pendingCount={stats.pending_approvals}
        sidebarWidth={sidebarWidth}
        onRefresh={currentPage === 'dashboard' ? undefined : undefined}
      />

      {/* Main Content Area */}
      <Box
        sx={{
          ml: `${sidebarWidth}px`,
          mt: '64px',
          minHeight: 'calc(100vh - 64px)',
          transition: 'margin-left 0.25s cubic-bezier(0.4,0,0.2,1)',
          position: 'relative',
          zIndex: 1,
        }}
      >
        {renderPage()}
      </Box>

      {/* Onboarding Tour */}
      {showTour && (
        <OnboardingTour
          onComplete={() => {
            setShowTour(false);
            localStorage.setItem('tour_completed', 'true');
          }}
        />
      )}

      <ToastContainer position="bottom-right" theme="dark" />
    </ThemeProvider>
  );
}

export default App;
