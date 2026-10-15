import { DatabaseSync } from 'node:sqlite'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { seedAnnouncements, seedReports, seedRoutes, seedStops, seedUsers } from './seedData.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const dbPath = path.join(__dirname, '..', 'data', 'wheresmybus.sqlite')

export const db = new DatabaseSync(dbPath)

db.exec(`
  CREATE TABLE IF NOT EXISTS stops (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    tamilName TEXT NOT NULL DEFAULT '',
    lat REAL NOT NULL,
    lng REAL NOT NULL,
    district TEXT NOT NULL DEFAULT '',
    city TEXT NOT NULL DEFAULT '',
    village TEXT NOT NULL DEFAULT '',
    placeType TEXT NOT NULL DEFAULT 'TOWN'
  );

  CREATE TABLE IF NOT EXISTS routes (
    id TEXT PRIMARY KEY,
    shortName TEXT NOT NULL,
    longName TEXT NOT NULL,
    busName TEXT NOT NULL DEFAULT '',
    colorHex TEXT NOT NULL DEFAULT '#145C45',
    stopIds TEXT NOT NULL DEFAULT '[]',
    stops TEXT NOT NULL DEFAULT '[]',
    startTime TEXT NOT NULL DEFAULT '06:00',
    liveMode TEXT NOT NULL DEFAULT 'TIMETABLE',
    district TEXT NOT NULL DEFAULT '',
    routeType TEXT NOT NULL DEFAULT 'LOCAL',
    distanceKm REAL NOT NULL DEFAULT 0,
    viaTowns TEXT NOT NULL DEFAULT '',
    operator TEXT NOT NULL DEFAULT '',
    operatorType TEXT NOT NULL DEFAULT 'GOVERNMENT',
    busType TEXT NOT NULL DEFAULT 'TOWN',
    acType TEXT NOT NULL DEFAULT 'NON_AC',
    source TEXT NOT NULL DEFAULT 'DEMO',
    verified INTEGER NOT NULL DEFAULT 0,
    busesLive INTEGER NOT NULL DEFAULT 0,
    sharerCount INTEGER NOT NULL DEFAULT 0,
    lastSeenMinutesAgo INTEGER NOT NULL DEFAULT 0,
    timetableEtaMinutes INTEGER NOT NULL DEFAULT 0,
    delayMinutes INTEGER NOT NULL DEFAULT 0
  );

  CREATE TABLE IF NOT EXISTS reports (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    message TEXT NOT NULL,
    routeId TEXT,
    stopId TEXT,
    createdAt TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'OPEN'
  );

  CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'SHARER',
    tripsShared INTEGER NOT NULL DEFAULT 0,
    joinedAt TEXT NOT NULL,
    active INTEGER NOT NULL DEFAULT 1
  );

  CREATE TABLE IF NOT EXISTS announcements (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    severity TEXT NOT NULL DEFAULT 'INFO',
    district TEXT NOT NULL DEFAULT '',
    createdAt TEXT NOT NULL
  );

  CREATE TABLE IF NOT EXISTS districts (
    name TEXT PRIMARY KEY
  );
`)

function columnNames(table) {
  return db.prepare(`PRAGMA table_info(${table})`).all().map((c) => c.name)
}
function ensureColumn(table, column, ddl) {
  if (!columnNames(table).includes(column)) {
    db.exec(`ALTER TABLE ${table} ADD COLUMN ${ddl}`)
  }
}
/** `city` on routes/announcements was really "which district" — rename it in place for
 * databases created before `district` existed, instead of losing that data. */
function renameColumnIfNeeded(table, from, to) {
  const cols = columnNames(table)
  if (!cols.includes(to) && cols.includes(from)) {
    db.exec(`ALTER TABLE ${table} RENAME COLUMN ${from} TO ${to}`)
  }
}

renameColumnIfNeeded('routes', 'city', 'district')
renameColumnIfNeeded('announcements', 'city', 'district')

