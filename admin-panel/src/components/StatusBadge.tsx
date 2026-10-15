import type { LiveMode } from '../data/types'

const LABEL: Record<LiveMode, string> = {
  LIVE: 'Live',
  RECENT: 'Recent',
  TIMETABLE: 'Timetable only',
}

export default function StatusBadge({ mode }: { mode: LiveMode }) {
  return (
    <span className={`badge ${mode.toLowerCase()}`}>
      <span className="dot" />
      {LABEL[mode]}
    </span>
  )
}
