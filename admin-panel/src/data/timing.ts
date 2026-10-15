import type { RouteStopTiming } from './types'

export interface StopScheduleRow {
  stopId: string
  travelMinFromPrev: number
  haltMin: number
  arrivalMin: number // minutes since midnight
  departureMin: number // minutes since midnight
}

function parseHHMM(hhmm: string): number {
  const [h, m] = hhmm.split(':').map(Number)
  return (h || 0) * 60 + (m || 0)
}

export function formatMinutesAsClock(totalMin: number): string {
  const normalized = ((totalMin % 1440) + 1440) % 1440
  const h = Math.floor(normalized / 60)
  const m = normalized % 60
  const period = h < 12 ? 'AM' : 'PM'
  const h12 = h % 12 === 0 ? 12 : h % 12
  return `${h12}:${String(m).padStart(2, '0')} ${period}`
}

/** Walks a route's stop timings into an absolute-time schedule, like a train timetable. */
export function buildSchedule(startTime: string, stops: RouteStopTiming[]): StopScheduleRow[] {
  let clock = parseHHMM(startTime)
  const rows: StopScheduleRow[] = []
  for (const stop of stops) {
    clock += stop.travelMinFromPrev
    const arrivalMin = clock
    clock += stop.haltMin
    const departureMin = clock
    rows.push({ stopId: stop.stopId, travelMinFromPrev: stop.travelMinFromPrev, haltMin: stop.haltMin, arrivalMin, departureMin })
  }
  return rows
}

export function totalDurationMin(stops: RouteStopTiming[]): number {
  return stops.reduce((sum, s) => sum + s.travelMinFromPrev + s.haltMin, 0)
}

export function formatDuration(totalMin: number): string {
  const h = Math.floor(totalMin / 60)
  const m = totalMin % 60
  if (h === 0) return `${m} min`
  if (m === 0) return `${h} hr`
  return `${h} hr ${m} min`
}

export function emptyStopTiming(stopId: string, isFirst: boolean): RouteStopTiming {
  return { stopId, travelMinFromPrev: isFirst ? 0 : 5, haltMin: 1 }
}
