import { Route, Routes } from 'react-router-dom'
import Sidebar from './components/Sidebar'
import Dashboard from './pages/Dashboard'
import RoutesStops from './pages/RoutesStops'
import RouteDetail from './pages/RouteDetail'
import StopDetail from './pages/StopDetail'
import UploadData from './pages/UploadData'
import Reports from './pages/Reports'
import UsersSharers from './pages/UsersSharers'
import Announcements from './pages/Announcements'
import SearchPreview from './pages/SearchPreview'

export default function App() {
  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/routes-stops" element={<RoutesStops />} />
          <Route path="/route/:routeId" element={<RouteDetail />} />
          <Route path="/stop/:stopId" element={<StopDetail />} />
          <Route path="/upload" element={<UploadData />} />
          <Route path="/search-preview" element={<SearchPreview />} />
          <Route path="/reports" element={<Reports />} />
          <Route path="/announcements" element={<Announcements />} />
          <Route path="/users" element={<UsersSharers />} />
        </Routes>
      </main>
    </div>
  )
}
