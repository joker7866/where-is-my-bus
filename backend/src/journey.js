import { db, routeFromRow } from './db.js'

function parseHHMM(hhmm) {
  const [h, m] = String(hhmm).split(':').map(Number)
  return (h || 0) * 60 + (m || 0)
}

export function clockStr(totalMin) {
  const normalized = ((totalMin % 1440) + 1440) % 1440
  const h = Math.floor(normalized / 60)
  const m = normalized % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

/** Walks a route's per-stop timing into absolute minutes-since-midnight, same math as
 * admin-panel/src/data/timing.ts, just ported server-side so search can use it too. */
export function buildSchedule(startTime, stops) {
  let clock = parseHHMM(startTime)
  return stops.map((stop) => {
    clock += stop.travelMinFromPrev
    const arrivalMin = clock
    clock += stop.haltMin
    const departureMin = clock
    return { stopId: stop.stopId, arrivalMin, departureMin }
  })
}

function allRoutes() {
  return db.prepare('SELECT * FROM routes ORDER BY shortName').all().map(routeFromRow)
}

/**
 * Resolves a "from"/"to" query to a stop row. Accepts a stop id, an exact English/Tamil
 * name, an exact city name, or a substring match against any of those — a lightweight
 * stand-in for full fuzzy/alias matching (Tamil-typed-in-English, spelling tolerance),
 * which is a later phase.
 */
export function resolveStop(query) {
  if (!query) return null
  const raw = String(query).trim()
  if (!raw) return null
  const stops = db.prepare('SELECT * FROM stops').all()

  const byId = stops.find((s) => s.id === raw.toUpperCase())
  if (byId) return byId

  const q = raw.toLowerCase()
  const exact = stops.find(
    (s) => s.name.toLowerCase() === q || s.tamilName === raw || s.city.toLowerCase() === q || s.village.toLowerCase() === q,
  )
  if (exact) return exact

  const partial = stops.find(
    (s) =>
      s.name.toLowerCase().includes(q) ||
      s.tamilName.includes(raw) ||
      s.city.toLowerCase().includes(q) ||
      s.village.toLowerCase().includes(q),
  )
  return partial || null
}

/** Direct services: routes whose stop sequence contains `from` strictly before `to`. */
export function findDirect(fromStopId, toStopId) {
  const results = []
  for (const route of allRoutes()) {
    const fromIndex = route.stopIds.indexOf(fromStopId)
    const toIndex = route.stopIds.indexOf(toStopId)
    if (fromIndex === -1 || toIndex === -1 || fromIndex >= toIndex) continue

    const schedule = buildSchedule(route.startTime, route.stops)
    results.push({
      route,
      fromIndex,
      toIndex,
      departureMin: schedule[fromIndex].departureMin,
      arrivalMin: schedule[toIndex].arrivalMin,
      durationMin: schedule[toIndex].arrivalMin - schedule[fromIndex].departureMin,
    })
  }
  results.sort((a, b) => a.departureMin - b.departureMin)
  return results
}

const MIN_TRANSFER_WAIT_MIN = 5
const MAX_TRANSFER_WAIT_MIN = 90

/**
 * One-transfer connections: ride routeA from `from` to some shared stop, then routeB from
 * that stop to `to`, with a plausible wait in between. O(routes^2 * stops) — fine at this
 * data scale; would need indexing for a large real network.
 */
export function findConnecting(fromStopId, toStopId, maxResults = 6) {
  const routes = allRoutes()
  const results = []

  for (const routeA of routes) {
    const fromIndex = routeA.stopIds.indexOf(fromStopId)
    if (fromIndex === -1) continue
    const scheduleA = buildSchedule(routeA.startTime, routeA.stops)

    for (let ti = fromIndex + 1; ti < routeA.stopIds.length; ti++) {
      const transferStopId = routeA.stopIds[ti]
      if (transferStopId === toStopId) continue // would be a direct trip, already covered
      const arrivalAtTransfer = scheduleA[ti].arrivalMin

      for (const routeB of routes) {
        if (routeB.id === routeA.id) continue
        const transferIndexB = routeB.stopIds.indexOf(transferStopId)
        const toIndexB = routeB.stopIds.indexOf(toStopId)
        if (transferIndexB === -1 || toIndexB === -1 || transferIndexB >= toIndexB) continue

        const scheduleB = buildSchedule(routeB.startTime, routeB.stops)
        const departureFromTransfer = scheduleB[transferIndexB].departureMin
        const waitMin = departureFromTransfer - arrivalAtTransfer
        if (waitMin < MIN_TRANSFER_WAIT_MIN || waitMin > MAX_TRANSFER_WAIT_MIN) continue

        results.push({
          transferStopId,
          waitMin,
          totalDurationMin: scheduleB[toIndexB].arrivalMin - scheduleA[fromIndex].departureMin,
          legs: [
            {
              route: routeA,
              fromIndex,
              toIndex: ti,
              departureMin: scheduleA[fromIndex].departureMin,
              arrivalMin: arrivalAtTransfer,
            },
            {
              route: routeB,
              fromIndex: transferIndexB,
              toIndex: toIndexB,
              departureMin: departureFromTransfer,
              arrivalMin: scheduleB[toIndexB].arrivalMin,
            },
          ],
        })
      }
    }
  }

  results.sort((a, b) => a.totalDurationMin - b.totalDurationMin)
  return results.slice(0, maxResults)
}
