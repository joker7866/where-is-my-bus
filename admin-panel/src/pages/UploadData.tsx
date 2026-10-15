import { useMemo, useRef, useState } from 'react'
import { useStore } from '../data/store.tsx'
import { useDistrict } from '../data/DistrictContext.tsx'
import { downloadCsv, parseCsv, rowsToObjects, toCsv } from '../data/csv'
import { emptyStopTiming } from '../data/timing'
import type { AcType, BusRoute, BusType, DataSource, LiveMode, OperatorType, PlaceType, RouteType, Stop } from '../data/types'

type UploadType = 'stops' | 'routes'

interface ParsedRow<T> {
  raw: Record<string, string>
  value: T | null
  errors: string[]
}

const LIVE_MODE_VALUES: LiveMode[] = ['LIVE', 'RECENT', 'TIMETABLE']
const ROUTE_TYPE_VALUES: RouteType[] = ['LOCAL', 'LONG_DISTANCE']
const PLACE_TYPE_VALUES: PlaceType[] = ['VILLAGE', 'TOWN', 'CITY', 'BUS_STAND', 'LANDMARK']
const OPERATOR_TYPE_VALUES: OperatorType[] = ['GOVERNMENT', 'PRIVATE']
const BUS_TYPE_VALUES: BusType[] = ['VILLAGE', 'TOWN', 'MOFUSSIL', 'EXPRESS', 'DELUXE', 'ULTRA_DELUXE', 'MINI', 'SPECIAL']
const AC_TYPE_VALUES: AcType[] = ['AC', 'NON_AC']
const DATA_SOURCE_VALUES: DataSource[] = ['DEMO', 'OFFICIAL', 'COMMUNITY']

