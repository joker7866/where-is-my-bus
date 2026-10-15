import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useStore } from '../data/store.tsx'
import { searchJourney } from '../data/api'
import { formatDuration } from '../data/timing'
import type { JourneySearchConnectingOption, JourneySearchDirectOption, JourneySearchLeg, JourneySearchResponse } from '../data/types'

type Status = { kind: 'idle' } | { kind: 'loading' } | { kind: 'error'; message: string } | { kind: 'result'; data: JourneySearchResponse }

export default function SearchPreview() {
  const { state } = useStore()
  const navigate = useNavigate()
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [status, setStatus] = useState<Status>({ kind: 'idle' })

  const stopNames = useMemo(() => state.stops.map((s) => s.name), [state.stops])

  async function runSearch() {
    if (!from.trim() || !to.trim()) return
    setStatus({ kind: 'loading' })
    try {
      const data = await searchJourney(from.trim(), to.trim())
      setStatus({ kind: 'result', data })
    } catch (err) {
      setStatus({ kind: 'error', message: err instanceof Error ? err.message : 'Search failed' })
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1 className="page-title">Search preview</h1>
          <p className="page-subtitle">
            Test the From → To rider search (direct + one-transfer connecting) against the live database —
            the same endpoint the Android app calls.
          </p>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="form-grid">
          <div className="field" style={{ margin: 0 }}>
            <label>From</label>
            <input
              list="search-preview-stops"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
              placeholder="e.g. Devakottai"
            />
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label>To</label>
            <input
              list="search-preview-stops"
              value={to}
              onChange={(e) => setTo(e.target.value)}
              placeholder="e.g. Madurai Mattuthavani"
            />
          </div>
          <datalist id="search-preview-stops">
            {stopNames.map((name) => (
              <option key={name} value={name} />
            ))}
          </datalist>
        </div>
        <button
          className="btn btn-primary"
          disabled={!from.trim() || !to.trim() || status.kind === 'loading'}
          onClick={runSearch}
          style={{ marginTop: 12 }}
        >
          {status.kind === 'loading' ? 'Searching…' : 'Run search'}
        </button>
      </div>

      {status.kind === 'error' && (
        <div className="card">
          <div className="error-text">{status.message}</div>
        </div>
      )}

      {status.kind === 'result' && (
        <ResultsView data={status.data} onRouteClick={(id) => navigate(`/route/${id}`)} />
      )}
    </>
  )
}

function ResultsView({ data, onRouteClick }: { data: JourneySearchResponse; onRouteClick: (id: string) => void }) {
  if (data.direct.length === 0 && data.connecting.length === 0) {
    return (
      <div className="card">
        <div className="empty-state">
          No DEMO bus services found between {data.from.name} and {data.to.name} yet.
        </div>
      </div>
    )
  }

  return (
    <>
      {data.direct.length > 0 && (
        <div className="card" style={{ marginBottom: 20 }}>
          <h2 className="card-title" style={{ marginBottom: 12 }}>
            Direct ({data.direct.length})
          </h2>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Route</th>
                  <th>Operator</th>
                  <th>Type</th>
                  <th>Depart</th>
                  <th>Arrive</th>
                  <th>Duration</th>
                  <th>Source</th>
                </tr>
              </thead>
              <tbody>
                {data.direct.map((option, i) => (
                  <DirectRow key={i} option={option} onClick={() => onRouteClick(option.route.id)} />
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {data.connecting.length > 0 && (
        <div className="card">
          <h2 className="card-title" style={{ marginBottom: 12 }}>
            Connecting via one transfer ({data.connecting.length})
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {data.connecting.map((option, i) => (
              <ConnectingRow key={i} option={option} onRouteClick={onRouteClick} />
            ))}
          </div>
        </div>
      )}
    </>
  )
}

function DirectRow({ option, onClick }: { option: JourneySearchDirectOption; onClick: () => void }) {
  return (
    <tr onClick={onClick} style={{ cursor: 'pointer' }}>
      <td>
        <span className="route-chip" style={{ background: '#DCEFE2', color: option.route.colorHex }}>
          {option.route.shortName}
        </span>{' '}
        {option.route.busName || option.route.longName}
      </td>
      <td className="muted">{option.route.operator || '—'}</td>
      <td className="muted">{option.route.busType} · {option.route.acType}</td>
      <td>{option.departureTime}</td>
      <td>{option.arrivalTime}</td>
      <td>{formatDuration(option.durationMin)}</td>
      <td>
        {(option.route.source === 'DEMO' || !option.route.verified) ? (
          <span className="badge" style={{ background: '#FCEFD8', color: '#7A5200' }}>DEMO</span>
        ) : (
          <span className="badge live">{option.route.source}</span>
        )}
      </td>
    </tr>
  )
}

function ConnectingRow({
  option,
  onRouteClick,
}: {
  option: JourneySearchConnectingOption
  onRouteClick: (id: string) => void
}) {
  return (
    <div style={{ border: '1px solid var(--border)', borderRadius: 12, padding: 14 }}>
      {option.legs.map((leg, i) => (
        <LegLine key={i} leg={leg} onClick={() => onRouteClick(leg.route.id)} />
      ))}
      <div className="muted" style={{ fontSize: 12, marginTop: 6 }}>
        Transfer wait {option.waitMin} min · Total {formatDuration(option.totalDurationMin)}
      </div>
    </div>
  )
}

function LegLine({ leg, onClick }: { leg: JourneySearchLeg; onClick: () => void }) {
  return (
    <div
      onClick={onClick}
      style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer', padding: '4px 0' }}
    >
      <span className="route-chip" style={{ background: '#DCEFE2', color: leg.route.colorHex }}>
        {leg.route.shortName}
      </span>
      <span>{leg.route.busName || leg.route.longName}</span>
      <span className="muted" style={{ fontSize: 12 }}>
        {leg.departureTime} → {leg.arrivalTime}
      </span>
      {(leg.route.source === 'DEMO' || !leg.route.verified) && (
        <span className="badge" style={{ background: '#FCEFD8', color: '#7A5200' }}>DEMO</span>
      )}
    </div>
  )
}
