import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useStore } from '../data/store.tsx'
import StatusBadge from '../components/StatusBadge'
import { buildSchedule, emptyStopTiming, formatDuration, formatMinutesAsClock, totalDurationMin } from '../data/timing'
import type { AcType, BusRoute, BusType, DataSource, LiveMode, OperatorType, RouteStopTiming, RouteType } from '../data/types'

function emptyRouteInDistrict(district: string): BusRoute {
  return {
    id: '', shortName: '', longName: '', busName: '', colorHex: '#145C45', stops: [], startTime: '06:00',
    liveMode: 'TIMETABLE', district, routeType: 'LOCAL', distanceKm: 0, viaTowns: '',
    operator: '', operatorType: 'GOVERNMENT', busType: 'TOWN', acType: 'NON_AC', source: 'DEMO', verified: false,
    busesLive: 0, sharerCount: 0, lastSeenMinutesAgo: 0, timetableEtaMinutes: 10, delayMinutes: 0,
  }
}

export default function RouteDetail() {
  const { routeId } = useParams<{ routeId: string }>()
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const route = state.routes.find((r) => r.id === routeId)

  const [draft, setDraft] = useState<BusRoute | null>(route ?? null)
  const [dirty, setDirty] = useState(false)
  const [newId, setNewId] = useState('')

  // Keep the draft in sync if the underlying route changes from elsewhere (e.g. a sync refresh).
  useEffect(() => {
    if (route && !dirty) setDraft(route)
  }, [route, dirty])

  if (!route || !draft) {
    return (
      <div className="empty-state">
        Route not found. <button className="link-btn" onClick={() => navigate('/routes-stops')}>Back to routes & stops</button>
      </div>
    )
  }

  const stopsById = Object.fromEntries(state.stops.map((s) => [s.id, s]))
  const availableStops = state.stops.filter(
    (s) => s.district === draft.district && !draft.stops.some((st) => st.stopId === s.id),
  )
  const schedule = buildSchedule(draft.startTime, draft.stops)
  const existingIds = state.routes.map((r) => r.id)

  function update(next: Partial<BusRoute>) {
    setDraft({ ...draft!, ...next })
    setDirty(true)
  }
  function updateStop(index: number, patch: Partial<RouteStopTiming>) {
    const next = draft!.stops.map((s, i) => (i === index ? { ...s, ...patch } : s))
    update({ stops: next })
  }
  function addStop(stopId: string) {
    if (!stopId) return
    update({ stops: [...draft!.stops, emptyStopTiming(stopId, draft!.stops.length === 0)] })
  }
  function removeStop(index: number) {
    update({ stops: draft!.stops.filter((_, i) => i !== index) })
  }
  function moveStop(index: number, dir: -1 | 1) {
    const next = [...draft!.stops]
    const target = index + dir
    if (target < 0 || target >= next.length) return
    ;[next[index], next[target]] = [next[target], next[index]]
    update({ stops: next })
  }
  function save() {
    dispatch({ type: 'UPSERT_ROUTE', route: draft! })
    setDirty(false)
  }
  function removeRoute() {
    if (confirm(`Delete route ${route!.shortName}? This cannot be undone.`)) {
      dispatch({ type: 'DELETE_ROUTE', id: route!.id })
      navigate('/routes-stops')
    }
  }
  function createNewRoute() {
    const id = newId.trim().toUpperCase()
    if (!id || existingIds.includes(id)) return
    const next = { ...emptyRouteInDistrict(draft!.district), id }
    dispatch({ type: 'UPSERT_ROUTE', route: next })
    setNewId('')
    navigate(`/route/${id}`)
  }

  return (
    <>
      <div className="page-header">
        <div>
          <button className="link-btn" style={{ marginBottom: 8 }} onClick={() => navigate('/routes-stops')}>
            ← Back to routes & stops
          </button>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
            <span className="route-chip" style={{ background: '#DCEFE2', color: draft.colorHex, fontSize: 16 }}>
              {draft.shortName}
            </span>
            <h1 className="page-title" style={{ fontSize: 22 }}>{draft.longName}</h1>
            <StatusBadge mode={draft.liveMode} />
            <span className={`badge ${draft.routeType === 'LONG_DISTANCE' ? 'recent' : 'timetable'}`}>
              {draft.routeType === 'LONG_DISTANCE' ? 'Long-distance' : 'Local'}
            </span>
            {(draft.source === 'DEMO' || !draft.verified) && (
              <span className="badge" style={{ background: '#FCEFD8', color: '#7A5200' }}>
                DEMO · unverified
              </span>
            )}
          </div>
          {draft.busName && (
            <p className="muted" style={{ margin: '2px 0 0', fontSize: 13, fontStyle: 'italic' }}>{draft.busName}</p>
          )}
          <p className="page-subtitle">
            {draft.stops.length} stop{draft.stops.length === 1 ? '' : 's'} · {formatDuration(totalDurationMin(draft.stops))} end to end · {draft.district}
            {draft.routeType === 'LONG_DISTANCE' && draft.distanceKm > 0 && ` · ${draft.distanceKm} km`}
            {draft.routeType === 'LONG_DISTANCE' && draft.viaTowns && ` · ${draft.viaTowns}`}
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-danger" onClick={removeRoute}>Delete route</button>
          <button className="btn btn-primary" disabled={!dirty} onClick={save}>
            {dirty ? 'Save changes' : 'Saved'}
          </button>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <h2 className="card-title" style={{ marginBottom: 12 }}>Overview</h2>
        <div className="form-grid">
          <div className="field" style={{ margin: 0 }}>
            <label>Short name</label>
            <input value={draft.shortName} onChange={(e) => update({ shortName: e.target.value })} />
          </div>
          <div className="field span-2" style={{ margin: 0 }}>
            <label>Long name (From → To)</label>
            <input value={draft.longName} onChange={(e) => update({ longName: e.target.value })} />
          </div>
          <div className="field span-2" style={{ margin: 0 }}>
            <label>Bus name</label>
            <input
              value={draft.busName}
              onChange={(e) => update({ busName: e.target.value })}
              placeholder="e.g. Sivaganga Express"
            />
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label>District</label>
            <input value={draft.district} onChange={(e) => update({ district: e.target.value })} />
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label>Color</label>
            <input type="color" value={draft.colorHex} onChange={(e) => update({ colorHex: e.target.value })} />
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label>Status</label>
            <select value={draft.liveMode} onChange={(e) => update({ liveMode: e.target.value as LiveMode })}>
              <option value="LIVE">Live</option>
              <option value="RECENT">Recent</option>
              <option value="TIMETABLE">Timetable only</option>
            </select>
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label>Route type</label>
            <select
              value={draft.routeType}
              onChange={(e) => update({ routeType: e.target.value as RouteType })}
            >
              <option value="LOCAL">Local (point to point)</option>
              <option value="LONG_DISTANCE">Long-distance</option>
            </select>
          </div>

          {draft.routeType === 'LONG_DISTANCE' && (
            <>
              <div className="field" style={{ margin: 0 }}>
                <label>Distance (km)</label>
                <input
                  type="number"
                  min={0}
                  step="0.5"
                  value={draft.distanceKm}
                  onChange={(e) => update({ distanceKm: Number(e.target.value) })}
                />
              </div>
              <div className="field" style={{ margin: 0 }}>
                <label>Via</label>
                <input
                  value={draft.viaTowns}
                  onChange={(e) => update({ viaTowns: e.target.value })}
                  placeholder="e.g. via Pillayarpatti"
                />
              </div>
            </>
          )}

          <div className="field" style={{ margin: 0 }}>
            <label>Operator</label>
            <input
              value={draft.operator}
              onChange={(e) => update({ operator: e.target.value })}
              placeholder="e.g. TNSTC"
            />
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label>Operator type</label>
            <select value={draft.operatorType} onChange={(e) => update({ operatorType: e.target.value as OperatorType })}>
              <option value="GOVERNMENT">Government</option>
              <option value="PRIVATE">Private</option>
            </select>
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label>Bus type</label>
            <select value={draft.busType} onChange={(e) => update({ busType: e.target.value as BusType })}>
              <option value="VILLAGE">Village</option>
              <option value="TOWN">Town</option>
              <option value="MOFUSSIL">Mofussil</option>
              <option value="EXPRESS">Express</option>
              <option value="DELUXE">Deluxe</option>
              <option value="ULTRA_DELUXE">Ultra deluxe</option>
              <option value="MINI">Mini</option>
              <option value="SPECIAL">Special</option>
            </select>
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label>AC / Non-AC</label>
            <select value={draft.acType} onChange={(e) => update({ acType: e.target.value as AcType })}>
              <option value="NON_AC">Non-AC</option>
              <option value="AC">AC</option>
            </select>
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label>Data source</label>
            <select value={draft.source} onChange={(e) => update({ source: e.target.value as DataSource })}>
              <option value="DEMO">Demo</option>
              <option value="OFFICIAL">Official</option>
              <option value="COMMUNITY">Community</option>
            </select>
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label>Verified</label>
            <select
              value={draft.verified ? 'yes' : 'no'}
              onChange={(e) => update({ verified: e.target.value === 'yes' })}
            >
              <option value="no">Not verified</option>
              <option value="yes">Verified</option>
            </select>
          </div>
          {(draft.source === 'DEMO' || !draft.verified) && (
            <div className="field span-2" style={{ margin: 0 }}>
              <div className="banner-warning">
                Shown to riders as DEMO / unverified until you set source to Official or Community and mark it
                Verified — never present demo data as real TNSTC information.
              </div>
            </div>
          )}

          {draft.liveMode === 'LIVE' && (
            <>
              <div className="field" style={{ margin: 0 }}>
                <label>Buses live</label>
                <input
                  type="number"
                  min={0}
                  value={draft.busesLive}
                  onChange={(e) => update({ busesLive: Number(e.target.value) })}
                />
              </div>
              <div className="field" style={{ margin: 0 }}>
                <label>Sharers</label>
                <input
                  type="number"
                  min={0}
                  value={draft.sharerCount}
                  onChange={(e) => update({ sharerCount: Number(e.target.value) })}
                />
              </div>
              <div className="field" style={{ margin: 0 }}>
                <label>Delay (min)</label>
                <input
                  type="number"
                  min={0}
                  value={draft.delayMinutes}
                  onChange={(e) => update({ delayMinutes: Number(e.target.value) })}
                />
              </div>
            </>
          )}
          {draft.liveMode === 'RECENT' && (
            <>
              <div className="field" style={{ margin: 0 }}>
                <label>Sharers</label>
                <input
                  type="number"
                  min={0}
                  value={draft.sharerCount}
                  onChange={(e) => update({ sharerCount: Number(e.target.value) })}
                />
              </div>
              <div className="field" style={{ margin: 0 }}>
                <label>Last seen (min ago)</label>
                <input
                  type="number"
                  min={0}
                  value={draft.lastSeenMinutesAgo}
                  onChange={(e) => update({ lastSeenMinutesAgo: Number(e.target.value) })}
                />
              </div>
            </>
          )}
          {draft.liveMode === 'TIMETABLE' && (
            <div className="field" style={{ margin: 0 }}>
              <label>Scheduled ETA (min)</label>
              <input
                type="number"
                min={0}
                value={draft.timetableEtaMinutes}
                onChange={(e) => update({ timetableEtaMinutes: Number(e.target.value) })}
              />
            </div>
          )}
        </div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="field" style={{ margin: 0, maxWidth: 220 }}>
          <label>Start time (first stop departure)</label>
          <input
            type="time"
            value={draft.startTime}
            onChange={(e) => update({ startTime: e.target.value })}
          />
        </div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="card-header">
          <h2 className="card-title">Stop-by-stop timetable</h2>
          <select value="" onChange={(e) => addStop(e.target.value)}>
            <option value="">+ Add stop…</option>
            {availableStops.map((s) => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>
        {availableStops.length === 0 && draft.stops.length === 0 && (
          <p className="muted" style={{ fontSize: 12 }}>
            No stops exist in {draft.district} yet — add some from Routes & stops first.
          </p>
        )}

        {draft.stops.length === 0 ? (
          <div className="empty-state">No stops yet — add the first one above.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th></th>
                  <th>Stop</th>
                  <th>Travel from prev (min)</th>
                  <th>Halt (min)</th>
                  <th>Arrival</th>
                  <th>Departure</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {schedule.map((row, i) => (
                  <tr key={`${row.stopId}-${i}`}>
                    <td className="muted">{i + 1}</td>
                    <td style={{ fontWeight: 600 }}>{stopsById[row.stopId]?.name ?? row.stopId}</td>
                    <td>
                      <input
                        type="number"
                        min={0}
                        disabled={i === 0}
                        value={i === 0 ? 0 : row.travelMinFromPrev}
                        onChange={(e) => updateStop(i, { travelMinFromPrev: Number(e.target.value) })}
                        style={{ width: 70 }}
                      />
                    </td>
                    <td>
                      <input
                        type="number"
                        min={0}
                        value={row.haltMin}
                        onChange={(e) => updateStop(i, { haltMin: Number(e.target.value) })}
                        style={{ width: 70 }}
                      />
                    </td>
                    <td className="muted">{formatMinutesAsClock(row.arrivalMin)}</td>
                    <td style={{ fontWeight: 600 }}>{formatMinutesAsClock(row.departureMin)}</td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <button type="button" onClick={() => moveStop(i, -1)} disabled={i === 0} title="Move up">↑</button>{' '}
                      <button type="button" onClick={() => moveStop(i, 1)} disabled={i === schedule.length - 1} title="Move down">↓</button>{' '}
                      <button type="button" onClick={() => removeStop(i)} title="Remove" style={{ color: '#C62828' }}>✕</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="card">
        <h2 className="card-title" style={{ marginBottom: 8 }}>Add another route</h2>
        <p className="muted" style={{ marginTop: 0, fontSize: 12 }}>
          Starts a new route in {draft.district}, ready for you to add stops and timing.
        </p>
        <div style={{ display: 'flex', gap: 8, maxWidth: 360 }}>
          <input
            value={newId}
            onChange={(e) => setNewId(e.target.value.toUpperCase())}
            placeholder="New route ID, e.g. R88"
            style={{ flex: 1, padding: '8px 10px', border: '1px solid var(--border)', borderRadius: 8, fontSize: 14 }}
          />
          <button
            className="btn btn-primary"
            disabled={!newId.trim() || existingIds.includes(newId.trim())}
            onClick={createNewRoute}
          >
            + Add route
          </button>
        </div>
        {newId.trim() && existingIds.includes(newId.trim()) && (
          <div className="error-text" style={{ marginTop: 6 }}>That ID is already in use.</div>
        )}
      </div>
    </>
  )
}
