import { Routes, Route, Navigate } from 'react-router-dom'
import { ProjectDashboard } from './routes/dashboard/ProjectDashboard.tsx'
import { WorkspaceLayout } from './routes/workspace/WorkspaceLayout.tsx'
import { ConversationalBuilder } from './routes/builder/ConversationalBuilder.tsx'
import { Marketplace } from './routes/marketplace/Marketplace.tsx'
import { Templates } from './routes/templates/Templates.tsx'
import { MigrationAssistant } from './routes/migration/MigrationAssistant.tsx'
import { AuthProvider } from './components/AuthProvider.tsx'
import { ProtectedRoute } from './components/ProtectedRoute.tsx'
import { Login } from './routes/auth/Login.tsx'

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<ProtectedRoute><ProjectDashboard /></ProtectedRoute>} />
        <Route path="/workspace/:projectId" element={<ProtectedRoute><WorkspaceLayout /></ProtectedRoute>} />
        <Route path="/builder" element={<ProtectedRoute><ConversationalBuilder /></ProtectedRoute>} />
        <Route path="/marketplace" element={<ProtectedRoute><Marketplace /></ProtectedRoute>} />
        <Route path="/templates" element={<ProtectedRoute><Templates /></ProtectedRoute>} />
        <Route path="/migration" element={<ProtectedRoute><MigrationAssistant /></ProtectedRoute>} />
      </Routes>
    </AuthProvider>
  )
}

export default App