ensureColumn('routes', 'stops', "stops TEXT NOT NULL DEFAULT '[]'")
ensureColumn('routes', 'startTime', "startTime TEXT NOT NULL DEFAULT '06:00'")
ensureColumn('routes', 'district', "district TEXT NOT NULL DEFAULT ''")
ensureColumn('routes', 'routeType', "routeType TEXT NOT NULL DEFAULT 'LOCAL'")
ensureColumn('routes', 'distanceKm', "distanceKm REAL NOT NULL DEFAULT 0")
ensureColumn('routes', 'viaTowns', "viaTowns TEXT NOT NULL DEFAULT ''")
ensureColumn('routes', 'busName', "busName TEXT NOT NULL DEFAULT ''")
ensureColumn('announcements', 'district', "district TEXT NOT NULL DEFAULT ''")
ensureColumn('stops', 'district', "district TEXT NOT NULL DEFAULT ''")
ensureColumn('stops', 'village', "village TEXT NOT NULL DEFAULT ''")
ensureColumn('stops', 'placeType', "placeType TEXT NOT NULL DEFAULT 'TOWN'")
ensureColumn('routes', 'operator', "operator TEXT NOT NULL DEFAULT ''")
ensureColumn('routes', 'operatorType', "operatorType TEXT NOT NULL DEFAULT 'GOVERNMENT'")
ensureColumn('routes', 'busType', "busType TEXT NOT NULL DEFAULT 'TOWN'")
ensureColumn('routes', 'acType', "acType TEXT NOT NULL DEFAULT 'NON_AC'")
ensureColumn('routes', 'source', "source TEXT NOT NULL DEFAULT 'DEMO'")
ensureColumn('routes', 'verified', "verified INTEGER NOT NULL DEFAULT 0")
// If a stop predates `district`, it still has its old `city` value — treat that as the district
// (that's what it was used for) and leave `city` holding the actual town/city name going forward.
db.exec(`UPDATE stops SET district = city WHERE district = ''`)

function seedIfEmpty() {
  const stopCount = db.prepare('SELECT COUNT(*) AS n FROM stops').get().n
  if (stopCount > 0) return

  const insertStop = db.prepare(
    'INSERT INTO stops (id, name, tamilName, lat, lng, district, city, village, placeType) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
  )
  for (const s of seedStops) {
    insertStop.run(s.id, s.name, s.tamilName, s.lat, s.lng, s.district, s.city, s.village ?? '', s.placeType ?? 'TOWN')
  }

  const insertRoute = db.prepare(`
    INSERT INTO routes (id, shortName, longName, busName, colorHex, stopIds, stops, startTime, liveMode, district, routeType, distanceKm, viaTowns, operator, operatorType, busType, acType, source, verified, busesLive, sharerCount, lastSeenMinutesAgo, timetableEtaMinutes, delayMinutes)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `)
  for (const r of seedRoutes) {
    insertRoute.run(
      r.id, r.shortName, r.longName, r.busName ?? '', r.colorHex, JSON.stringify(r.stops.map((s) => s.stopId)),
      JSON.stringify(r.stops), r.startTime, r.liveMode, r.district,
      r.routeType ?? 'LOCAL', r.distanceKm ?? 0, r.viaTowns ?? '',
      r.operator ?? '', r.operatorType ?? 'GOVERNMENT', r.busType ?? 'TOWN', r.acType ?? 'NON_AC',
      r.source ?? 'DEMO', r.verified ? 1 : 0,
      r.busesLive, r.sharerCount, r.lastSeenMinutesAgo, r.timetableEtaMinutes, r.delayMinutes,
    )
  }

  const insertReport = db.prepare(`
    INSERT INTO reports (id, type, message, routeId, stopId, createdAt, status) VALUES (?, ?, ?, ?, ?, ?, ?)
  `)
  for (const r of seedReports) {
    insertReport.run(r.id, r.type, r.message, r.routeId, r.stopId, r.createdAt, r.status)
  }

  const insertUser = db.prepare(`
    INSERT INTO users (id, name, email, role, tripsShared, joinedAt, active) VALUES (?, ?, ?, ?, ?, ?, ?)
  `)
  for (const u of seedUsers) {
    insertUser.run(u.id, u.name, u.email, u.role, u.tripsShared, u.joinedAt, u.active)
  }
}

function seedAnnouncementsIfEmpty() {
  const count = db.prepare('SELECT COUNT(*) AS n FROM announcements').get().n
  if (count > 0) return
  const insertAnnouncement = db.prepare(`
    INSERT INTO announcements (id, title, body, severity, district, createdAt) VALUES (?, ?, ?, ?, ?, ?)
  `)
  for (const a of seedAnnouncements) {
    insertAnnouncement.run(a.id, a.title, a.body, a.severity, a.district, a.createdAt)
  }
}

