import { Router } from 'express'
import { stopFromRow } from '../db.js'
import { clockStr, findConnecting, findDirect, resolveStop } from '../journey.js'

const router = Router()

function routeSummary(route) {
  return {
    id: route.id,
    shortName: route.shortName,
    longName: route.longName,
    busName: route.busName,
    colorHex: route.colorHex,
    liveMode: route.liveMode,
    routeType: route.routeType,
    operator: route.operator,
    operatorType: route.operatorType,
    busType: route.busType,
    acType: route.acType,
    source: route.source,
    verified: route.verified,
    busesLive: route.busesLive,
    sharerCount: route.sharerCount,
    delayMinutes: route.delayMinutes,
  }
}

function leg(l) {
  return {
    route: routeSummary(l.route),
    fromStopId: l.route.stopIds[l.fromIndex],
    toStopId: l.route.stopIds[l.toIndex],
    departureMin: l.departureMin,
    departureTime: clockStr(l.departureMin),
    arrivalMin: l.arrivalMin,
    arrivalTime: clockStr(l.arrivalMin),
  }
}

router.get('/', (req, res) => {
  const fromQuery = req.query.from
  const toQuery = req.query.to
  if (!fromQuery || !toQuery) {
    return res.status(400).json({ error: 'from and to query params are required' })
  }

  const fromStop = resolveStop(String(fromQuery))
  const toStop = resolveStop(String(toQuery))
  if (!fromStop) return res.status(404).json({ error: `Could not find a stop matching "${fromQuery}"` })
  if (!toStop) return res.status(404).json({ error: `Could not find a stop matching "${toQuery}"` })
  if (fromStop.id === toStop.id) {
    return res.status(400).json({ error: 'From and To must be different stops' })
  }

  const direct = findDirect(fromStop.id, toStop.id)
  // Connecting results are only useful when there's no direct bus — surfacing both would
  // bury the simpler option the user actually wants.
  const connecting = direct.length > 0 ? [] : findConnecting(fromStop.id, toStop.id)

  res.json({
    from: stopFromRow(fromStop),
    to: stopFromRow(toStop),
    direct: direct.map((d) => ({
      ...leg(d),
      durationMin: d.durationMin,
    })),
    connecting: connecting.map((c) => ({
      transferStopId: c.transferStopId,
      waitMin: c.waitMin,
      totalDurationMin: c.totalDurationMin,
      legs: c.legs.map(leg),
    })),
  })
})

export default router
