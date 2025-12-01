import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './features/auth/contexts/AuthContext';
import { ProtectedRoute } from './features/auth/components/ProtectedRoute';
import { LoginPage } from './features/auth/pages/LoginPage';
import { HomePage } from './pages/HomePage';
import ProfilePage from './features/profile/pages/ProfilePage';
import { DirectoryPage } from './features/directory/pages/DirectoryPage';
import { AbsencePage } from './features/absence/pages/AbsencePage';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <HomePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/profile/:userId"
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/people"
            element={
              <ProtectedRoute>
                <DirectoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/absences"
            element={
              <ProtectedRoute>
                <AbsencePage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
