// @ts-nocheck
import { useState, useEffect } from 'react';
import {
  Box, Typography, Button, Paper, Grid, AppBar, Toolbar,
  Container, IconButton, Avatar, Menu, MenuItem, Dialog,
  DialogTitle, DialogContent, DialogActions, TextField, CircularProgress,
  Chip, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Tooltip, Divider, Badge
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import NotificationsIcon from '@mui/icons-material/Notifications';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import HistoryIcon from '@mui/icons-material/History';
import VisibilityIcon from '@mui/icons-material/Visibility';
import RefreshIcon from '@mui/icons-material/Refresh';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import AssessmentIcon from '@mui/icons-material/Assessment';
import SecurityIcon from '@mui/icons-material/Security';
import WarningIcon from '@mui/icons-material/Warning';
import ShieldIcon from '@mui/icons-material/Shield';
import { toast } from 'react-toastify';
import { useDropzone } from 'react-dropzone';
import {
  Tooltip as ReTooltip, ResponsiveContainer, Cell, PieChart, Pie, Legend
} from 'recharts';
import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000';
const getParsedToken = () => {
  try {
    const token = localStorage.getItem('token');
    if (!token) return null;
    return JSON.parse(atob(token.split('.')[1]));
  } catch { return null; }
};
const getToken = () => localStorage.getItem('token');
const authHeaders = () => ({ Authorization: `Bearer ${getToken()}` });

const RISK_COLORS: Record<string, string> = {
  low: '#10B981', medium: '#F59E0B', high: '#F97316', critical: '#EF4444', unknown: '#6B7280'
};
const STATUS_COLORS: Record<string, string> = {
  pending_review: '#F59E0B', under_analysis: '#3B82F6', approved: '#10B981',
  rejected: '#EF4444', draft: '#6B7280', in_review: '#8B5CF6', failed: '#EF4444'
};

interface DashboardProps {
  onLogout: () => void;
  onViewReview: (reviewId: string) => void;
  onNavigateHistory: () => void;
}

export default function Dashboard({ onLogout, onViewReview, onNavigateHistory }: DashboardProps) {
  const tokenParsed = getParsedToken();
  const userName = tokenParsed?.email || 'Enterprise User';
  const userRole = tokenParsed?.role || 'ENGINEER';

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [stats, setStats] = useState({ total_reviews: 0, open_reviews: 0, high_risk_findings: 0, compliance_violations: 0, pending_approvals: 0 });
  const [recentReviews, setRecentReviews] = useState<any[]>([]);
  const [loadingReviews, setLoadingReviews] = useState(true);

  // Upload State
  const [oldFile, setOldFile] = useState<File | null>(null);
  const [newFile, setNewFile] = useState<File | null>(null);
  const [isReviewDialogOpen, setIsReviewDialogOpen] = useState(false);
  const [reviewTitle, setReviewTitle] = useState('');
  const [ticketId, setTicketId] = useState('');
  const [isUploading, setIsUploading] = useState(false);


  useEffect(() => {
    fetchDashboardStats();
    fetchRecentReviews();
  }, []);

  const fetchDashboardStats = async () => {
    try {
      const res = await axios.get(`${API_BASE}/api/v1/dashboard`, { headers: authHeaders() });
      setStats(res.data);
    } catch (e) {
      console.error('Failed to fetch dashboard stats', e);
    }
  };

  const fetchRecentReviews = async () => {
    setLoadingReviews(true);
    try {
      const res = await axios.get(`${API_BASE}/api/v1/reviews`, {
        headers: authHeaders(),
        params: { page: 1, size: 8 }
      });
      setRecentReviews(res.data);
    } catch (e) {
      console.error('Failed to fetch recent reviews', e);
    } finally {
      setLoadingReviews(false);
    }
  };

  const handleRefresh = () => {
    fetchDashboardStats();
    fetchRecentReviews();
    toast.info('Dashboard refreshed!');
  };

  const { getRootProps: getOldRootProps, getInputProps: getOldInputProps, isDragActive: isOldDragActive } = useDropzone({
    onDrop: (files) => { if (files.length > 0) setOldFile(files[0]); }, maxFiles: 1
  });
  const { getRootProps: getNewRootProps, getInputProps: getNewInputProps, isDragActive: isNewDragActive } = useDropzone({
    onDrop: (files) => { if (files.length > 0) setNewFile(files[0]); }, maxFiles: 1
  });

  const handleStartAnalysis = () => {
    if (!oldFile || !newFile) { toast.warning('Please upload both Old and New configurations.'); return; }
    setIsReviewDialogOpen(true);
  };

  const submitReview = async () => {
    if (!oldFile || !newFile || !reviewTitle) { toast.error('Please fill all required fields.'); return; }
    setIsUploading(true);
    try {

      const headers = authHeaders();

      // 1. Upload both config files
      const formData = new FormData();
      formData.append('old_file', oldFile);
      formData.append('new_file', newFile);
      formData.append('config_type', 'AWS_SECURITY_GROUP');
      formData.append('cloud_provider', 'AWS');
      const uploadRes = await axios.post(`${API_BASE}/api/v1/upload`, formData, { headers });
      if (!uploadRes.data?.upload_id) throw new Error('Upload failed — no upload_id returned');

      // 2. Create diff review (triggers Celery + Gemini analysis)
      const diffRes = await axios.post(`${API_BASE}/api/v1/diff`, {
        upload_id: uploadRes.data.upload_id,
        title: reviewTitle,
        ticket_id: ticketId || undefined,
        compliance_frameworks: ['CIS', 'NIST'],
        auto_approve_if_low_risk: false,
        notify_manager: true
      }, { headers });
      if (!diffRes.data?.review_id) throw new Error('Diff creation failed — no review_id returned');

      const reviewId = diffRes.data.review_id;
      // Navigate to detail page so user can watch analysis live
      setTimeout(() => onViewReview(reviewId), 1000);
    } catch (error: any) {
      const detail = error.response?.data?.detail || error.message || 'Unknown error';
      toast.error(`❌ ${detail}`);
    } finally {
      setIsUploading(false);
    }
  };

  // Pie chart for risk distribution from recent reviews
  const riskDist = ['low', 'medium', 'high', 'critical'].map(r => ({
    name: r.toUpperCase(),
    value: recentReviews.filter(rv => rv.risk_level === r).length,
    fill: RISK_COLORS[r]
  })).filter(d => d.value > 0);

  const statCards = [
    { title: 'Total Reviews', value: stats.total_reviews, color: '#3B82F6', icon: <AssessmentIcon />, sub: 'All time' },
    { title: 'Open Reviews', value: stats.open_reviews, color: '#F59E0B', icon: <TrendingUpIcon />, sub: 'Awaiting action' },
    { title: 'High Risk Findings', value: stats.high_risk_findings, color: '#EF4444', icon: <WarningIcon />, sub: 'Critical & High' },
    { title: 'Compliance Violations', value: stats.compliance_violations, color: '#F97316', icon: <SecurityIcon />, sub: 'CIS & NIST' },
    { title: 'Pending Approvals', value: stats.pending_approvals, color: '#8B5CF6', icon: <ShieldIcon />, sub: 'Needs review' },
  ];

  return (
    <Box sx={{ flexGrow: 1, zIndex: 1, position: 'relative', minHeight: '100vh', overflow: 'auto' }}>
      {/* AppBar */}
      <AppBar position="sticky" sx={{ bgcolor: 'rgba(15,23,42,0.92)', backdropFilter: 'blur(16px)', borderBottom: '1px solid rgba(255,255,255,0.08)' }} elevation={0}>
        <Toolbar>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexGrow: 1 }}>
            <SecurityIcon sx={{ color: '#3B82F6', fontSize: 28 }} />
            <Typography variant="h6" sx={{ fontWeight: 800, color: 'white', letterSpacing: '-0.5px' }}>
              NetConfig<span style={{ color: '#3B82F6' }}>AI</span>
            </Typography>
            <Chip label="Enterprise" size="small" sx={{ bgcolor: 'rgba(59,130,246,0.15)', color: '#3B82F6', fontWeight: 700, ml: 1 }} />
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Tooltip title="Refresh Dashboard">
              <IconButton onClick={handleRefresh} sx={{ color: '#94A3B8', '&:hover': { color: 'white' } }}>
                <RefreshIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="View All History">
              <IconButton onClick={onNavigateHistory} sx={{ color: '#94A3B8', '&:hover': { color: 'white' } }}>
                <Badge badgeContent={stats.total_reviews || null} color="primary" max={99}>
                  <HistoryIcon />
                </Badge>
              </IconButton>
            </Tooltip>
            <Tooltip title="Notifications">
              <IconButton sx={{ color: '#94A3B8', '&:hover': { color: 'white' } }}>
                <Badge badgeContent={stats.pending_approvals || null} color="warning">
                  <NotificationsIcon />
                </Badge>
              </IconButton>
            </Tooltip>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, ml: 1, cursor: 'pointer' }} onClick={(e) => setAnchorEl(e.currentTarget)}>
              <Box sx={{ textAlign: 'right', display: { xs: 'none', sm: 'block' } }}>
                <Typography variant="subtitle2" sx={{ lineHeight: 1.2, fontWeight: 600 }}>{userName}</Typography>
                <Typography variant="caption" sx={{ color: '#3B82F6', fontWeight: 700 }}>{userRole.toUpperCase()}</Typography>
              </Box>
              <Avatar sx={{ bgcolor: '#3B82F6', fontWeight: 700, width: 38, height: 38 }}>{userName.charAt(0).toUpperCase()}</Avatar>
            </Box>
            <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}
              PaperProps={{ sx: { bgcolor: '#1E293B', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 2 } }}>
              <MenuItem onClick={onNavigateHistory} sx={{ gap: 1.5 }}><HistoryIcon sx={{ fontSize: 18 }} /> Review History</MenuItem>
              <Divider sx={{ borderColor: 'rgba(255,255,255,0.1)' }} />
              <MenuItem onClick={onLogout} sx={{ color: '#EF4444', gap: 1.5 }}><LogoutIcon sx={{ fontSize: 18 }} /> Sign Out</MenuItem>
            </Menu>
          </Box>
        </Toolbar>
      </AppBar>

      <Container maxWidth="xl" sx={{ mt: 4, mb: 8, px: { xs: 2, md: 4 } }}>
        {/* Page Title */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', mb: 4 }}>
          <Box>
            <Typography variant="h4" sx={{ fontWeight: 800, mb: 0.5 }}>Enterprise Dashboard</Typography>
            <Typography variant="body2" sx={{ color: '#64748B' }}>
              AI-powered network configuration change review platform
            </Typography>
          </Box>
          <Button
            variant="outlined"
            startIcon={<HistoryIcon />}
            onClick={onNavigateHistory}
            sx={{ borderColor: 'rgba(255,255,255,0.2)', color: '#94A3B8', borderRadius: 2, '&:hover': { borderColor: '#3B82F6', color: '#3B82F6' } }}
          >
            View All History
          </Button>
        </Box>

        {/* Stat Cards */}
        <Grid container spacing={3} sx={{ mb: 5 }}>
          {statCards.map((stat) => (
            <Grid item xs={6} sm={4} md key={stat.title}>
              <Paper sx={{
                p: 3, borderRadius: 3,
                bgcolor: 'rgba(30,41,59,0.6)', backdropFilter: 'blur(10px)',
                border: `1px solid ${stat.color}22`,
                transition: 'transform 0.2s, box-shadow 0.2s',
                '&:hover': { transform: 'translateY(-2px)', boxShadow: `0 8px 30px ${stat.color}22` }
              }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="caption" sx={{ color: '#94A3B8', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    {stat.title}
                  </Typography>
                  <Box sx={{ color: stat.color, opacity: 0.7 }}>{stat.icon}</Box>
                </Box>
                <Typography variant="h3" sx={{ color: stat.color, fontWeight: 900, lineHeight: 1.1, mb: 0.5 }}>
                  {stat.value}
                </Typography>
                <Typography variant="caption" sx={{ color: '#64748B' }}>{stat.sub}</Typography>
              </Paper>
            </Grid>
          ))}
        </Grid>

        {/* Upload + Chart Row */}
        <Grid container spacing={4} sx={{ mb: 5 }}>
          {/* Upload Section */}
          <Grid item xs={12} lg={7}>
            <Typography variant="h5" sx={{ fontWeight: 700, mb: 3, display: 'flex', alignItems: 'center', gap: 1.5 }}>
              <CloudUploadIcon sx={{ color: '#3B82F6' }} />
              Start New Analysis
            </Typography>
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Paper {...getOldRootProps()} sx={{
                  p: 4, borderRadius: 3, textAlign: 'center', cursor: 'pointer', transition: 'all 0.2s',
                  bgcolor: isOldDragActive ? 'rgba(59,130,246,0.1)' : 'rgba(30,41,59,0.6)',
                  border: `2px dashed ${oldFile ? '#10B981' : isOldDragActive ? '#3B82F6' : 'rgba(255,255,255,0.15)'}`,
                  '&:hover': { borderColor: '#3B82F6', bgcolor: 'rgba(59,130,246,0.05)' }
                }}>
                  <input {...getOldInputProps()} />
                  {oldFile ? (
                    <Box>
                      <CheckCircleOutlineIcon sx={{ fontSize: 44, color: '#10B981', mb: 1 }} />
                      <Typography variant="h6" sx={{ color: '#10B981', fontWeight: 600 }}>Old Config Ready</Typography>
                      <Typography variant="caption" sx={{ color: '#94A3B8' }}>{oldFile.name}</Typography>
                      <br />
                      <Typography variant="caption" sx={{ color: '#64748B' }}>{(oldFile.size / 1024).toFixed(1)} KB</Typography>
                    </Box>
                  ) : (
                    <Box>
                      <CloudUploadIcon sx={{ fontSize: 44, color: '#475569', mb: 1 }} />
                      <Typography variant="h6" sx={{ color: '#E2E8F0' }}>Old Configuration</Typography>
                      <Typography variant="body2" sx={{ color: '#64748B' }}>Baseline / Current State</Typography>
                      <Typography variant="caption" sx={{ color: '#475569', mt: 1, display: 'block' }}>
                        Drop file or click to browse
                      </Typography>
                    </Box>
                  )}
                </Paper>
              </Grid>
              <Grid item xs={12} md={6}>
                <Paper {...getNewRootProps()} sx={{
                  p: 4, borderRadius: 3, textAlign: 'center', cursor: 'pointer', transition: 'all 0.2s',
                  bgcolor: isNewDragActive ? 'rgba(59,130,246,0.1)' : 'rgba(30,41,59,0.6)',
                  border: `2px dashed ${newFile ? '#10B981' : isNewDragActive ? '#3B82F6' : 'rgba(255,255,255,0.15)'}`,
                  '&:hover': { borderColor: '#3B82F6', bgcolor: 'rgba(59,130,246,0.05)' }
                }}>
                  <input {...getNewInputProps()} />
                  {newFile ? (
                    <Box>
                      <CheckCircleOutlineIcon sx={{ fontSize: 44, color: '#10B981', mb: 1 }} />
                      <Typography variant="h6" sx={{ color: '#10B981', fontWeight: 600 }}>New Config Ready</Typography>
                      <Typography variant="caption" sx={{ color: '#94A3B8' }}>{newFile.name}</Typography>
                      <br />
                      <Typography variant="caption" sx={{ color: '#64748B' }}>{(newFile.size / 1024).toFixed(1)} KB</Typography>
                    </Box>
                  ) : (
                    <Box>
                      <CloudUploadIcon sx={{ fontSize: 44, color: '#475569', mb: 1 }} />
                      <Typography variant="h6" sx={{ color: '#E2E8F0' }}>New Configuration</Typography>
                      <Typography variant="body2" sx={{ color: '#64748B' }}>Proposed Changes</Typography>
                      <Typography variant="caption" sx={{ color: '#475569', mt: 1, display: 'block' }}>
                        Drop file or click to browse
                      </Typography>
                    </Box>
                  )}
                </Paper>
              </Grid>
            </Grid>
            <Box sx={{ mt: 3, display: 'flex', gap: 2, alignItems: 'center' }}>
              <Button
                variant="contained"
                size="large"
                disabled={!oldFile || !newFile}
                onClick={handleStartAnalysis}
                sx={{
                  bgcolor: '#3B82F6', px: 6, py: 1.5, borderRadius: 2, fontWeight: 700,
                  fontSize: '1rem', boxShadow: '0 4px 20px rgba(59,130,246,0.4)',
                  '&:hover': { bgcolor: '#2563EB', boxShadow: '0 6px 25px rgba(59,130,246,0.5)' },
                  '&:disabled': { bgcolor: 'rgba(255,255,255,0.1)', color: '#475569' }
                }}
              >
                🚀 Submit & Analyze
              </Button>
              {(oldFile || newFile) && (
                <Button
                  variant="text"
                  onClick={() => { setOldFile(null); setNewFile(null); }}
                  sx={{ color: '#64748B', '&:hover': { color: '#94A3B8' } }}
                >
                  Clear
                </Button>
              )}
            </Box>
          </Grid>

          {/* Risk Distribution */}
          <Grid item xs={12} lg={5}>
            <Paper sx={{ p: 3, bgcolor: 'rgba(30,41,59,0.6)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 3, height: '100%', minHeight: 280 }}>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>Risk Distribution</Typography>
              {riskDist.length === 0 ? (
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: 200, color: '#475569' }}>
                  <AssessmentIcon sx={{ fontSize: 48, mb: 2, opacity: 0.5 }} />
                  <Typography variant="body2">No reviews yet. Your risk dashboard will appear here.</Typography>
                </Box>
              ) : (
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie data={riskDist} cx="50%" cy="50%" outerRadius={80} dataKey="value"
                      label={({ name, value }) => `${name}: ${value}`} labelLine={false}>
                      {riskDist.map((e, i) => <Cell key={i} fill={e.fill} />)}
                    </Pie>
                    <ReTooltip contentStyle={{ background: '#1E293B', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8 }} />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </Paper>
          </Grid>
        </Grid>

        {/* Recent Reviews Table */}
        <Box sx={{ mb: 4 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
            <Typography variant="h5" sx={{ fontWeight: 700 }}>Recent Reviews</Typography>
            <Button
              variant="outlined"
              size="small"
              startIcon={<HistoryIcon />}
              onClick={onNavigateHistory}
              sx={{ borderColor: 'rgba(255,255,255,0.2)', color: '#94A3B8', borderRadius: 2 }}
            >
              View All
            </Button>
          </Box>

          <Paper sx={{ bgcolor: 'rgba(30,41,59,0.6)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 3, overflow: 'hidden' }}>
            {loadingReviews ? (
              <Box sx={{ p: 6, textAlign: 'center' }}>
                <CircularProgress size={32} sx={{ color: '#3B82F6' }} />
              </Box>
            ) : recentReviews.length === 0 ? (
              <Box sx={{ p: 8, textAlign: 'center' }}>
                <AssessmentIcon sx={{ fontSize: 56, color: '#334155', mb: 2 }} />
                <Typography variant="h6" sx={{ color: '#64748B', mb: 1 }}>No reviews yet</Typography>
                <Typography variant="body2" sx={{ color: '#475569' }}>
                  Upload your first configuration files above to generate your first AI analysis.
                </Typography>
              </Box>
            ) : (
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow sx={{ bgcolor: 'rgba(255,255,255,0.02)' }}>
                      {['Title', 'Status', 'Risk Level', 'Risk Score', 'Compliance', 'Created', ''].map(h => (
                        <TableCell key={h} sx={{ color: '#475569', fontWeight: 700, fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', borderColor: 'rgba(255,255,255,0.06)' }}>
                          {h}
                        </TableCell>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {recentReviews.map((r) => {
                      const riskColor = RISK_COLORS[r.risk_level] || '#6B7280';
                      const statusColor = STATUS_COLORS[r.status] || '#6B7280';
                      return (
                        <TableRow key={r.id}
                          onClick={() => onViewReview(r.id)}
                          sx={{ cursor: 'pointer', '&:hover': { bgcolor: 'rgba(59,130,246,0.06)' }, borderBottom: '1px solid rgba(255,255,255,0.04)' }}>
                          <TableCell sx={{ borderColor: 'rgba(255,255,255,0.04)' }}>
                            <Typography variant="body2" sx={{ fontWeight: 600, color: '#E2E8F0', mb: 0.3 }}>{r.title}</Typography>
                            {r.ticket_id && <Typography variant="caption" sx={{ color: '#475569' }}>#{r.ticket_id}</Typography>}
                          </TableCell>
                          <TableCell sx={{ borderColor: 'rgba(255,255,255,0.04)' }}>
                            <Chip label={r.status?.replace('_', ' ').toUpperCase()} size="small"
                              sx={{ bgcolor: statusColor + '22', color: statusColor, fontWeight: 700, fontSize: '0.68rem' }} />
                          </TableCell>
                          <TableCell sx={{ borderColor: 'rgba(255,255,255,0.04)' }}>
                            <Chip label={r.risk_level?.toUpperCase() || 'UNKNOWN'} size="small"
                              sx={{ bgcolor: riskColor + '22', color: riskColor, fontWeight: 700, fontSize: '0.68rem' }} />
                          </TableCell>
                          <TableCell sx={{ borderColor: 'rgba(255,255,255,0.04)', color: riskColor, fontWeight: 700 }}>
                            {r.overall_risk_score != null ? `${Math.round(r.overall_risk_score)}/100` : '—'}
                          </TableCell>
                          <TableCell sx={{ borderColor: 'rgba(255,255,255,0.04)', color: '#10B981', fontWeight: 600 }}>
                            {r.compliance_score != null ? `${Math.round(r.compliance_score)}%` : '—'}
                          </TableCell>
                          <TableCell sx={{ borderColor: 'rgba(255,255,255,0.04)', color: '#64748B', fontSize: '0.78rem' }}>
                            {r.created_at ? new Date(r.created_at).toLocaleDateString() : '—'}
                          </TableCell>
                          <TableCell sx={{ borderColor: 'rgba(255,255,255,0.04)' }}>
                            <Tooltip title="View Details">
                              <IconButton size="small" sx={{ color: '#3B82F6' }} onClick={(e) => { e.stopPropagation(); onViewReview(r.id); }}>
                                <VisibilityIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Paper>
        </Box>
      </Container>

      {/* Review Setup Dialog */}
      <Dialog
        open={isReviewDialogOpen}
        onClose={() => !isUploading && setIsReviewDialogOpen(false)}
        PaperProps={{ sx: { bgcolor: '#1E293B', color: 'white', minWidth: 420, borderRadius: 3, border: '1px solid rgba(255,255,255,0.1)' } }}
      >
        <DialogTitle sx={{ fontWeight: 700, fontSize: '1.2rem' }}>
          Review Details
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ color: '#94A3B8', mb: 3 }}>
            Provide context for this configuration change analysis.
          </Typography>
          <TextField
            autoFocus margin="dense" label="Review Title *" type="text" fullWidth variant="outlined"
            value={reviewTitle} onChange={(e) => setReviewTitle(e.target.value)} disabled={isUploading}
            placeholder="e.g. Firewall rule update for prod-vpc-01"
            sx={{ mb: 2, input: { color: 'white' }, label: { color: '#94A3B8' }, fieldset: { borderColor: 'rgba(255,255,255,0.2)' } }}
          />
          <TextField
            margin="dense" label="Ticket ID (Optional)" type="text" fullWidth variant="outlined"
            value={ticketId} onChange={(e) => setTicketId(e.target.value)} disabled={isUploading}
            placeholder="e.g. NETOPS-1234"
            sx={{ input: { color: 'white' }, label: { color: '#94A3B8' }, fieldset: { borderColor: 'rgba(255,255,255,0.2)' } }}
          />
          <Box sx={{ mt: 3, p: 2, bgcolor: 'rgba(59,130,246,0.1)', borderRadius: 2, border: '1px solid rgba(59,130,246,0.3)' }}>
            <Typography variant="caption" sx={{ color: '#3B82F6', fontWeight: 600 }}>
              🤖 Gemini AI will analyze your configuration changes for security risks, compliance violations, and generate an executive report.
            </Typography>
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 3, gap: 1 }}>
          <Button onClick={() => setIsReviewDialogOpen(false)} disabled={isUploading} sx={{ color: '#94A3B8', borderRadius: 2 }}>
            Cancel
          </Button>
          <Button
            onClick={submitReview}
            variant="contained"
            disabled={isUploading || !reviewTitle}
            sx={{ bgcolor: '#3B82F6', borderRadius: 2, fontWeight: 700, px: 4, '&:hover': { bgcolor: '#2563EB' } }}
          >
            {isUploading ? <CircularProgress size={22} color="inherit" /> : '🚀 Submit & Analyze'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
