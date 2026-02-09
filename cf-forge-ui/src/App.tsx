import { Routes, Route, Navigate } from 'react-router-dom'
import { ProjectDashboard } from './routes/dashboard/ProjectDashboard.tsx'
import { WorkspaceLayout } from './routes/workspace/WorkspaceLayout.tsx'
import { ConversationalBuilder } from './routes/builder/ConversationalBuilder.tsx'
import { Marketplace } from './routes/marketplace/Marketplace.tsx'
import { Templates } from './routes/templates/Templates.tsx'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/dashboard" element={<ProjectDashboard />} />
      <Route path="/workspace/:projectId" element={<WorkspaceLayout />} />
      <Route path="/builder" element={<ConversationalBuilder />} />
      <Route path="/marketplace" element={<Marketplace />} />
      <Route path="/templates" element={<Templates />} />
    </Routes>
  )
}

export default App