/** Upserts the pilot stops/routes (Devakottai <-> Madurai corridor) even into an
 * already-seeded database, so adding a new pilot region never requires wiping existing data. */
function ensurePilotData() {
  const pilotStopIds = new Set(['VLG', 'DVK', 'TPT', 'PLP', 'KKD', 'MLR', 'MDU'])
  const pilotRouteIds = new Set(['R515', 'R140', 'R141', 'RVLG1'])

  const insertStop = db.prepare(
    'INSERT OR IGNORE INTO stops (id, name, tamilName, lat, lng, district, city, village, placeType) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
  )
  for (const s of seedStops) {
    if (pilotStopIds.has(s.id)) {
      insertStop.run(s.id, s.name, s.tamilName, s.lat, s.lng, s.district, s.city, s.village ?? '', s.placeType ?? 'TOWN')
    }
  }

  const insertRoute = db.prepare(`
    INSERT OR IGNORE INTO routes (id, shortName, longName, busName, colorHex, stopIds, stops, startTime, liveMode, district, routeType, distanceKm, viaTowns, operator, operatorType, busType, acType, source, verified, busesLive, sharerCount, lastSeenMinutesAgo, timetableEtaMinutes, delayMinutes)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `)
  for (const r of seedRoutes) {
    if (pilotRouteIds.has(r.id)) {
      insertRoute.run(
        r.id, r.shortName, r.longName, r.busName ?? '', r.colorHex, JSON.stringify(r.stops.map((s) => s.stopId)),
        JSON.stringify(r.stops), r.startTime, r.liveMode, r.district,
        r.routeType ?? 'LOCAL', r.distanceKm ?? 0, r.viaTowns ?? '',
        r.operator ?? '', r.operatorType ?? 'GOVERNMENT', r.busType ?? 'TOWN', r.acType ?? 'NON_AC',
        r.source ?? 'DEMO', r.verified ? 1 : 0,
        r.busesLive, r.sharerCount, r.lastSeenMinutesAgo, r.timetableEtaMinutes, r.delayMinutes,
      )
    }
  }
}

seedIfEmpty()
seedAnnouncementsIfEmpty()
ensurePilotData()
// Pillayarpatti predates `placeType` and defaulted to TOWN — it's actually a village.
db.exec(`UPDATE stops SET placeType = 'VILLAGE' WHERE id = 'PLP' AND placeType = 'TOWN'`)

// Backfill the districts table from whatever districts already show up on stops/routes
// (seeded or pre-existing), so a district already in use is never missing from the list.
db.exec(`
  INSERT OR IGNORE INTO districts (name)
  SELECT district FROM stops WHERE district != ''
  UNION
  SELECT district FROM routes WHERE district != ''
`)

export function stopFromRow(row) {
  return row
}

export function routeFromRow(row) {
  const stopIds = JSON.parse(row.stopIds)
  let stops = JSON.parse(row.stops || '[]')
  if (stops.length === 0 && stopIds.length > 0) {
    // Legacy row from before per-stop timing existed — synthesize a reasonable default
    // timetable so the admin panel always has something to show and fine-tune.
    stops = stopIds.map((stopId, i) => ({
      stopId,
      travelMinFromPrev: i === 0 ? 0 : 5,
      haltMin: i === stopIds.length - 1 ? 0 : 1,
    }))
  }
  return {
    ...row,
    stopIds,
    stops,
    startTime: row.startTime || '06:00',
    routeType: row.routeType || 'LOCAL',
    distanceKm: Number(row.distanceKm) || 0,
    operator: row.operator || '',
    operatorType: row.operatorType || 'GOVERNMENT',
    busType: row.busType || 'TOWN',
    acType: row.acType || 'NON_AC',
    source: row.source || 'DEMO',
    verified: Boolean(row.verified),
    busesLive: Number(row.busesLive),
  }
}

export function userFromRow(row) {
  return { ...row, active: Boolean(row.active) }
}

const registerDistrictStmt = db.prepare('INSERT OR IGNORE INTO districts (name) VALUES (?)')
/** Auto-registers a district the first time it's used on a stop/route, so typing a new
 * one in those forms works immediately without a separate "add district" step. */
export function registerDistrict(name) {
  if (name) registerDistrictStmt.run(name)
}
