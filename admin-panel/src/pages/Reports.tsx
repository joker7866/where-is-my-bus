import { useMemo, useState } from 'react'
import { useStore } from '../data/store.tsx'
import { useDistrict } from '../data/DistrictContext.tsx'
import { formatRelativeTime, reportTypeLabel } from '../data/format'
import type { ReportStatus } from '../data/types'

type Filter = 'ALL' | ReportStatus

export default function Reports() {
  const { state, dispatch } = useStore()
  const { district } = useDistrict()
  const [filter, setFilter] = useState<Filter>('ALL')

  function stop(id?: string) {
    return id ? state.stops.find((s) => s.id === id) : undefined
  }
  function route(id?: string) {
    return id ? state.routes.find((r) => r.id === id) : undefined
  }

  const reports = useMemo(() => {
    return [...state.reports]
      .filter((r) => {
        const reportDistrict = stop(r.stopId)?.district ?? route(r.routeId)?.district
        return !district || !reportDistrict || reportDistrict === district
      })
      .filter((r) => filter === 'ALL' || r.status === filter)
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.reports, state.stops, state.routes, district, filter])

  const openCount = state.reports.filter((r) => r.status === 'OPEN').length

  return (
    <>
      <div className="page-header">
        <div>
          <h1 className="page-title">Reports</h1>
          <p className="page-subtitle">
            Passenger-submitted issues{district ? ` for ${district}` : ''} — {openCount} open
          </p>
        </div>
      </div>

      <div className="tabs">
        <button className={`tab ${filter === 'ALL' ? 'active' : ''}`} onClick={() => setFilter('ALL')}>
          All ({state.reports.length})
        </button>
        <button className={`tab ${filter === 'OPEN' ? 'active' : ''}`} onClick={() => setFilter('OPEN')}>
          Open ({openCount})
        </button>
        <button className={`tab ${filter === 'RESOLVED' ? 'active' : ''}`} onClick={() => setFilter('RESOLVED')}>
          Resolved ({state.reports.length - openCount})
        </button>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Type</th>
                <th>Message</th>
                <th>Related to</th>
                <th>Reported</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {reports.map((r) => {
                const relatedStop = stop(r.stopId)
                const relatedRoute = route(r.routeId)
                return (
                  <tr key={r.id}>
                    <td>
                      <span className={`badge ${r.status === 'OPEN' ? 'open' : 'resolved'}`}>
                        {reportTypeLabel(r.type)}
                      </span>
                    </td>
                    <td style={{ maxWidth: 360 }}>{r.message}</td>
                    <td className="muted">
                      {[relatedRoute && `Route ${relatedRoute.shortName}`, relatedStop?.name].filter(Boolean).join(' · ') || '—'}
                    </td>
                    <td className="muted">{formatRelativeTime(r.createdAt)}</td>
                    <td>
                      <span className={`badge ${r.status === 'OPEN' ? 'open' : 'resolved'}`}>
                        {r.status === 'OPEN' ? 'Open' : 'Resolved'}
                      </span>
                    </td>
                    <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                      {r.status === 'OPEN' ? (
                        <button
                          className="btn btn-primary btn-sm"
                          onClick={() => dispatch({ type: 'SET_REPORT_STATUS', id: r.id, status: 'RESOLVED' })}
                        >
                          Mark resolved
                        </button>
                      ) : (
                        <button
                          className="btn btn-outline btn-sm"
                          onClick={() => dispatch({ type: 'SET_REPORT_STATUS', id: r.id, status: 'OPEN' })}
                        >
                          Reopen
                        </button>
                      )}
                    </td>
                  </tr>
                )
              })}
              {reports.length === 0 && (
                <tr>
                  <td colSpan={6} className="empty-state">
                    No reports here.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}
