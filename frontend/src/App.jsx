import { Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AuthProvider } from './context/AuthContext'
import { WebSocketProvider } from './context/WebSocketContext'
import { AbsencePage } from './pages/AbsencePage'
import { AuditLogPage } from './pages/AuditLogPage'
import { DashboardPage } from './pages/DashboardPage'
import { HandoverReportsPage } from './pages/HandoverReportsPage'
import { LoginPage } from './pages/LoginPage'
import { MapPage } from './pages/MapPage'
import { ObservationsPage } from './pages/ObservationsPage'

export default function App() {
  return (
    <AuthProvider>
      <WebSocketProvider>
        <Routes>
          <Route path="/logg-inn" element={<LoginPage />} />
          <Route
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route path="/" element={<DashboardPage />} />
            <Route path="/kart" element={<MapPage />} />
            <Route path="/fravaer" element={<AbsencePage />} />
            <Route path="/journal" element={<ObservationsPage />} />
            <Route path="/rapporter" element={<HandoverReportsPage />} />
            <Route path="/revisjonslogg" element={<AuditLogPage />} />
          </Route>
        </Routes>
      </WebSocketProvider>
    </AuthProvider>
  )
}
