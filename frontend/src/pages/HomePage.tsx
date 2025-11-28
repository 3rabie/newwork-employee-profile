import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../features/auth/contexts/AuthContext';
import { SwitchUserDialog } from '../features/auth/components/SwitchUserDialog';
import './HomePage.css';

export function HomePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [isSwitchDialogOpen, setIsSwitchDialogOpen] = useState(false);
  const roleLabel = user?.role ?? 'UNKNOWN';
  const roleClassSuffix = user?.role ? user.role.toLowerCase() : 'unknown';

  return (
    <div className="home-page">
      <header className="home-header">
        <div className="header-content">
          <h1>Employee Profile</h1>
          <div className="header-actions">
            <div className="user-info">
              <span className="user-email">{user?.email}</span>
              <span className={`user-role role-${roleClassSuffix}`}>
                {roleLabel}
              </span>
            </div>
            <button
              className="btn-switch"
              onClick={() => setIsSwitchDialogOpen(true)}
            >
              Switch User
            </button>
            <button className="btn-logout" onClick={logout}>
              Logout
            </button>
          </div>
        </div>
      </header>

      <main className="home-main">
        <div className="welcome-section">
          <h2>Welcome, {user?.email}!</h2>
          <p>You are logged in as a <strong>{user?.role}</strong></p>
          <div className="user-details">
            <div className="detail-item">
              <span className="detail-label">User ID:</span>
              <span className="detail-value">{user?.userId}</span>
            </div>
            <div className="detail-item">
              <span className="detail-label">Employee ID:</span>
              <span className="detail-value">{user?.employeeId}</span>
            </div>
            {user?.managerId && (
              <div className="detail-item">
                <span className="detail-label">Manager ID:</span>
                <span className="detail-value">{user?.managerId}</span>
              </div>
            )}
          </div>
          <div className="actions-section">
            <button
              className="btn-primary"
              onClick={() => navigate(`/profile/${user?.userId}`)}
            >
              View My Profile
            </button>
          </div>
        </div>
      </main>

      <SwitchUserDialog
        isOpen={isSwitchDialogOpen}
        onClose={() => setIsSwitchDialogOpen(false)}
      />
    </div>
  );
}
