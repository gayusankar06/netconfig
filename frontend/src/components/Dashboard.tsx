import React, { useState } from 'react';
import { 
  Box, Typography, Button, Paper, Grid, AppBar, Toolbar, 
  Container, IconButton, Avatar, Menu, MenuItem, Chip
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import NotificationsIcon from '@mui/icons-material/Notifications';
import SettingsIcon from '@mui/icons-material/Settings';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import { toast } from 'react-toastify';
import { useDropzone } from 'react-dropzone';
import { 
  LineChart, Line, AreaChart, Area, BarChart, Bar, PieChart, Pie, 
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell 
} from 'recharts';
import { getToken, getParsedToken } from '../keycloak';
import axios from 'axios';

// Mock Data for Charts
const riskTrendData = [
  { name: 'Mon', high: 4, med: 10, low: 24 },
  { name: 'Tue', high: 3, med: 8, low: 30 },
  { name: 'Wed', high: 5, med: 12, low: 28 },
  { name: 'Thu', high: 2, med: 5, low: 35 },
  { name: 'Fri', high: 1, med: 3, low: 40 },
  { name: 'Sat', high: 0, med: 2, low: 20 },
  { name: 'Sun', high: 2, med: 6, low: 25 },
];

const complianceData = [
  { name: 'Week 1', score: 85 },
  { name: 'Week 2', score: 88 },
  { name: 'Week 3', score: 92 },
  { name: 'Week 4', score: 96 },
];

const statusData = [
  { name: 'Pending', value: 12, color: '#F59E0B' },
  { name: 'Approved', value: 45, color: '#10B981' },
  { name: 'Rejected', value: 3, color: '#EF4444' },
];

export default function Dashboard({ onLogout }: { onLogout: () => void }) {
  const tokenParsed = getParsedToken();
  const userName = tokenParsed?.name || tokenParsed?.preferred_username || 'Enterprise User';
  const userRole = tokenParsed?.realm_access?.roles?.[0] || 'NETWORK_ENGINEER';

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const onDrop = async (acceptedFiles: File[]) => {
    if (acceptedFiles.length === 0) return;
    const file = acceptedFiles[0];
    
    const formData = new FormData();
    formData.append('file', file);
    
    toast.info(`Uploading ${file.name}...`);
    try {
      await axios.post(
        `${import.meta.env.VITE_API_BASE_URL}/api/v1/upload`, 
        formData, 
        {
          headers: {
            'Content-Type': 'multipart/form-data',
            'Authorization': `Bearer ${getToken()}`
          }
        }
      );
      toast.success(`${file.name} uploaded successfully and analysis started.`);
    } catch (error: any) {
      toast.error(error.response?.data?.detail || "Error uploading file");
    }
  };

  const { getRootProps, getInputProps, isDragActive } = useDropzone({ onDrop });

  const StatCard = ({ title, value, subValue, color }: any) => (
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
      {subValue && <Typography variant="caption" sx={{ color: 'text.secondary', mt: 1 }}>{subValue}</Typography>}
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
          <Typography variant="h4" sx={{ fontWeight: 700 }}>Executive Overview</Typography>
          <Button variant="contained" startIcon={<CloudUploadIcon />} sx={{ bgcolor: '#3B82F6', borderRadius: 2 }}>
            New Review
          </Button>
        </Box>

        {/* Stats Row */}
        <Grid container spacing={3} sx={{ mb: 4 }}>
          <Grid item xs={12} sm={6} md={3}><StatCard title="Total Reviews (30d)" value="1,284" subValue="+12% from last month" color="#fff" /></Grid>
          <Grid item xs={12} sm={6} md={3}><StatCard title="High Risk Findings" value="12" subValue="-4 since yesterday" color="#EF4444" /></Grid>
          <Grid item xs={12} sm={6} md={3}><StatCard title="Compliance Score" value="96%" subValue="Target: 99%" color="#10B981" /></Grid>
          <Grid item xs={12} sm={6} md={3}><StatCard title="Pending Approvals" value="8" subValue="3 critical" color="#F59E0B" /></Grid>
        </Grid>

        {/* Charts Row */}
        <Grid container spacing={3}>
          <Grid item xs={12} lg={8}>
            <Paper sx={{ p: 3, borderRadius: 4, bgcolor: 'rgba(30, 41, 59, 0.6)', border: '1px solid rgba(255,255,255,0.05)', height: 400 }}>
              <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>Risk Trend Analysis</Typography>
              <ResponsiveContainer width="100%" height="85%">
                <AreaChart data={riskTrendData}>
                  <defs>
                    <linearGradient id="colorHigh" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#EF4444" stopOpacity={0.3}/>
                      <stop offset="95%" stopColor="#EF4444" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
                  <XAxis dataKey="name" stroke="#94A3B8" />
                  <YAxis stroke="#94A3B8" />
                  <Tooltip contentStyle={{ backgroundColor: '#1E293B', border: 'none', borderRadius: 8 }} />
                  <Area type="monotone" dataKey="high" stroke="#EF4444" fillOpacity={1} fill="url(#colorHigh)" />
                  <Area type="monotone" dataKey="med" stroke="#F59E0B" fillOpacity={0.1} fill="#F59E0B" />
                  <Area type="monotone" dataKey="low" stroke="#3B82F6" fillOpacity={0.1} fill="#3B82F6" />
                </AreaChart>
              </ResponsiveContainer>
            </Paper>
          </Grid>
          <Grid item xs={12} lg={4}>
            <Paper sx={{ p: 3, borderRadius: 4, bgcolor: 'rgba(30, 41, 59, 0.6)', border: '1px solid rgba(255,255,255,0.05)', height: 400 }}>
              <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>Reviews By Status</Typography>
              <ResponsiveContainer width="100%" height="85%">
                <PieChart>
                  <Pie data={statusData} innerRadius={80} outerRadius={110} paddingAngle={5} dataKey="value">
                    {statusData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ backgroundColor: '#1E293B', border: 'none', borderRadius: 8 }} />
                </PieChart>
              </ResponsiveContainer>
              <Box sx={{ display: 'flex', justifyContent: 'center', gap: 2, mt: -2 }}>
                {statusData.map(d => (
                  <Box key={d.name} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: d.color }} />
                    <Typography variant="caption" sx={{ color: '#94A3B8' }}>{d.name}</Typography>
                  </Box>
                ))}
              </Box>
            </Paper>
          </Grid>
        </Grid>

        {/* Upload Section */}
        <Box sx={{ mt: 4 }}>
          <Paper 
            {...getRootProps()}
            sx={{ 
              p: 6, 
              borderRadius: 4, 
              bgcolor: isDragActive ? 'rgba(59, 130, 246, 0.1)' : 'rgba(30, 41, 59, 0.6)',
              border: `2px dashed ${isDragActive ? '#3B82F6' : 'rgba(255,255,255,0.1)'}`,
              textAlign: 'center',
              cursor: 'pointer',
              transition: 'all 0.2s ease',
              '&:hover': { borderColor: '#3B82F6', bgcolor: 'rgba(59, 130, 246, 0.05)' }
            }}
          >
            <input {...getInputProps()} />
            <CloudUploadIcon sx={{ fontSize: 64, color: isDragActive ? '#3B82F6' : '#64748B', mb: 2 }} />
            <Typography variant="h5" sx={{ fontWeight: 600, mb: 1 }}>
              {isDragActive ? "Drop Configuration File" : "Upload Configuration for Analysis"}
            </Typography>
            <Typography variant="body1" sx={{ color: '#94A3B8', maxWidth: 600, mx: 'auto' }}>
              Drag and drop your JSON, YAML, or Terraform diffs here. Our AI engine will immediately begin security and compliance evaluation.
            </Typography>
          </Paper>
        </Box>
      </Container>
    </Box>
  );
}
