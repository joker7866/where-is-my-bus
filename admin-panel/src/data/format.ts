export function formatRelativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime()
  const diffMin = Math.round(diffMs / 60000)
  if (diffMin < 1) return 'just now'
  if (diffMin < 60) return `${diffMin} min ago`
  const diffHr = Math.round(diffMin / 60)
  if (diffHr < 24) return `${diffHr} hr ago`
  const diffDay = Math.round(diffHr / 24)
  return `${diffDay} day${diffDay === 1 ? '' : 's'} ago`
}

export function formatClock(date: Date = new Date()): string {
  return date.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })
}

const REPORT_LABEL: Record<string, string> = {
  STOP_FIX: 'Stop fix',
  NO_SHOW: 'No show',
  BREAKDOWN: 'Breakdown',
  FEEDBACK: 'Feedback',
}

export function reportTypeLabel(type: string): string {
  return REPORT_LABEL[type] ?? type
}

const PLACE_TYPE_LABEL: Record<string, string> = {
  VILLAGE: 'Village',
  TOWN: 'Town',
  CITY: 'City',
  BUS_STAND: 'Bus stand',
  LANDMARK: 'Landmark',
}

export function placeTypeLabel(placeType: string): string {
  return PLACE_TYPE_LABEL[placeType] ?? placeType
}
