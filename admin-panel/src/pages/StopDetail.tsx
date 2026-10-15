import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useStore } from '../data/store.tsx'
import StatusBadge from '../components/StatusBadge'
import { buildSchedule, emptyStopTiming, formatMinutesAsClock } from '../data/timing'
import { placeTypeLabel } from '../data/format'
import type { BusRoute } from '../data/types'

export default function StopDetail() {
  const { stopId } = useParams<{ stopId: string }>()
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const stop = state.stops.find((s) => s.id === stopId)
  const [newRouteId, setNewRouteId] = useState('')

  if (!stop) {
    return (
      <div className="empty-state">
        Stop not found. <button className="link-btn" onClick={() => navigate('/routes-stops')}>Back to routes & stops</button>
      </div>
    )
  }

  const servingRoutes = state.routes
    .filter((r) => r.stops.some((s) => s.stopId === stop.id))
    .map((route) => {
      const schedule = buildSchedule(route.startTime, route.stops)
      const index = schedule.findIndex((row) => row.stopId === stop.id)
      const row = schedule[index]
      return { route, row, position: index + 1, total: schedule.length }
    })
    .sort((a, b) => a.row.arrivalMin - b.row.arrivalMin)

  const existingIds = state.routes.map((r) => r.id)

  function createRouteThroughStop() {
    const id = newRouteId.trim().toUpperCase()
    if (!id || existingIds.includes(id)) return
    const route: BusRoute = {
      id, shortName: '', longName: '', busName: '', colorHex: '#145C45',
      stops: [emptyStopTiming(stop!.id, true)], startTime: '06:00',
      liveMode: 'TIMETABLE', district: stop!.district, routeType: 'LOCAL', distanceKm: 0, viaTowns: '',
      operator: '', operatorType: 'GOVERNMENT', busType: 'TOWN', acType: 'NON_AC', source: 'DEMO', verified: false,
      busesLive: 0, sharerCount: 0, lastSeenMinutesAgo: 0, timetableEtaMinutes: 10, delayMinutes: 0,
    }
    dispatch({ type: 'UPSERT_ROUTE', route })
    setNewRouteId('')
    navigate(`/route/${id}`)
  }

  return (
    <>
      <div className="page-header">
        <div>
          <button className="link-btn" style={{ marginBottom: 8 }} onClick={() => navigate('/routes-stops')}>
            ← Back to routes & stops
          </button>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <h1 className="page-title">{stop.name}</h1>
            <span className="badge timetable">{placeTypeLabel(stop.placeType)}</span>
          </div>
          <p className="page-subtitle">
            {stop.tamilName} · {stop.lat.toFixed(4)}, {stop.lng.toFixed(4)}
            <br />
            {[stop.village, stop.city, stop.district].filter(Boolean).join(', ')}
          </p>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="card-header">
          <h2 className="card-title">Buses serving this stop</h2>
        </div>
        {servingRoutes.length === 0 ? (
          <div className="empty-state">No routes serve this stop yet.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Route</th>
                  <th>Status</th>
                  <th>Stop #</th>
                  <th>Arrival</th>
                  <th>Departure</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {servingRoutes.map(({ route, row, position, total }) => (
                  <tr key={route.id}>
                    <td>
                      <span className="route-chip" style={{ background: '#DCEFE2', color: route.colorHex }}>
                        {route.shortName}
                      </span>{' '}
                      <span className="muted">{route.longName}</span>
                    </td>
                    <td>
                      <StatusBadge mode={route.liveMode} />
                      {(route.source === 'DEMO' || !route.verified) && (
                        <span className="badge" style={{ marginLeft: 6, background: '#FCEFD8', color: '#7A5200' }}>
                          DEMO
                        </span>
                      )}
                    </td>
                    <td className="muted">{position} of {total}</td>
                    <td>{formatMinutesAsClock(row.arrivalMin)}</td>
                    <td style={{ fontWeight: 600 }}>{formatMinutesAsClock(row.departureMin)}</td>
                    <td>
                      <button className="btn btn-outline btn-sm" onClick={() => navigate(`/route/${route.id}`)}>
                        Open timetable
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="card">
        <h2 className="card-title" style={{ marginBottom: 8 }}>Add a route through here</h2>
        <p className="muted" style={{ marginTop: 0, fontSize: 12 }}>
          Starts a new route in {stop.district} with {stop.name} already added as the first stop.
        </p>
        <div style={{ display: 'flex', gap: 8, maxWidth: 360 }}>
          <input
            value={newRouteId}
            onChange={(e) => setNewRouteId(e.target.value.toUpperCase())}
            placeholder="New route ID, e.g. R88"
            style={{ flex: 1, padding: '8px 10px', border: '1px solid var(--border)', borderRadius: 8, fontSize: 14 }}
          />
          <button
            className="btn btn-primary"
            disabled={!newRouteId.trim() || existingIds.includes(newRouteId.trim())}
            onClick={createRouteThroughStop}
          >
            + Add route
          </button>
        </div>
        {newRouteId.trim() && existingIds.includes(newRouteId.trim()) && (
          <div className="error-text" style={{ marginTop: 6 }}>That ID is already in use.</div>
        )}
      </div>
    </>
  )
}
