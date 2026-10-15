import { Router } from 'express'
import { db, registerDistrict, routeFromRow } from '../db.js'

const router = Router()

const upsertStmt = db.prepare(`
  INSERT INTO routes (id, shortName, longName, busName, colorHex, stopIds, stops, startTime, liveMode, district, routeType, distanceKm, viaTowns, operator, operatorType, busType, acType, source, verified, busesLive, sharerCount, lastSeenMinutesAgo, timetableEtaMinutes, delayMinutes)
  VALUES (@id, @shortName, @longName, @busName, @colorHex, @stopIds, @stops, @startTime, @liveMode, @district, @routeType, @distanceKm, @viaTowns, @operator, @operatorType, @busType, @acType, @source, @verified, @busesLive, @sharerCount, @lastSeenMinutesAgo, @timetableEtaMinutes, @delayMinutes)
  ON CONFLICT(id) DO UPDATE SET
    shortName=excluded.shortName, longName=excluded.longName, busName=excluded.busName, colorHex=excluded.colorHex,
    stopIds=excluded.stopIds, stops=excluded.stops, startTime=excluded.startTime,
    liveMode=excluded.liveMode, district=excluded.district,
    routeType=excluded.routeType, distanceKm=excluded.distanceKm, viaTowns=excluded.viaTowns,
    operator=excluded.operator, operatorType=excluded.operatorType, busType=excluded.busType,
    acType=excluded.acType, source=excluded.source, verified=excluded.verified,
    busesLive=excluded.busesLive, sharerCount=excluded.sharerCount, lastSeenMinutesAgo=excluded.lastSeenMinutesAgo,
    timetableEtaMinutes=excluded.timetableEtaMinutes, delayMinutes=excluded.delayMinutes
`)

const ROUTE_TYPES = ['LOCAL', 'LONG_DISTANCE']
const OPERATOR_TYPES = ['GOVERNMENT', 'PRIVATE']
const BUS_TYPES = ['VILLAGE', 'TOWN', 'MOFUSSIL', 'EXPRESS', 'DELUXE', 'ULTRA_DELUXE', 'MINI', 'SPECIAL']
const AC_TYPES = ['AC', 'NON_AC']
const SOURCES = ['DEMO', 'OFFICIAL', 'COMMUNITY']

/** A stop's per-stop timing entry; stopId is the only strictly required field. */
function normalizeStopTiming(entry) {
  return {
    stopId: String(entry.stopId),
    travelMinFromPrev: Number(entry.travelMinFromPrev) || 0,
    haltMin: Number(entry.haltMin) || 0,
  }
}

function normalize(body, id) {
  // `stops` (per-stop timing, ordered) is the single source of truth for the stop
  // sequence; `stopIds` is derived from it so older clients (the Android app) that
  // only read `stopIds` keep working unchanged.
  const stops = Array.isArray(body.stops) && body.stops.length > 0
    ? body.stops.map(normalizeStopTiming)
    : (Array.isArray(body.stopIds) ? body.stopIds : []).map((stopId, i, arr) => ({
        stopId,
        travelMinFromPrev: i === 0 ? 0 : 5,
        haltMin: i === arr.length - 1 ? 0 : 1,
      }))

  return {
    id,
    shortName: body.shortName ?? '',
    longName: body.longName ?? '',
    busName: body.busName ?? '',
    colorHex: body.colorHex ?? '#145C45',
    stopIds: JSON.stringify(stops.map((s) => s.stopId)),
    stops: JSON.stringify(stops),
    startTime: body.startTime ?? '06:00',
    liveMode: body.liveMode ?? 'TIMETABLE',
    district: body.district ?? '',
    routeType: ROUTE_TYPES.includes(body.routeType) ? body.routeType : 'LOCAL',
    distanceKm: Number(body.distanceKm) || 0,
    viaTowns: body.viaTowns ?? '',
    operator: body.operator ?? '',
    operatorType: OPERATOR_TYPES.includes(body.operatorType) ? body.operatorType : 'GOVERNMENT',
    busType: BUS_TYPES.includes(body.busType) ? body.busType : 'TOWN',
    acType: AC_TYPES.includes(body.acType) ? body.acType : 'NON_AC',
    source: SOURCES.includes(body.source) ? body.source : 'DEMO',
    verified: body.verified ? 1 : 0,
    busesLive: Number(body.busesLive) || 0,
    sharerCount: Number(body.sharerCount) || 0,
    lastSeenMinutesAgo: Number(body.lastSeenMinutesAgo) || 0,
    timetableEtaMinutes: Number(body.timetableEtaMinutes) || 0,
    delayMinutes: Number(body.delayMinutes) || 0,
  }
}

router.get('/', (req, res) => {
  const rows = db.prepare('SELECT * FROM routes ORDER BY shortName').all()
  res.json(rows.map(routeFromRow))
})

router.put('/:id', (req, res) => {
  const route = normalize(req.body, req.params.id)
  if (!route.shortName || !route.longName || !route.district) {
    return res.status(400).json({ error: 'shortName, longName and district are required' })
  }
  upsertStmt.run(route)
  registerDistrict(route.district)
  res.json(routeFromRow(db.prepare('SELECT * FROM routes WHERE id = ?').get(route.id)))
})

router.delete('/:id', (req, res) => {
  db.prepare('DELETE FROM routes WHERE id = ?').run(req.params.id)
  res.status(204).end()
})

router.post('/import', (req, res) => {
  const routes = Array.isArray(req.body.routes) ? req.body.routes : []
  let count = 0
  for (const route of routes) {
    if (!route.id || !route.shortName) continue
    const normalized = normalize(route, route.id)
    upsertStmt.run(normalized)
    registerDistrict(normalized.district)
    count++
  }
  res.json({ imported: count })
})

export default router
