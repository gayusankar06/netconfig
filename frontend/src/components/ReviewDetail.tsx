import React from 'react';
import { Box, Typography, Paper, Grid, Chip, Button, Divider, LinearProgress } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningIcon from '@mui/icons-material/Warning';
import AssignmentTurnedInIcon from '@mui/icons-material/AssignmentTurnedIn';

// Mock Diff Data
const diffLines = [
  { type: 'context', oldLine: 45, newLine: 45, content: ' interface GigabitEthernet0/1' },
  { type: 'context', oldLine: 46, newLine: 46, content: '  description Uplink to Core' },
  { type: 'removed', oldLine: 47, newLine: null, content: '- ip address 10.0.1.1 255.255.255.0' },
  { type: 'added', oldLine: null, newLine: 47, content: '+ ip address 10.0.1.2 255.255.255.0' },
  { type: 'context', oldLine: 48, newLine: 48, content: '  no shutdown' },
];

export default function ReviewDetail() {
  return (
    <Box sx={{ p: 4, bgcolor: '#0F172A', minHeight: '100vh', color: 'white' }}>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 4 }}>Review #REV-8492</Typography>

      <Grid container spacing={4}>
        {/* Left Side: GitHub-style Diff Viewer */}
        <Grid item xs={12} lg={7}>
          <Paper sx={{ p: 0, borderRadius: 3, bgcolor: '#1E293B', overflow: 'hidden', border: '1px solid rgba(255,255,255,0.1)' }}>
            <Box sx={{ bgcolor: '#0F172A', p: 2, borderBottom: '1px solid rgba(255,255,255,0.1)', display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="subtitle2" sx={{ fontFamily: 'monospace' }}>router-core-ny.cfg</Typography>
              <Chip label="1 addition, 1 deletion" size="small" sx={{ bgcolor: 'rgba(255,255,255,0.1)', color: '#94A3B8' }} />
            </Box>
            <Box sx={{ fontFamily: 'Consolas, monospace', fontSize: '0.875rem' }}>
              {diffLines.map((line, idx) => (
                <Box key={idx} sx={{ 
                  display: 'flex', 
                  bgcolor: line.type === 'added' ? 'rgba(16, 185, 129, 0.15)' : line.type === 'removed' ? 'rgba(239, 68, 68, 0.15)' : 'transparent',
                  '&:hover': { bgcolor: 'rgba(255,255,255,0.05)' }
                }}>
                  <Box sx={{ width: 40, color: '#64748B', textAlign: 'right', pr: 1, userSelect: 'none', borderRight: '1px solid rgba(255,255,255,0.1)' }}>
                    {line.oldLine || ' '}
                  </Box>
                  <Box sx={{ width: 40, color: '#64748B', textAlign: 'right', pr: 1, userSelect: 'none', borderRight: '1px solid rgba(255,255,255,0.1)' }}>
                    {line.newLine || ' '}
                  </Box>
                  <Box sx={{ px: 2, whiteSpace: 'pre', color: line.type === 'added' ? '#10B981' : line.type === 'removed' ? '#EF4444' : '#E2E8F0' }}>
                    {line.content}
                  </Box>
                </Box>
              ))}
            </Box>
          </Paper>
        </Grid>

        {/* Right Side: AI Executive Report */}
        <Grid item xs={12} lg={5}>
          <Paper sx={{ p: 4, borderRadius: 3, bgcolor: 'rgba(30, 41, 59, 0.6)', backdropFilter: 'blur(10px)', border: '1px solid rgba(255,255,255,0.1)' }}>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 3 }}>AI Executive Report</Typography>
            
            <Box sx={{ display: 'flex', gap: 3, mb: 4 }}>
              <Box sx={{ flex: 1 }}>
                <Typography variant="body2" sx={{ color: '#94A3B8', mb: 1 }}>Risk Score</Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Typography variant="h3" sx={{ color: '#F59E0B', fontWeight: 700 }}>68</Typography>
                  <LinearProgress variant="determinate" value={68} color="warning" sx={{ flex: 1, height: 8, borderRadius: 4 }} />
                </Box>
              </Box>
              <Box sx={{ flex: 1 }}>
                <Typography variant="body2" sx={{ color: '#94A3B8', mb: 1 }}>Compliance Score</Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Typography variant="h3" sx={{ color: '#10B981', fontWeight: 700 }}>92</Typography>
                  <LinearProgress variant="determinate" value={92} color="success" sx={{ flex: 1, height: 8, borderRadius: 4 }} />
                </Box>
              </Box>
            </Box>

            <Divider sx={{ borderColor: 'rgba(255,255,255,0.1)', mb: 3 }} />

            <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>Top Findings</Typography>
            <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2, mb: 2 }}>
              <WarningIcon sx={{ color: '#F59E0B', mt: 0.5 }} />
              <Box>
                <Typography variant="body1" sx={{ fontWeight: 500 }}>IP Address Change (Medium Risk)</Typography>
                <Typography variant="body2" sx={{ color: '#94A3B8' }}>Changing uplink IP from 10.0.1.1 to .2 may cause BGP neighborhood drops if the peer is not updated simultaneously.</Typography>
              </Box>
            </Box>

            <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2, mt: 4 }}>AI Recommendations</Typography>
            <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2, mb: 4 }}>
              <CheckCircleIcon sx={{ color: '#3B82F6', mt: 0.5 }} />
              <Box>
                <Typography variant="body1" sx={{ fontWeight: 500 }}>Coordinate BGP Peer Update</Typography>
                <Typography variant="body2" sx={{ color: '#94A3B8' }}>Ensure the neighboring router config is staged for execution at the exact same maintenance window.</Typography>
              </Box>
            </Box>

            <Box sx={{ display: 'flex', gap: 2 }}>
              <Button variant="contained" color="success" startIcon={<AssignmentTurnedInIcon />} fullWidth sx={{ py: 1.5, borderRadius: 2 }}>
                Approve Change
              </Button>
              <Button variant="outlined" color="error" fullWidth sx={{ py: 1.5, borderRadius: 2, borderColor: 'rgba(239, 68, 68, 0.5)' }}>
                Reject
              </Button>
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}