export default function UploadData() {
  const { state, dispatch } = useStore()
  const { district } = useDistrict()
  const [type, setType] = useState<UploadType>('stops')
  const [fileName, setFileName] = useState<string | null>(null)
  const [stopRows, setStopRows] = useState<ParsedRow<Stop>[] | null>(null)
  const [routeRows, setRouteRows] = useState<ParsedRow<BusRoute>[] | null>(null)
  const [summary, setSummary] = useState<string | null>(null)
  const [dragOver, setDragOver] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const existingStopIds = useMemo(() => new Set(state.stops.map((s) => s.id)), [state.stops])

  function reset() {
    setStopRows(null)
    setRouteRows(null)
    setSummary(null)
    setFileName(null)
  }

  function handleFile(file: File) {
    setSummary(null)
    setFileName(file.name)
    const reader = new FileReader()
    reader.onload = () => {
      const text = String(reader.result ?? '')
      const objects = rowsToObjects(parseCsv(text))
      if (type === 'stops') {
        setStopRows(objects.map(parseStopRow))
        setRouteRows(null)
      } else {
        setRouteRows(objects.map((o) => parseRouteRow(o, existingStopIds)))
        setStopRows(null)
      }
    }
    reader.readAsText(file)
  }

  function parseStopRow(raw: Record<string, string>): ParsedRow<Stop> {
    const errors: string[] = []
    const id = raw.id?.trim().toUpperCase()
    if (!id) errors.push('missing id')
    const name = raw.name?.trim()
    if (!name) errors.push('missing name')
    const lat = Number(raw.lat)
    if (!raw.lat || Number.isNaN(lat)) errors.push('invalid lat')
    const lng = Number(raw.lng)
    if (!raw.lng || Number.isNaN(lng)) errors.push('invalid lng')
    const rowDistrict = raw.district?.trim() || district
    if (!rowDistrict) errors.push('missing district')
    const placeType = (raw.placeType?.trim().toUpperCase() as PlaceType) || 'TOWN'
    if (!PLACE_TYPE_VALUES.includes(placeType)) errors.push(`unknown placeType "${raw.placeType}"`)

    if (errors.length > 0 || !id) return { raw, value: null, errors }
    return {
      raw,
      errors,
      value: {
        id,
        name: name ?? '',
        tamilName: raw.tamilName?.trim() ?? '',
        lat,
        lng,
        district: rowDistrict,
        city: raw.city?.trim() ?? '',
        village: raw.village?.trim() ?? '',
        placeType,
      },
    }
  }

  function parseRouteRow(raw: Record<string, string>, stopIds: Set<string>): ParsedRow<BusRoute> {
    const errors: string[] = []
    const id = raw.id?.trim().toUpperCase()
    if (!id) errors.push('missing id')
    const shortName = raw.shortName?.trim()
    if (!shortName) errors.push('missing shortName')
    const longName = raw.longName?.trim()
    if (!longName) errors.push('missing longName')
    const liveMode = (raw.liveMode?.trim().toUpperCase() as LiveMode) || 'TIMETABLE'
    if (!LIVE_MODE_VALUES.includes(liveMode)) errors.push(`unknown liveMode "${raw.liveMode}"`)
    const routeType = (raw.routeType?.trim().toUpperCase() as RouteType) || 'LOCAL'
    if (!ROUTE_TYPE_VALUES.includes(routeType)) errors.push(`unknown routeType "${raw.routeType}"`)
    const rowDistrict = raw.district?.trim() || district
    if (!rowDistrict) errors.push('missing district')
    const stopIdList = (raw.stopIds ?? '')
      .split('|')
      .map((s) => s.trim().toUpperCase())
      .filter(Boolean)
    const unknownStops = stopIdList.filter((sid) => !stopIds.has(sid))
    if (unknownStops.length > 0) errors.push(`unknown stop id(s): ${unknownStops.join(', ')}`)
    const operatorType = (raw.operatorType?.trim().toUpperCase() as OperatorType) || 'GOVERNMENT'
    if (!OPERATOR_TYPE_VALUES.includes(operatorType)) errors.push(`unknown operatorType "${raw.operatorType}"`)
    const busType = (raw.busType?.trim().toUpperCase() as BusType) || 'TOWN'
    if (!BUS_TYPE_VALUES.includes(busType)) errors.push(`unknown busType "${raw.busType}"`)
    const acType = (raw.acType?.trim().toUpperCase() as AcType) || 'NON_AC'
    if (!AC_TYPE_VALUES.includes(acType)) errors.push(`unknown acType "${raw.acType}"`)
    const source = (raw.source?.trim().toUpperCase() as DataSource) || 'DEMO'
    if (!DATA_SOURCE_VALUES.includes(source)) errors.push(`unknown source "${raw.source}"`)

    if (errors.length > 0 || !id) return { raw, value: null, errors }
    return {
      raw,
      errors,
      value: {
        id,
        shortName: shortName ?? '',
        longName: longName ?? '',
        busName: raw.busName?.trim() ?? '',
        colorHex: raw.colorHex?.trim() || '#145C45',
        stops: stopIdList.map((sid, i) => emptyStopTiming(sid, i === 0)),
        startTime: raw.startTime?.trim() || '06:00',
        liveMode,
        district: rowDistrict,
        routeType,
        distanceKm: Number(raw.distanceKm) || 0,
        viaTowns: raw.viaTowns?.trim() ?? '',
        operator: raw.operator?.trim() ?? '',
        operatorType,
        busType,
        acType,
        source,
        verified: raw.verified?.trim().toLowerCase() === 'true',
        busesLive: Number(raw.busesLive) || 0,
        sharerCount: Number(raw.sharerCount) || 0,
        lastSeenMinutesAgo: Number(raw.lastSeenMinutesAgo) || 0,
        timetableEtaMinutes: Number(raw.timetableEtaMinutes) || 0,
        delayMinutes: Number(raw.delayMinutes) || 0,
      },
    }
  }

  function confirmImport() {
    if (type === 'stops' && stopRows) {
      const valid = stopRows.map((r) => r.value).filter((v): v is Stop => v !== null)
      dispatch({ type: 'IMPORT_STOPS', stops: valid })
      setSummary(`${valid.length} stop${valid.length === 1 ? '' : 's'} imported, ${stopRows.length - valid.length} skipped.`)
      setStopRows(null)
    } else if (type === 'routes' && routeRows) {
      const valid = routeRows.map((r) => r.value).filter((v): v is BusRoute => v !== null)
      dispatch({ type: 'IMPORT_ROUTES', routes: valid })
      setSummary(`${valid.length} route${valid.length === 1 ? '' : 's'} imported, ${routeRows.length - valid.length} skipped.`)
      setRouteRows(null)
    }
    setFileName(null)
  }

  function downloadTemplate() {
    if (type === 'stops') {
      downloadCsv(
        'stops-template.csv',
        toCsv(
          ['id', 'name', 'tamilName', 'lat', 'lng', 'district', 'city', 'village', 'placeType'],
          [['PMD', 'Peelamedu', 'பீளமேடு', '11.0260', '77.0010', 'Coimbatore', 'Coimbatore', '', 'TOWN']],
        ),
      )
    } else {
      downloadCsv(
        'routes-template.csv',
        toCsv(
          [
            'id', 'shortName', 'longName', 'busName', 'stopIds', 'startTime', 'liveMode', 'district',
            'routeType', 'distanceKm', 'viaTowns', 'operator', 'operatorType', 'busType', 'acType', 'source', 'verified',
            'colorHex', 'busesLive', 'sharerCount', 'lastSeenMinutesAgo', 'timetableEtaMinutes', 'delayMinutes',
          ],
          [[
            'R88', '88', 'Gandhipuram → Peelamedu', '', 'GPM|PMD', '06:00', 'TIMETABLE', 'Coimbatore',
            'LOCAL', '0', '', 'TNSTC', 'GOVERNMENT', 'TOWN', 'NON_AC', 'DEMO', 'false',
            '#145C45', '0', '0', '0', '15', '0',
          ]],
        ),
      )
    }
  }

  const rows = type === 'stops' ? stopRows : routeRows
  const validCount = rows?.filter((r) => r.value !== null).length ?? 0
  const errorCount = (rows?.length ?? 0) - validCount

  return (
    <>
      <div className="page-header">
        <div>
          <h1 className="page-title">Upload data</h1>
          <p className="page-subtitle">Bulk-import stops or routes from a CSV file</p>
        </div>
      </div>

      <div className="tabs">
        <button
          className={`tab ${type === 'stops' ? 'active' : ''}`}
          onClick={() => {
            setType('stops')
            reset()
          }}
        >
          Stops
        </button>
        <button
          className={`tab ${type === 'routes' ? 'active' : ''}`}
          onClick={() => {
            setType('routes')
            reset()
          }}
        >
          Routes
        </button>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <div className="toolbar" style={{ marginBottom: 0 }}>
          <p className="muted" style={{ margin: 0, fontSize: 13 }}>
            {type === 'stops'
              ? 'Columns: id, name, tamilName, lat, lng, district, city, village (village optional). District defaults to the sidebar filter if left blank.'
              : 'Columns: id, shortName, longName, stopIds (pipe-separated stop IDs, in order), startTime (HH:MM), liveMode, district, routeType (LOCAL or LONG_DISTANCE), distanceKm, viaTowns, and optional numeric fields. Per-stop travel/halt times default to 5/1 min — fine-tune them on the route\'s timetable page after import.'}
          </p>
          <button className="btn btn-outline btn-sm" onClick={downloadTemplate}>
            Download template
          </button>
        </div>
      </div>

      <div
        className={`dropzone ${dragOver ? 'drag' : ''}`}
        onClick={() => fileInputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault()
          setDragOver(true)
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => {
          e.preventDefault()
          setDragOver(false)
          const file = e.dataTransfer.files[0]
          if (file) handleFile(file)
        }}
      >
        <div style={{ fontSize: 28, marginBottom: 8 }}>⬆️</div>
        <div style={{ fontWeight: 600, color: '#1a1a1a' }}>
          {fileName ? fileName : 'Click or drop a CSV file here'}
        </div>
        <div style={{ fontSize: 12, marginTop: 4 }}>
          {type === 'stops' ? 'Importing stops' : 'Importing routes'} for upload
        </div>
        <input
          ref={fileInputRef}
          type="file"
          accept=".csv,text/csv"
          style={{ display: 'none' }}
          onChange={(e) => {
            const file = e.target.files?.[0]
            if (file) handleFile(file)
            e.target.value = ''
          }}
        />
      </div>

      {summary && <div className="import-summary">{summary}</div>}

      {rows && rows.length > 0 && (
        <div className="card" style={{ marginTop: 20 }}>
          <div className="card-header">
            <h2 className="card-title">
              Preview — {validCount} valid, {errorCount} with errors
            </h2>
            <button className="btn btn-primary" disabled={validCount === 0} onClick={confirmImport}>
              Confirm import
            </button>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Row</th>
                  <th>ID</th>
                  <th>{type === 'stops' ? 'Name' : 'Short / Long name'}</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r, i) => (
                  <tr key={i}>
                    <td className="muted">{i + 1}</td>
                    <td>{r.raw.id || <span className="muted">—</span>}</td>
                    <td>
                      {type === 'stops' ? r.raw.name : `${r.raw.shortName ?? ''} — ${r.raw.longName ?? ''}`}
                    </td>
                    <td>
                      {r.value ? (
                        <span className="badge resolved">Ready</span>
                      ) : (
                        <span className="error-text">{r.errors.join('; ')}</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  )
}
