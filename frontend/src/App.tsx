import type { ReactNode } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext';
import Login from './pages/Login';
import Register from './pages/Register';
import AuthCallback from './pages/AuthCallback';
import Dashboard from './pages/Dashboard';
import Telegram from './pages/Telegram';
import Cards from './pages/Cards';
import Savings from './pages/Savings';
import { BottomNav } from './components/BottomNav';

function Protected({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return <p style={{ color: 'var(--muted)' }}>Cargando…</p>;
  if (!user) return <Navigate to="/login" replace />;
  return (
    <>
      {children}
      <BottomNav />
    </>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/auth/callback" element={<AuthCallback />} />
          <Route
            path="/"
            element={
              <Protected>
                <Dashboard />
              </Protected>
            }
          />
          <Route
            path="/telegram"
            element={
              <Protected>
                <Telegram />
              </Protected>
            }
          />
          <Route
            path="/cards"
            element={
              <Protected>
                <Cards />
              </Protected>
            }
          />
          <Route
            path="/savings"
            element={
              <Protected>
                <Savings />
              </Protected>
            }
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}