import { NavLink } from 'react-router-dom'
import { useStore } from '../data/store.tsx'
import { useDistrict } from '../data/DistrictContext.tsx'

const CONNECTION_LABEL: Record<string, string> = {
  connecting: 'Connecting…',
  online: 'Connected to server',
  offline: 'Offline — local only',
}

export default function Sidebar() {
  const { state, dispatch, connection } = useStore()
  const { district, setDistrict } = useDistrict()
  const openReports = state.reports.filter((r) => r.status === 'OPEN').length

  function addDistrict() {
    const name = prompt('New district name (e.g. "Madurai"):')?.trim()
    if (!name) return
    if (state.districts.includes(name)) {
      setDistrict(name)
      return
    }
    dispatch({ type: 'ADD_DISTRICT', name })
    setDistrict(name)
  }

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="sidebar-logo">🚌</div>
        <div className="sidebar-brand-text">
          <div className="name">Where is my Bus</div>
          <div className="tag">Admin panel</div>
        </div>
      </div>

      <div className="sidebar-section-label" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span>District</span>
        <button
          type="button"
          onClick={addDistrict}
          title="Add district"
          style={{
            background: 'none', border: 'none', color: '#bfd8ce', cursor: 'pointer',
            fontSize: 14, fontWeight: 700, padding: '0 2px', lineHeight: 1,
          }}
        >
          +
        </button>
      </div>
      <select value={district} onChange={(e) => setDistrict(e.target.value)}>
        <option value="">All districts</option>
        {state.districts.map((d) => (
          <option key={d} value={d}>{d}</option>
        ))}
      </select>

      <nav className="sidebar-nav">
        <NavLink to="/" end>
          Dashboard
        </NavLink>
        <NavLink to="/routes-stops">Routes & stops</NavLink>
        <NavLink to="/upload">Upload data</NavLink>
        <NavLink to="/search-preview">Search preview</NavLink>
        <NavLink to="/reports">
          <span>Reports</span>
          {openReports > 0 && <span className="count">{openReports}</span>}
        </NavLink>
        <NavLink to="/announcements">News & updates</NavLink>
        <NavLink to="/users">Users & sharers</NavLink>
      </nav>

      <div className="sidebar-footer">
        <div className="name">{state.users[0]?.name ?? 'Admin'}</div>
        <div className="role">Super admin</div>
        <div className={`connection-pill ${connection}`}>
          <span className="dot" />
          {CONNECTION_LABEL[connection]}
        </div>
      </div>
    </aside>
  )
}
