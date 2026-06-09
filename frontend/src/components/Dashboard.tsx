import React, { useState, useEffect } from 'react';
import { 
  Box, Typography, Button, Paper, Grid, AppBar, Toolbar, 
  Container, IconButton, Avatar, Menu, MenuItem, Dialog,
  DialogTitle, DialogContent, DialogActions, TextField, CircularProgress
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import NotificationsIcon from '@mui/icons-material/Notifications';
import SettingsIcon from '@mui/icons-material/Settings';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import { toast } from 'react-toastify';
import { useDropzone } from 'react-dropzone';
import { 
  LineChart, Line, AreaChart, Area, BarChart, Bar, PieChart, Pie, 
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell 
} from 'recharts';
import axios from 'axios';

const getParsedToken = () => {
  try {
      const token = localStorage.getItem('token');
      if (!token) return null;
      return JSON.parse(atob(token.split('.')[1]));
  } catch (e) {
      return null;
  }
};
const getToken = () => localStorage.getItem('token');

export default function Dashboard({ onLogout }: { onLogout: () => void }) {
  const tokenParsed = getParsedToken();
  const userName = tokenParsed?.email || 'Enterprise User';
  const userRole = tokenParsed?.role || 'NETWORK_ENGINEER';

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  
  // Dashboard Stats State
  const [stats, setStats] = useState({
    total_reviews: 0,
    open_reviews: 0,
    high_risk_findings: 0,
    compliance_violations: 0,
    pending_approvals: 0
  });

  // Upload State
  const [oldFile, setOldFile] = useState<File | null>(null);
  const [newFile, setNewFile] = useState<File | null>(null);
  const [isReviewDialogOpen, setIsReviewDialogOpen] = useState(false);
  const [reviewTitle, setReviewTitle] = useState('');
  const [ticketId, setTicketId] = useState('');
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    fetchDashboardStats();
  }, []);

  const fetchDashboardStats = async () => {
    try {
      const response = await axios.get(`${import.meta.env.VITE_API_BASE_URL}/api/v1/dashboard`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      });
      setStats(response.data);
    } catch (error) {
      console.error("Failed to fetch dashboard stats", error);
    }
  };

  const handleMenu = (event: React.MouseEvent<HTMLElement>) => setAnchorEl(event.currentTarget);
  const handleClose = () => setAnchorEl(null);

  const { getRootProps: getOldRootProps, getInputProps: getOldInputProps, isDragActive: isOldDragActive } = useDropzone({ 
    onDrop: (files) => { if (files.length > 0) setOldFile(files[0]); },
    maxFiles: 1
  });

  const { getRootProps: getNewRootProps, getInputProps: getNewInputProps, isDragActive: isNewDragActive } = useDropzone({ 
    onDrop: (files) => { if (files.length > 0) setNewFile(files[0]); },
    maxFiles: 1
  });

  const handleStartAnalysis = () => {
    if (!oldFile || !newFile) {
      toast.warning("Please upload both Old and New configurations.");
      return;
    }
    setIsReviewDialogOpen(true);
  };

  const submitReview = async () => {
    if (!oldFile || !newFile || !reviewTitle) {
      toast.error("Please fill all required fields.");
      return;
    }

    setIsUploading(true);
    try {
      // 1. Upload Files
      const formData = new FormData();
      formData.append('old_file', oldFile);
      formData.append('new_file', newFile);
      formData.append('config_type', 'AWS_SECURITY_GROUP');
      formData.append('cloud_provider', 'AWS');

      const uploadRes = await axios.post(
        `${import.meta.env.VITE_API_BASE_URL}/api/v1/upload`, 
        formData, 
        { headers: { 'Authorization': `Bearer ${getToken()}` } }
      );
      
      const uploadId = uploadRes.data.upload_id;

      // 2. Trigger Diff Analysis
      await axios.post(
        `${import.meta.env.VITE_API_BASE_URL}/api/v1/diff`, 
        {
          upload_id: uploadId,
          title: reviewTitle,
          ticket_id: ticketId,
          compliance_frameworks: ["CIS", "NIST"],
          auto_approve_if_low_risk: true,
          notify_manager: true
        },
        { headers: { 'Authorization': `Bearer ${getToken()}` } }
      );

      toast.success("Review created and analysis started successfully!");
      setIsReviewDialogOpen(false);
      setOldFile(null);
      setNewFile(null);
      setReviewTitle('');
      setTicketId('');
      
      // Refresh Stats
      fetchDashboardStats();
    } catch (error: any) {
      toast.error(error.response?.data?.detail || "Error triggering analysis");
    } finally {
      setIsUploading(false);
    }
  };

  const StatCard = ({ title, value, color }: any) => (
    <Paper sx={{ 
      p: 3, 
      display: 'flex', 
      flexDirection: 'column', 
      bgcolor: 'rgba(30, 41, 59, 0.6)',
      backdropFilter: 'blur(10px)',
      border: '1px solid rgba(255,255,255,0.05)',
      borderRadius: 4
    }}>
      <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 600 }}>{title}</Typography>
      <Typography variant="h3" sx={{ color: color || 'white', mt: 1, fontWeight: 700 }}>{value}</Typography>
    </Paper>
  );

  return (
    <Box sx={{ flexGrow: 1, zIndex: 1, position: 'relative', height: '100vh', overflow: 'auto' }}>
      <AppBar position="sticky" sx={{ bgcolor: 'rgba(15, 23, 42, 0.8)', backdropFilter: 'blur(12px)', borderBottom: '1px solid rgba(255,255,255,0.1)' }} elevation={0}>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1, fontWeight: 700, color: 'white' }}>
            NetConfig<span style={{color: '#3B82F6'}}>AI</span>
          </Typography>
          
          <IconButton color="inherit" sx={{ mr: 2 }}>
            <NotificationsIcon />
          </IconButton>
          
          <Box sx={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }} onClick={handleMenu}>
            <Box sx={{ textAlign: 'right', mr: 2, display: { xs: 'none', sm: 'block' } }}>
              <Typography variant="subtitle2" sx={{ lineHeight: 1.2 }}>{userName}</Typography>
              <Typography variant="caption" sx={{ color: '#3B82F6', fontWeight: 600 }}>{userRole}</Typography>
            </Box>
            <Avatar sx={{ bgcolor: '#3B82F6' }}>{userName.charAt(0).toUpperCase()}</Avatar>
          </Box>
          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleClose}
            PaperProps={{ sx: { bgcolor: '#1E293B', border: '1px solid rgba(255,255,255,0.1)' } }}
          >
            <MenuItem onClick={handleClose}><SettingsIcon sx={{ mr: 2, fontSize: 20 }}/> Settings</MenuItem>
            <MenuItem onClick={onLogout}><LogoutIcon sx={{ mr: 2, fontSize: 20, color: '#EF4444' }}/> Sign Out</MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Container maxWidth="xl" sx={{ mt: 4, mb: 8 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>Enterprise Dashboard</Typography>
        </Box>

        {/* Stats Row */}
        <Grid container spacing={3} sx={{ mb: 4 }}>
          <Grid item xs={12} sm={6} md={3}><StatCard title="Total Reviews" value={stats.total_reviews} color="#3B82F6" /></Grid>
          <Grid item xs={12} sm={6} md={3}><StatCard title="Open Reviews" value={stats.open_reviews} color="#F59E0B" /></Grid>
          <Grid item xs={12} sm={6} md={3}><StatCard title="High Risk Findings" value={stats.high_risk_findings} color="#EF4444" /></Grid>
          <Grid item xs={12} sm={6} md={3}><StatCard title="Compliance Violations" value={stats.compliance_violations} color="#10B981" /></Grid>
        </Grid>

        {/* Upload Section */}
        <Box sx={{ mt: 4, mb: 6 }}>
          <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>Start New Differential Analysis</Typography>
          
          <Grid container spacing={4}>
            <Grid item xs={12} md={6}>
              <Paper 
                {...getOldRootProps()}
                sx={{ 
                  p: 4, 
                  borderRadius: 4, 
                  bgcolor: isOldDragActive ? 'rgba(59, 130, 246, 0.1)' : 'rgba(30, 41, 59, 0.6)',
                  border: `2px dashed ${oldFile ? '#10B981' : (isOldDragActive ? '#3B82F6' : 'rgba(255,255,255,0.2)')}`,
                  textAlign: 'center',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  '&:hover': { borderColor: oldFile ? '#10B981' : '#3B82F6' }
                }}
              >
                <input {...getOldInputProps()} />
                {oldFile ? (
                  <Box>
                    <CheckCircleOutlineIcon sx={{ fontSize: 48, color: '#10B981', mb: 1 }} />
                    <Typography variant="h6" sx={{ color: '#10B981' }}>Old Config Selected</Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>{oldFile.name}</Typography>
                  </Box>
                ) : (
                  <Box>
                    <CloudUploadIcon sx={{ fontSize: 48, color: '#94A3B8', mb: 1 }} />
                    <Typography variant="h6">Upload Old Configuration</Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>Baseline / Previous State</Typography>
                  </Box>
                )}
              </Paper>
            </Grid>
            <Grid item xs={12} md={6}>
               <Paper 
                {...getNewRootProps()}
                sx={{ 
                  p: 4, 
                  borderRadius: 4, 
                  bgcolor: isNewDragActive ? 'rgba(59, 130, 246, 0.1)' : 'rgba(30, 41, 59, 0.6)',
                  border: `2px dashed ${newFile ? '#10B981' : (isNewDragActive ? '#3B82F6' : 'rgba(255,255,255,0.2)')}`,
                  textAlign: 'center',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  '&:hover': { borderColor: newFile ? '#10B981' : '#3B82F6' }
                }}
              >
                <input {...getNewInputProps()} />
                {newFile ? (
                  <Box>
                    <CheckCircleOutlineIcon sx={{ fontSize: 48, color: '#10B981', mb: 1 }} />
                    <Typography variant="h6" sx={{ color: '#10B981' }}>New Config Selected</Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>{newFile.name}</Typography>
                  </Box>
                ) : (
                  <Box>
                    <CloudUploadIcon sx={{ fontSize: 48, color: '#94A3B8', mb: 1 }} />
                    <Typography variant="h6">Upload New Configuration</Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>Proposed Changes</Typography>
                  </Box>
                )}
              </Paper>
            </Grid>
          </Grid>

          <Box sx={{ mt: 4, display: 'flex', justifyContent: 'flex-end' }}>
             <Button 
                variant="contained" 
                size="large"
                disabled={!oldFile || !newFile}
                onClick={handleStartAnalysis}
                sx={{ bgcolor: '#3B82F6', px: 6, py: 1.5, borderRadius: 2 }}
              >
               Start AI Analysis
             </Button>
          </Box>
        </Box>

        {/* Dynamic empty state for charts if no reviews */}
        {stats.total_reviews === 0 && (
          <Paper sx={{ p: 6, textAlign: 'center', bgcolor: 'rgba(30, 41, 59, 0.6)', borderRadius: 4, border: '1px solid rgba(255,255,255,0.05)' }}>
            <Typography variant="h5" sx={{ color: '#94A3B8', mb: 2 }}>No reviews have been processed yet.</Typography>
            <Typography variant="body1" sx={{ color: '#64748B' }}>Upload your first set of configurations above to generate enterprise analytics.</Typography>
          </Paper>
        )}

      </Container>

      {/* Review Setup Dialog */}
      <Dialog 
        open={isReviewDialogOpen} 
        onClose={() => !isUploading && setIsReviewDialogOpen(false)}
        PaperProps={{ sx: { bgcolor: '#1E293B', color: 'white', minWidth: '400px', borderRadius: 3 } }}
      >
        <DialogTitle sx={{ fontWeight: 700 }}>Review Details</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ color: '#94A3B8', mb: 3 }}>
            Please provide context for this network configuration change.
          </Typography>
          <TextField
            autoFocus
            margin="dense"
            label="Review Title"
            type="text"
            fullWidth
            required
            variant="outlined"
            value={reviewTitle}
            onChange={(e) => setReviewTitle(e.target.value)}
            disabled={isUploading}
            sx={{ mb: 2, input: { color: 'white' }, label: { color: '#94A3B8' }, fieldset: { borderColor: 'rgba(255,255,255,0.2)' } }}
          />
          <TextField
            margin="dense"
            label="Ticket ID (Optional)"
            type="text"
            fullWidth
            variant="outlined"
            value={ticketId}
            onChange={(e) => setTicketId(e.target.value)}
            disabled={isUploading}
            sx={{ input: { color: 'white' }, label: { color: '#94A3B8' }, fieldset: { borderColor: 'rgba(255,255,255,0.2)' } }}
          />
        </DialogContent>
        <DialogActions sx={{ p: 3 }}>
          <Button onClick={() => setIsReviewDialogOpen(false)} disabled={isUploading} sx={{ color: '#94A3B8' }}>
            Cancel
          </Button>
          <Button 
            onClick={submitReview} 
            variant="contained" 
            disabled={isUploading || !reviewTitle}
            sx={{ bgcolor: '#3B82F6' }}
          >
            {isUploading ? <CircularProgress size={24} color="inherit" /> : "Submit & Analyze"}
          </Button>
        </DialogActions>
      </Dialog>

    </Box>
  );
}
