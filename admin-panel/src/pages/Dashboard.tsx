import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useStore } from '../data/store.tsx'
import { useDistrict } from '../data/DistrictContext.tsx'
import StatusBadge from '../components/StatusBadge'
import { formatClock, formatRelativeTime, reportTypeLabel } from '../data/format'

export default function Dashboard() {
  const { state } = useStore()
  const { district } = useDistrict()

  const routes = useMemo(
    () => state.routes.filter((r) => !district || r.district === district),
    [state.routes, district],
  )
  const reports = useMemo(
    () => [...state.reports].sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
    [state.reports],
  )

  const busesLiveNow = routes.reduce((sum, r) => sum + r.busesLive, 0)
  const sharersNow = routes.reduce((sum, r) => sum + r.sharerCount, 0)
  const routesWithLiveData = routes.filter((r) => r.liveMode === 'LIVE').length
  const openReports = state.reports.filter((r) => r.status === 'OPEN').length

  const localCount = routes.filter((r) => r.routeType === 'LOCAL').length
  const longDistanceCount = routes.filter((r) => r.routeType === 'LONG_DISTANCE').length
  const verifiedCount = routes.filter((r) => r.verified).length
  const demoCount = routes.filter((r) => r.source === 'DEMO').length
  const officialCount = routes.filter((r) => r.source === 'OFFICIAL').length
  const communityCount = routes.filter((r) => r.source === 'COMMUNITY').length

  const districtBreakdown = useMemo(() => {
    const counts = new Map<string, { routes: number; stops: number }>()
    for (const r of routes) {
      const entry = counts.get(r.district) ?? { routes: 0, stops: 0 }
      entry.routes += 1
      counts.set(r.district, entry)
    }
    for (const s of state.stops) {
      if (district && s.district !== district) continue
      const entry = counts.get(s.district) ?? { routes: 0, stops: 0 }
      entry.stops += 1
      counts.set(s.district, entry)
    }
    return [...counts.entries()].sort((a, b) => b[1].routes + b[1].stops - (a[1].routes + a[1].stops))
  }, [routes, state.stops, district])

  function stopName(id?: string) {
    return id ? state.stops.find((s) => s.id === id)?.name : undefined
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1 className="page-title">Dashboard</h1>
          <p className="page-subtitle">
            Live overview of buses, sharers and reports{district ? ` in ${district}` : ' across all districts'}
          </p>
        </div>
        <span className="live-badge">
          <span className="dot" />
          System live · last sync {formatClock()}
        </span>
      </div>

      <div className="stat-grid">
        <StatCard label="Buses live now" value={busesLiveNow} delta="Across tracked routes" />
        <StatCard
          label="Passengers sharing"
          value={sharersNow}
          delta={routes.length ? `Avg. ${(sharersNow / Math.max(busesLiveNow, 1)).toFixed(1)} per live bus` : ''}
        />
        <StatCard
          label="Routes with live data"
          value={`${routesWithLiveData} / ${routes.length}`}
          delta={routes.length ? `${Math.round((routesWithLiveData / routes.length) * 100)}% coverage today` : ''}
        />
        <StatCard label="Open reports" value={openReports} delta="Needs review" warn />
      </div>

      <div className="panel-grid" style={{ marginBottom: 20 }}>
        <div className="card">
          <h2 className="card-title" style={{ marginBottom: 12 }}>Route mix</h2>
          <BreakdownRow label="Local" value={localCount} total={routes.length} color="#1E8E5A" />
          <BreakdownRow label="Long-distance" value={longDistanceCount} total={routes.length} color="#8E24AA" />
          <div style={{ height: 14 }} />
          <BreakdownRow label="Official / Community" value={officialCount + communityCount} total={routes.length} color="#1E8E5A" />
          <BreakdownRow label="Demo" value={demoCount} total={routes.length} color="#B5651D" />
          <div style={{ height: 14 }} />
          <BreakdownRow label="Verified" value={verifiedCount} total={routes.length} color="#1E8E5A" />
          <BreakdownRow label="Unverified" value={routes.length - verifiedCount} total={routes.length} color="#B5651D" />
        </div>

        <div className="card">
          <h2 className="card-title" style={{ marginBottom: 12 }}>Coverage by district</h2>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>District</th>
                  <th>Routes</th>
                  <th>Stops</th>
                </tr>
              </thead>
              <tbody>
                {districtBreakdown.map(([name, counts]) => (
                  <tr key={name}>
                    <td>{name}</td>
                    <td className="muted">{counts.routes}</td>
                    <td className="muted">{counts.stops}</td>
                  </tr>
                ))}
                {districtBreakdown.length === 0 && (
                  <tr>
                    <td colSpan={3} className="empty-state">No data yet.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div className="panel-grid">
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Route coverage right now</h2>
            <Link className="link-btn" to="/routes-stops">
              Manage routes
            </Link>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Route</th>
                  <th>From → To</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Buses live</th>
                  <th>Sharers</th>
                </tr>
              </thead>
              <tbody>
                {routes.map((route) => (
                  <tr key={route.id}>
                    <td>
                      <span className="route-chip">{route.shortName}</span>
                    </td>
                    <td>{route.longName}</td>
                    <td>
                      <span className={`badge ${route.routeType === 'LONG_DISTANCE' ? 'recent' : 'timetable'}`}>
                        {route.routeType === 'LONG_DISTANCE' ? 'Long-distance' : 'Local'}
                      </span>
                    </td>
                    <td>
                      <StatusBadge mode={route.liveMode} />
                    </td>
                    <td>{route.busesLive}</td>
                    <td>{route.sharerCount}</td>
                  </tr>
                ))}
                {routes.length === 0 && (
                  <tr>
                    <td colSpan={6} className="empty-state">
                      No routes for this district yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Latest reports</h2>
            <Link className="link-btn" to="/reports">
              Review all
            </Link>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {reports.slice(0, 4).map((report) => (
              <div key={report.id}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 2 }}>
                  <span className={`badge ${report.status === 'OPEN' ? 'open' : 'resolved'}`}>
                    {reportTypeLabel(report.type)}
                  </span>
                  <span className="muted" style={{ fontSize: 11 }}>
                    {formatRelativeTime(report.createdAt)}
                  </span>
                </div>
                <div style={{ fontSize: 13 }}>
                  {report.message}
                  {stopName(report.stopId) && (
                    <span className="muted"> — {stopName(report.stopId)}</span>
                  )}
                </div>
              </div>
            ))}
            {reports.length === 0 && <div className="empty-state">No reports yet.</div>}
          </div>
        </div>
      </div>
    </>
  )
}

function BreakdownRow({
  label,
  value,
  total,
  color,
}: {
  label: string
  value: number
  total: number
  color: string
}) {
  const pct = total > 0 ? Math.round((value / total) * 100) : 0
  return (
    <div style={{ marginBottom: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 3 }}>
        <span>{label}</span>
        <span className="muted">{value} ({pct}%)</span>
      </div>
      <div style={{ height: 6, background: '#EFEFEF', borderRadius: 999 }}>
        <div style={{ height: 6, width: `${pct}%`, background: color, borderRadius: 999 }} />
      </div>
    </div>
  )
}

function StatCard({
  label,
  value,
  delta,
  warn,
}: {
  label: string
  value: string | number
  delta?: string
  warn?: boolean
}) {
  return (
    <div className="stat-card">
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {delta && <div className={`stat-delta ${warn ? 'warn' : ''}`}>{delta}</div>}
    </div>
  )
}
