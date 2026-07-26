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
import Debts from './pages/Debts';
import { BottomNav } from './components/BottomNav';
import More from './pages/More';
import Profile from './pages/Profile';
import Invoices from './pages/Invoices';
import Income from './pages/Income';
import Cash from './pages/Cash';
import Transactions from './pages/Transactions';


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
          <Route path="/transactions" element={<Protected><Transactions /></Protected>} />
          <Route path="/more" element={<Protected><More /></Protected>} />
          <Route path="/invoices" element={<Protected><Invoices /></Protected>} />
          <Route path="/income" element={<Protected><Income /></Protected>} />
          <Route path="/cash" element={<Protected><Cash /></Protected>} />
          <Route path="/profile" element={<Protected><Profile /></Protected>} />
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
          <Route
            path="/debts"
            element={
              <Protected>
                <Debts />
              </Protected>
            }
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}