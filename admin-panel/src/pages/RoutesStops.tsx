import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useStore } from '../data/store.tsx'
import { useDistrict } from '../data/DistrictContext.tsx'
import StatusBadge from '../components/StatusBadge'
import { placeTypeLabel } from '../data/format'
import type { AcType, BusRoute, BusType, DataSource, LiveMode, OperatorType, PlaceType, RouteType, Stop } from '../data/types'

type Tab = 'routes' | 'stops'

function emptyRoute(district: string): BusRoute {
  return {
    id: '', shortName: '', longName: '', busName: '', colorHex: '#145C45', stops: [], startTime: '06:00',
    liveMode: 'TIMETABLE', district, routeType: 'LOCAL', distanceKm: 0, viaTowns: '',
    operator: '', operatorType: 'GOVERNMENT', busType: 'TOWN', acType: 'NON_AC', source: 'DEMO', verified: false,
    busesLive: 0, sharerCount: 0, lastSeenMinutesAgo: 0, timetableEtaMinutes: 10, delayMinutes: 0,
  }
}

function emptyStop(district: string): Stop {
  return { id: '', name: '', tamilName: '', lat: 0, lng: 0, district, city: '', village: '', placeType: 'TOWN' }
}

export default function RoutesStops() {
  const { state, dispatch } = useStore()
  const { district } = useDistrict()
  const navigate = useNavigate()
  const [tab, setTab] = useState<Tab>('routes')
  const [query, setQuery] = useState('')

  const [routeDraft, setRouteDraft] = useState<BusRoute | null>(null)
  const [stopDraft, setStopDraft] = useState<Stop | null>(null)
  const [selectedRouteIds, setSelectedRouteIds] = useState<Set<string>>(new Set())

  const knownDistricts = state.districts

  const routes = useMemo(
    () =>
      state.routes.filter(
        (r) =>
          (!district || r.district === district) &&
          (r.shortName.toLowerCase().includes(query.toLowerCase()) ||
            r.longName.toLowerCase().includes(query.toLowerCase())),
      ),
    [state.routes, district, query],
  )
  const stops = useMemo(
    () =>
      state.stops.filter(
        (s) => (!district || s.district === district) && s.name.toLowerCase().includes(query.toLowerCase()),
      ),
    [state.stops, district, query],
  )

  function saveRoute(route: BusRoute) {
    dispatch({ type: 'UPSERT_ROUTE', route })
    setRouteDraft(null)
    navigate(`/route/${route.id}`)
  }
  function deleteRoute(id: string) {
    if (confirm('Delete this route? This cannot be undone.')) dispatch({ type: 'DELETE_ROUTE', id })
  }
  function saveStop(stop: Stop) {
    dispatch({ type: 'UPSERT_STOP', stop })
    setStopDraft(null)
  }
  function deleteStop(id: string) {
    const usedBy = state.routes.filter((r) => r.stops.some((s) => s.stopId === id))
    if (usedBy.length > 0) {
      alert(`Can't delete — used by route${usedBy.length > 1 ? 's' : ''} ${usedBy.map((r) => r.shortName).join(', ')}.`)
      return
    }
    if (confirm('Delete this stop? This cannot be undone.')) dispatch({ type: 'DELETE_STOP', id })
  }

  function toggleRouteSelected(id: string) {
    setSelectedRouteIds((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }
  function toggleSelectAllRoutes() {
    setSelectedRouteIds((current) =>
      current.size === routes.length ? new Set() : new Set(routes.map((r) => r.id)),
    )
  }
  function bulkUpdateSelectedRoutes(patch: Partial<BusRoute>) {
    for (const route of routes) {
      if (!selectedRouteIds.has(route.id)) continue
      dispatch({ type: 'UPSERT_ROUTE', route: { ...route, ...patch } })
    }
    setSelectedRouteIds(new Set())
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1 className="page-title">Routes & stops</h1>
          <p className="page-subtitle">
            Add, edit and retire routes and stops{district ? ` in ${district}` : ' across all districts'}
          </p>
        </div>
      </div>

      <div className="tabs">
        <button className={`tab ${tab === 'routes' ? 'active' : ''}`} onClick={() => setTab('routes')}>
          Routes ({routes.length})
        </button>
        <button className={`tab ${tab === 'stops' ? 'active' : ''}`} onClick={() => setTab('stops')}>
          Stops ({stops.length})
        </button>
      </div>

      <div className="toolbar">
        <input
          className="search-input"
          placeholder={tab === 'routes' ? 'Search routes…' : 'Search stops…'}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        {tab === 'routes' ? (
          <button
            className="btn btn-primary"
            onClick={() => setRouteDraft(emptyRoute(district || knownDistricts[0] || ''))}
          >
            + Add route
          </button>
        ) : (
          <button
            className="btn btn-primary"
            onClick={() => setStopDraft(emptyStop(district || knownDistricts[0] || ''))}
          >
            + Add stop
          </button>
        )}
      </div>

      {tab === 'routes' ? (
        <div className="card">
          <p className="muted" style={{ marginTop: 0, fontSize: 12 }}>
            Click a route to manage its stop-by-stop timetable. Select rows to verify or publish several at once.
          </p>
          {selectedRouteIds.size > 0 && (
            <div
              style={{
                display: 'flex', alignItems: 'center', gap: 10, background: '#F0F6F2',
                borderRadius: 10, padding: '10px 14px', marginBottom: 12,
              }}
            >
              <span style={{ fontSize: 13, fontWeight: 600 }}>{selectedRouteIds.size} selected</span>
              <button
                className="btn btn-outline btn-sm"
                onClick={() => bulkUpdateSelectedRoutes({ verified: true })}
              >
                Mark Verified
              </button>
              <button
                className="btn btn-outline btn-sm"
                onClick={() => bulkUpdateSelectedRoutes({ source: 'OFFICIAL', verified: true })}
              >
                Publish as Official
              </button>
              <button
                className="btn btn-outline btn-sm"
                onClick={() => bulkUpdateSelectedRoutes({ source: 'DEMO', verified: false })}
              >
                Revert to Demo
              </button>
              <button className="link-btn" onClick={() => setSelectedRouteIds(new Set())}>
                Clear
              </button>
            </div>
          )}
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th style={{ width: 28 }}>
                    <input
                      type="checkbox"
                      checked={routes.length > 0 && selectedRouteIds.size === routes.length}
                      onChange={toggleSelectAllRoutes}
                    />
                  </th>
                  <th>Route</th>
                  <th>From → To</th>
                  <th>District</th>
                  <th>Type</th>
                  <th>Stops</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {routes.map((route) => (
                  <tr
                    key={route.id}
                    onClick={() => navigate(`/route/${route.id}`)}
                    style={{ cursor: 'pointer' }}
                  >
                    <td onClick={(e) => e.stopPropagation()}>
                      <input
                        type="checkbox"
                        checked={selectedRouteIds.has(route.id)}
                        onChange={() => toggleRouteSelected(route.id)}
                      />
                    </td>
                    <td>
                      <span className="route-chip" style={{ background: '#DCEFE2', color: route.colorHex }}>
                        {route.shortName}
                      </span>
                    </td>
                    <td>
                      {route.longName}
                      {route.busName && <div className="muted" style={{ fontSize: 12 }}>{route.busName}</div>}
                    </td>
                    <td className="muted">{route.district}</td>
                    <td>
                      <span className={`badge ${route.routeType === 'LONG_DISTANCE' ? 'recent' : 'timetable'}`}>
                        {route.routeType === 'LONG_DISTANCE' ? 'Long-distance' : 'Local'}
                      </span>
                    </td>
                    <td>{route.stops.length}</td>
                    <td>
                      <StatusBadge mode={route.liveMode} />
                      {(route.source === 'DEMO' || !route.verified) && (
                        <span className="badge" style={{ marginLeft: 6, background: '#FCEFD8', color: '#7A5200' }}>
                          DEMO
                        </span>
                      )}
                    </td>
                    <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }} onClick={(e) => e.stopPropagation()}>
                      <button className="btn btn-outline btn-sm" onClick={() => setRouteDraft(route)}>
                        Edit
                      </button>{' '}
                      <button className="btn btn-danger btn-sm" onClick={() => deleteRoute(route.id)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
                {routes.length === 0 && (
                  <tr>
                    <td colSpan={8} className="empty-state">
                      No routes match.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <div className="card">
          <p className="muted" style={{ marginTop: 0, fontSize: 12 }}>
            Click a stop to see which routes serve it and when.
          </p>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Stop</th>
                  <th>Tamil name</th>
                  <th>Type</th>
                  <th>District</th>
                  <th>City</th>
                  <th>Village</th>
                  <th>Coordinates</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {stops.map((stop) => (
                  <tr key={stop.id} onClick={() => navigate(`/stop/${stop.id}`)} style={{ cursor: 'pointer' }}>
                    <td>{stop.name}</td>
                    <td>{stop.tamilName}</td>
                    <td className="muted">{placeTypeLabel(stop.placeType)}</td>
                    <td className="muted">{stop.district}</td>
                    <td className="muted">{stop.city}</td>
                    <td className="muted">{stop.village || '—'}</td>
                    <td className="muted">
                      {stop.lat.toFixed(4)}, {stop.lng.toFixed(4)}
                    </td>
                    <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }} onClick={(e) => e.stopPropagation()}>
                      <button className="btn btn-outline btn-sm" onClick={() => setStopDraft(stop)}>
                        Edit
                      </button>{' '}
                      <button className="btn btn-danger btn-sm" onClick={() => deleteStop(stop.id)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
                {stops.length === 0 && (
                  <tr>
                    <td colSpan={8} className="empty-state">
                      No stops match.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {routeDraft && (
        <RouteModal
          initial={routeDraft}
          existingIds={state.routes.map((r) => r.id)}
          knownDistricts={knownDistricts}
          onCancel={() => setRouteDraft(null)}
          onSave={saveRoute}
        />
      )}
      {stopDraft && (
        <StopModal
          initial={stopDraft}
          existingIds={state.stops.map((s) => s.id)}
          knownDistricts={knownDistricts}
          onCancel={() => setStopDraft(null)}
          onSave={saveStop}
        />
      )}
    </>
  )
}

function RouteModal({
  initial,
  existingIds,
  knownDistricts,
  onCancel,
  onSave,
}: {
  initial: BusRoute
  existingIds: string[]
  knownDistricts: string[]
  onCancel: () => void
  onSave: (r: BusRoute) => void
}) {
  const [draft, setDraft] = useState<BusRoute>(initial)
  const isNew = !existingIds.includes(initial.id)
  const idTaken = isNew && existingIds.includes(draft.id)
  const canSave = draft.id.trim() && draft.shortName.trim() && draft.longName.trim() && draft.district.trim() && !idTaken

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>{isNew ? 'Add route' : `Edit ${initial.shortName}`}</h2>
        <p className="modal-subtitle">
          {isNew
            ? "Basic details — you'll add stops and timing next."
            : 'Basic details. Manage stops and timing from the route page.'}
        </p>

        <div className="form-grid">
          <div className="field">
            <label>Route ID</label>
            <input
              value={draft.id}
              disabled={!isNew}
              onChange={(e) => setDraft({ ...draft, id: e.target.value.toUpperCase() })}
              placeholder="e.g. R88"
            />
            {idTaken && <div className="error-text">That ID is already in use.</div>}
          </div>
          <div className="field">
            <label>Short name</label>
            <input
              value={draft.shortName}
              onChange={(e) => setDraft({ ...draft, shortName: e.target.value })}
              placeholder="e.g. 88"
            />
          </div>
          <div className="field span-2">
            <label>Long name</label>
            <input
              value={draft.longName}
              onChange={(e) => setDraft({ ...draft, longName: e.target.value })}
              placeholder="e.g. Gandhipuram → Peelamedu"
            />
          </div>
          <div className="field span-2">
            <label>Bus name (optional)</label>
            <input
              value={draft.busName}
              onChange={(e) => setDraft({ ...draft, busName: e.target.value })}
              placeholder="e.g. Sivaganga Express"
            />
          </div>
          <div className="field">
            <label>District</label>
            <input
              list="route-districts"
              value={draft.district}
              onChange={(e) => setDraft({ ...draft, district: e.target.value })}
              placeholder="e.g. Coimbatore"
            />
            <datalist id="route-districts">
              {knownDistricts.map((d) => (
                <option key={d} value={d} />
              ))}
            </datalist>
          </div>
          <div className="field">
            <label>Route type</label>
            <select
              value={draft.routeType}
              onChange={(e) => setDraft({ ...draft, routeType: e.target.value as RouteType })}
            >
              <option value="LOCAL">Local (point to point)</option>
              <option value="LONG_DISTANCE">Long-distance</option>
            </select>
          </div>

          {draft.routeType === 'LONG_DISTANCE' && (
            <>
              <div className="field">
                <label>Distance (km)</label>
                <input
                  type="number"
                  min={0}
                  step="0.5"
                  value={draft.distanceKm}
                  onChange={(e) => setDraft({ ...draft, distanceKm: Number(e.target.value) })}
                />
              </div>
              <div className="field">
                <label>Via</label>
                <input
                  value={draft.viaTowns}
                  onChange={(e) => setDraft({ ...draft, viaTowns: e.target.value })}
                  placeholder="e.g. via Pillayarpatti"
                />
              </div>
            </>
          )}

          <div className="field">
            <label>Operator</label>
            <input
              value={draft.operator}
              onChange={(e) => setDraft({ ...draft, operator: e.target.value })}
              placeholder="e.g. TNSTC"
            />
          </div>
          <div className="field">
            <label>Operator type</label>
            <select
              value={draft.operatorType}
              onChange={(e) => setDraft({ ...draft, operatorType: e.target.value as OperatorType })}
            >
              <option value="GOVERNMENT">Government</option>
              <option value="PRIVATE">Private</option>
            </select>
          </div>
          <div className="field">
            <label>Bus type</label>
            <select
              value={draft.busType}
              onChange={(e) => setDraft({ ...draft, busType: e.target.value as BusType })}
            >
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
          <div className="field">
            <label>AC / Non-AC</label>
            <select
              value={draft.acType}
              onChange={(e) => setDraft({ ...draft, acType: e.target.value as AcType })}
            >
              <option value="NON_AC">Non-AC</option>
              <option value="AC">AC</option>
            </select>
          </div>
          <div className="field">
            <label>Data source</label>
            <select
              value={draft.source}
              onChange={(e) => setDraft({ ...draft, source: e.target.value as DataSource })}
            >
              <option value="DEMO">Demo</option>
              <option value="OFFICIAL">Official</option>
              <option value="COMMUNITY">Community</option>
            </select>
          </div>
          <div className="field">
            <label>Verified</label>
            <select
              value={draft.verified ? 'yes' : 'no'}
              onChange={(e) => setDraft({ ...draft, verified: e.target.value === 'yes' })}
            >
              <option value="no">Not verified</option>
              <option value="yes">Verified</option>
            </select>
          </div>
          {(draft.source === 'DEMO' || !draft.verified) && (
            <div className="field span-2">
              <div className="banner-warning">
                This route is shown to riders as DEMO / unverified until you set source to Official or
                Community and mark it Verified — never present demo data as real TNSTC information.
              </div>
            </div>
          )}

          <div className="field">
            <label>Status</label>
            <select
              value={draft.liveMode}
              onChange={(e) => setDraft({ ...draft, liveMode: e.target.value as LiveMode })}
            >
              <option value="LIVE">Live</option>
              <option value="RECENT">Recent</option>
              <option value="TIMETABLE">Timetable only</option>
            </select>
          </div>
          <div className="field">
            <label>Color</label>
            <input
              type="color"
              value={draft.colorHex}
              onChange={(e) => setDraft({ ...draft, colorHex: e.target.value })}
            />
          </div>

          {draft.liveMode === 'LIVE' && (
            <>
              <div className="field">
                <label>Buses live</label>
                <input
                  type="number"
                  min={0}
                  value={draft.busesLive}
                  onChange={(e) => setDraft({ ...draft, busesLive: Number(e.target.value) })}
                />
              </div>
              <div className="field">
                <label>Sharers</label>
                <input
                  type="number"
                  min={0}
                  value={draft.sharerCount}
                  onChange={(e) => setDraft({ ...draft, sharerCount: Number(e.target.value) })}
                />
              </div>
              <div className="field">
                <label>Delay (min)</label>
                <input
                  type="number"
                  min={0}
                  value={draft.delayMinutes}
                  onChange={(e) => setDraft({ ...draft, delayMinutes: Number(e.target.value) })}
                />
              </div>
            </>
          )}
          {draft.liveMode === 'RECENT' && (
            <>
              <div className="field">
                <label>Sharers</label>
                <input
                  type="number"
                  min={0}
                  value={draft.sharerCount}
                  onChange={(e) => setDraft({ ...draft, sharerCount: Number(e.target.value) })}
                />
              </div>
              <div className="field">
                <label>Last seen (min ago)</label>
                <input
                  type="number"
                  min={0}
                  value={draft.lastSeenMinutesAgo}
                  onChange={(e) => setDraft({ ...draft, lastSeenMinutesAgo: Number(e.target.value) })}
                />
              </div>
            </>
          )}
          {draft.liveMode === 'TIMETABLE' && (
            <div className="field">
              <label>Scheduled ETA (min)</label>
              <input
                type="number"
                min={0}
                value={draft.timetableEtaMinutes}
                onChange={(e) => setDraft({ ...draft, timetableEtaMinutes: Number(e.target.value) })}
              />
            </div>
          )}
        </div>

        <div className="modal-actions">
          <button className="btn btn-outline" onClick={onCancel}>
            Cancel
          </button>
          <button className="btn btn-primary" disabled={!canSave} onClick={() => onSave(draft)}>
            {isNew ? 'Next: add stops →' : 'Save route'}
          </button>
        </div>
      </div>
    </div>
  )
}

function StopModal({
  initial,
  existingIds,
  knownDistricts,
  onCancel,
  onSave,
}: {
  initial: Stop
  existingIds: string[]
  knownDistricts: string[]
  onCancel: () => void
  onSave: (s: Stop) => void
}) {
  const [draft, setDraft] = useState<Stop>(initial)
  const isNew = !existingIds.includes(initial.id)
  const idTaken = isNew && existingIds.includes(draft.id)
  const canSave =
    draft.id.trim() && draft.name.trim() && draft.district.trim() && !idTaken &&
    !Number.isNaN(draft.lat) && !Number.isNaN(draft.lng)

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>{isNew ? 'Add stop' : `Edit ${initial.name}`}</h2>
        <p className="modal-subtitle">Stop details, shown in both languages on the rider app.</p>

        <div className="form-grid">
          <div className="field">
            <label>Stop ID</label>
            <input
              value={draft.id}
              disabled={!isNew}
              onChange={(e) => setDraft({ ...draft, id: e.target.value.toUpperCase() })}
              placeholder="e.g. PMD"
            />
            {idTaken && <div className="error-text">That ID is already in use.</div>}
          </div>
          <div className="field">
            <label>Name (English)</label>
            <input value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} />
          </div>
          <div className="field span-2">
            <label>Name (Tamil)</label>
            <input value={draft.tamilName} onChange={(e) => setDraft({ ...draft, tamilName: e.target.value })} />
          </div>
          <div className="field">
            <label>District</label>
            <input
              list="stop-districts"
              value={draft.district}
              onChange={(e) => setDraft({ ...draft, district: e.target.value })}
              placeholder="e.g. Coimbatore"
            />
            <datalist id="stop-districts">
              {knownDistricts.map((d) => (
                <option key={d} value={d} />
              ))}
            </datalist>
          </div>
          <div className="field">
            <label>City / town</label>
            <input
              value={draft.city}
              onChange={(e) => setDraft({ ...draft, city: e.target.value })}
              placeholder="e.g. Coimbatore"
            />
          </div>
          <div className="field">
            <label>Village (optional)</label>
            <input
              value={draft.village}
              onChange={(e) => setDraft({ ...draft, village: e.target.value })}
              placeholder="e.g. Pillayarpatti"
            />
          </div>
          <div className="field">
            <label>Place type</label>
            <select
              value={draft.placeType}
              onChange={(e) => setDraft({ ...draft, placeType: e.target.value as PlaceType })}
            >
              <option value="VILLAGE">Village</option>
              <option value="TOWN">Town</option>
              <option value="CITY">City</option>
              <option value="BUS_STAND">Bus stand</option>
              <option value="LANDMARK">Landmark</option>
            </select>
          </div>
          <div className="field">
            <label>Latitude</label>
            <input
              type="number"
              step="0.0001"
              value={draft.lat}
              onChange={(e) => setDraft({ ...draft, lat: Number(e.target.value) })}
            />
          </div>
          <div className="field">
            <label>Longitude</label>
            <input
              type="number"
              step="0.0001"
              value={draft.lng}
              onChange={(e) => setDraft({ ...draft, lng: Number(e.target.value) })}
            />
          </div>
        </div>

        <div className="modal-actions">
          <button className="btn btn-outline" onClick={onCancel}>
            Cancel
          </button>
          <button className="btn btn-primary" disabled={!canSave} onClick={() => onSave(draft)}>
            Save stop
          </button>
        </div>
      </div>
    </div>
  )
}
