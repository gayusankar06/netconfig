import React, { useState } from 'react';
import { Box, Button, Typography, Paper, Grid, TextField, CircularProgress } from '@mui/material';
import GoogleIcon from '@mui/icons-material/Google';
import SecurityIcon from '@mui/icons-material/Security';
import ShieldIcon from '@mui/icons-material/Shield';
import SpeedIcon from '@mui/icons-material/Speed';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { toast } from 'react-toastify';

interface AuthPageProps {
  onLogin: (token: string) => void;
}

const FeatureItem = ({ icon, text }: { icon: React.ReactNode, text: string }) => (
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

const AuthPage: React.FC<AuthPageProps> = ({ onLogin }) => {
    const [view, setView] = useState<'login' | 'signup' | 'forgot'>('login');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [fullName, setFullName] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        try {
            if (view === 'login') {
                const response = await fetch('http://localhost:8000/api/v1/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });
                
                if (!response.ok) throw new Error('Invalid email or password');
                const data = await response.json();
                toast.success('Successfully logged in!');
                onLogin(data.access_token);

            } else if (view === 'signup') {
                const response = await fetch('http://localhost:8000/api/v1/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password, full_name: fullName, role: 'network_engineer' })
                });

                if (!response.ok) throw new Error('Failed to register. Email may already exist.');
                toast.success('Successfully registered! Please log in.');
                setView('login');
                setPassword('');

            } else if (view === 'forgot') {
                const response = await fetch('http://localhost:8000/api/v1/auth/forgot-password', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email })
                });

                if (!response.ok) throw new Error('Failed to send reset email');
                toast.success('If an account exists, a reset link has been sent.');
                setView('login');
            }
        } catch (error: any) {
            toast.error(error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleGoogleAuth = () => {
        toast.info('Google Authentication will be enabled soon via Firebase.');
    };

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
                        {view === 'login' ? 'Welcome Back' : view === 'signup' ? 'Create Account' : 'Reset Password'}
                    </Typography>
                    <Typography variant="body1" align="center" sx={{ color: '#94A3B8', mb: 4 }}>
                        {view === 'login' ? 'Sign in to access your enterprise dashboard' : 
                         view === 'signup' ? 'Sign up for local database access' : 
                         'Enter your email to receive a secure reset link'}
                    </Typography>

                    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                        {view === 'signup' && (
                            <TextField
                                label="Full Name"
                                variant="outlined"
                                fullWidth
                                required
                                value={fullName}
                                onChange={(e) => setFullName(e.target.value)}
                                sx={{ input: { color: 'white' }, label: { color: '#94A3B8' }, fieldset: { borderColor: 'rgba(255,255,255,0.2)' } }}
                            />
                        )}

                        <TextField
                            label="Email Address"
                            type="email"
                            variant="outlined"
                            fullWidth
                            required
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            sx={{ input: { color: 'white' }, label: { color: '#94A3B8' }, fieldset: { borderColor: 'rgba(255,255,255,0.2)' } }}
                        />

                        {view !== 'forgot' && (
                            <TextField
                                label="Password"
                                type="password"
                                variant="outlined"
                                fullWidth
                                required
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                sx={{ input: { color: 'white' }, label: { color: '#94A3B8' }, fieldset: { borderColor: 'rgba(255,255,255,0.2)' } }}
                            />
                        )}

                        {view === 'login' && (
                            <Typography 
                                variant="body2" 
                                align="right" 
                                sx={{ color: '#3B82F6', cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
                                onClick={() => setView('forgot')}
                            >
                                Forgot Password?
                            </Typography>
                        )}

                        <Button
                            type="submit"
                            fullWidth
                            variant="contained"
                            size="large"
                            disabled={loading}
                            sx={{ 
                                py: 1.5,
                                mt: 2,
                                bgcolor: '#3B82F6', 
                                fontWeight: 600,
                                borderRadius: '12px',
                                '&:hover': { bgcolor: '#2563EB' }
                            }}
                        >
                            {loading ? <CircularProgress size={24} color="inherit" /> : 
                             view === 'login' ? 'Sign In' : 
                             view === 'signup' ? 'Sign Up' : 'Send Reset Link'}
                        </Button>
                    </form>

                    <Box sx={{ display: 'flex', alignItems: 'center', my: 3 }}>
                        <Box sx={{ flex: 1, height: '1px', bgcolor: 'rgba(255,255,255,0.1)' }} />
                        <Typography sx={{ color: '#94A3B8', px: 2, fontSize: '0.875rem' }}>OR</Typography>
                        <Box sx={{ flex: 1, height: '1px', bgcolor: 'rgba(255,255,255,0.1)' }} />
                    </Box>

                    <Button
                        fullWidth
                        variant="contained"
                        size="large"
                        startIcon={<GoogleIcon />}
                        onClick={handleGoogleAuth}
                        sx={{ 
                            py: 1.5,
                            bgcolor: 'white', 
                            color: '#1E293B',
                            fontWeight: 600,
                            borderRadius: '12px',
                            '&:hover': { bgcolor: '#F8FAFC' }
                        }}
                    >
                        Continue with Google
                    </Button>

                    <Box sx={{ mt: 4, textAlign: 'center' }}>
                        {view === 'login' ? (
                            <Typography sx={{ color: '#94A3B8' }}>
                                Don't have an account?{' '}
                                <span onClick={() => setView('signup')} style={{ color: '#3B82F6', cursor: 'pointer', fontWeight: 600 }}>
                                    Sign up
                                </span>
                            </Typography>
                        ) : (
                            <Typography sx={{ color: '#94A3B8' }}>
                                Already have an account?{' '}
                                <span onClick={() => setView('login')} style={{ color: '#3B82F6', cursor: 'pointer', fontWeight: 600 }}>
                                    Sign in
                                </span>
                            </Typography>
                        )}
                    </Box>
                    
                    <Typography align="center" sx={{ color: '#64748B', mt: 4, fontSize: '0.875rem' }}>
                        Secured by Enterprise DB Auth
                    </Typography>
                </Paper>
            </Grid>
        </Grid>
    );
};

export default AuthPage;
