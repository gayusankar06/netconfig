import React from 'react';
import { Box, Button, Typography, Paper, Grid } from '@mui/material';
import GoogleIcon from '@mui/icons-material/Google';
import SecurityIcon from '@mui/icons-material/Security';
import ShieldIcon from '@mui/icons-material/Shield';
import SpeedIcon from '@mui/icons-material/Speed';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { doLogin } from '../keycloak';

const FeatureItem = ({ icon, text }: { icon: React.ReactNode, text: str }) => (
    <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Box sx={{ 
            bgcolor: 'rgba(255,255,255,0.1)', 
            p: 1.5, 
            borderRadius: '12px',
            mr: 2,
            display: 'flex'
        }}>
            {icon}
        </Box>
        <Typography variant="h6" sx={{ color: 'white', fontWeight: 500 }}>
            {text}
        </Typography>
    </Box>
);

const AuthPage: React.FC = () => {
    return (
        <Grid container sx={{ minHeight: '100vh', bgcolor: '#0B0F19' }}>
            {/* Left Side - Branding & Features */}
            <Grid item xs={12} md={6} sx={{ 
                display: { xs: 'none', md: 'flex' }, 
                flexDirection: 'column', 
                justifyContent: 'center',
                p: 8,
                background: 'linear-gradient(135deg, #0B0F19 0%, #1A233A 100%)',
                position: 'relative',
                overflow: 'hidden'
            }}>
                <Box sx={{
                    position: 'absolute',
                    top: '-10%',
                    left: '-10%',
                    width: '600px',
                    height: '600px',
                    background: 'radial-gradient(circle, rgba(59,130,246,0.15) 0%, rgba(0,0,0,0) 70%)',
                    borderRadius: '50%',
                }} />
                
                <Box sx={{ zIndex: 1 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', mb: 6 }}>
                        <SecurityIcon sx={{ color: '#3B82F6', fontSize: 48, mr: 2 }} />
                        <Typography variant="h3" sx={{ color: 'white', fontWeight: 700 }}>
                            NetConfig<span style={{ color: '#3B82F6' }}>AI</span>
                        </Typography>
                    </Box>

                    <Typography variant="h4" sx={{ color: '#E2E8F0', mb: 2, fontWeight: 600 }}>
                        Enterprise Network Configuration Review
                    </Typography>
                    
                    <Typography variant="h6" sx={{ color: '#94A3B8', mb: 8, fontWeight: 400, maxWidth: '80%' }}>
                        Automate compliance, detect security risks, and accelerate your infrastructure deployments with AI-powered differential analysis.
                    </Typography>

                    <FeatureItem icon={<ShieldIcon sx={{ color: '#10B981' }}/>} text="Automated Security & Compliance Analysis" />
                    <FeatureItem icon={<SpeedIcon sx={{ color: '#F59E0B' }}/>} text="Accelerated Review Workflows" />
                    <FeatureItem icon={<CheckCircleIcon sx={{ color: '#3B82F6' }}/>} text="Enterprise RBAC & Audit Trails" />
                </Box>
            </Grid>

            {/* Right Side - Login Card */}
            <Grid item xs={12} md={6} sx={{ 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'center',
                p: 4,
                position: 'relative'
            }}>
                 <Box sx={{
                    position: 'absolute',
                    bottom: '-10%',
                    right: '-10%',
                    width: '500px',
                    height: '500px',
                    background: 'radial-gradient(circle, rgba(16,185,129,0.1) 0%, rgba(0,0,0,0) 70%)',
                    borderRadius: '50%',
                }} />

                <Paper elevation={24} sx={{ 
                    p: 6, 
                    width: '100%', 
                    maxWidth: 480, 
                    bgcolor: 'rgba(30, 41, 59, 0.7)',
                    backdropFilter: 'blur(16px)',
                    border: '1px solid rgba(255,255,255,0.1)',
                    borderRadius: '24px',
                    zIndex: 1
                }}>
                    <Typography variant="h4" align="center" sx={{ color: 'white', fontWeight: 700, mb: 1 }}>
                        Welcome Back
                    </Typography>
                    <Typography variant="body1" align="center" sx={{ color: '#94A3B8', mb: 6 }}>
                        Sign in to access your enterprise dashboard
                    </Typography>

                    <Button
                        fullWidth
                        variant="contained"
                        size="large"
                        startIcon={<GoogleIcon />}
                        onClick={() => doLogin({ idpHint: 'google' })}
                        sx={{ 
                            mb: 3, 
                            py: 1.5,
                            bgcolor: 'white', 
                            color: '#1E293B',
                            fontWeight: 600,
                            borderRadius: '12px',
                            '&:hover': { bgcolor: '#F8FAFC' }
                        }}
                    >
                        Sign in with Google
                    </Button>

                    <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                        <Box sx={{ flex: 1, height: '1px', bgcolor: 'rgba(255,255,255,0.1)' }} />
                        <Typography sx={{ color: '#94A3B8', px: 2, fontSize: '0.875rem' }}>OR</Typography>
                        <Box sx={{ flex: 1, height: '1px', bgcolor: 'rgba(255,255,255,0.1)' }} />
                    </Box>

                    <Button
                        fullWidth
                        variant="contained"
                        size="large"
                        startIcon={<SecurityIcon />}
                        onClick={() => doLogin()}
                        sx={{ 
                            py: 1.5,
                            bgcolor: '#3B82F6', 
                            fontWeight: 600,
                            borderRadius: '12px',
                            '&:hover': { bgcolor: '#2563EB' }
                        }}
                    >
                        Sign in with Enterprise SSO
                    </Button>
                    
                    <Typography align="center" sx={{ color: '#64748B', mt: 4, fontSize: '0.875rem' }}>
                        Secured by Keycloak Identity Provider
                    </Typography>
                </Paper>
            </Grid>
        </Grid>
    );
};

export default AuthPage;
