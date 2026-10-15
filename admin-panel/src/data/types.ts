export type LiveMode = 'LIVE' | 'RECENT' | 'TIMETABLE'

/** Free text — e.g. "Coimbatore", "Sivaganga". The primary filter/grouping field. */
export type District = string

export type PlaceType = 'VILLAGE' | 'TOWN' | 'CITY' | 'BUS_STAND' | 'LANDMARK'

export interface Stop {
  id: string
  name: string
  tamilName: string
  lat: number
  lng: number
  district: District
  /** Town/city this stop is in, e.g. "Coimbatore", "Karaikudi". */
  city: string
  /** Village or locality, for rural stops — empty for stops directly in a city. */
  village: string
  placeType: PlaceType
}

export interface RouteStopTiming {
  stopId: string
  /** Minutes of travel from the previous stop (0 for the first stop). */
  travelMinFromPrev: number
  /** Minutes the bus waits at this stop before departing (0 for the last stop). */
  haltMin: number
}

export type RouteType = 'LOCAL' | 'LONG_DISTANCE'
export type OperatorType = 'GOVERNMENT' | 'PRIVATE'
export type BusType = 'VILLAGE' | 'TOWN' | 'MOFUSSIL' | 'EXPRESS' | 'DELUXE' | 'ULTRA_DELUXE' | 'MINI' | 'SPECIAL'
export type AcType = 'AC' | 'NON_AC'
/** DEMO data must never be presented as real/factual TNSTC information in the app —
 * keep it visibly flagged until an admin marks it OFFICIAL/COMMUNITY and verified. */
export type DataSource = 'DEMO' | 'OFFICIAL' | 'COMMUNITY'

export interface BusRoute {
  id: string
  shortName: string
  longName: string
  /** Named bus/service, e.g. "Sivaganga Express" — distinct from the route's From → To description. */
  busName: string
  colorHex: string
  /** Ordered stop sequence with per-stop timing — the single source of truth for stops. */
  stops: RouteStopTiming[]
  /** "HH:MM" departure time from the first stop. */
  startTime: string
  liveMode: LiveMode
  district: District
  /** Point-to-point town route vs. a longer intercity/inter-town service. */
  routeType: RouteType
  /** End-to-end distance in km — most meaningful for LONG_DISTANCE routes. */
  distanceKm: number
  /** Free text, e.g. "via Pillayarpatti" — shown for LONG_DISTANCE routes. */
  viaTowns: string
  /** Operating company name, e.g. "TNSTC", "Sivaganga Travels". */
  operator: string
  operatorType: OperatorType
  busType: BusType
  acType: AcType
  source: DataSource
  /** Admin-confirmed as real/accurate — false means it must be shown as unverified. */
  verified: boolean
  busesLive: number
  sharerCount: number
  lastSeenMinutesAgo: number
  timetableEtaMinutes: number
  delayMinutes: number
}

export type ReportType = 'STOP_FIX' | 'NO_SHOW' | 'BREAKDOWN' | 'FEEDBACK'
export type ReportStatus = 'OPEN' | 'RESOLVED'

export interface Report {
  id: string
  type: ReportType
  message: string
  routeId?: string
  stopId?: string
  createdAt: string
  status: ReportStatus
}

export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'SHARER'

export interface AppUser {
  id: string
  name: string
  email: string
  role: UserRole
  tripsShared: number
  joinedAt: string
  active: boolean
}

export type AnnouncementSeverity = 'INFO' | 'WARNING'

export interface Announcement {
  id: string
  title: string
  body: string
  severity: AnnouncementSeverity
  district: District
  createdAt: string
}

export interface JourneySearchRouteSummary {
  id: string
  shortName: string
  longName: string
  busName: string
  colorHex: string
  liveMode: LiveMode
  routeType: RouteType
  operator: string
  operatorType: OperatorType
  busType: BusType
  acType: AcType
  source: DataSource
  verified: boolean
  busesLive: number
  sharerCount: number
  delayMinutes: number
}

export interface JourneySearchLeg {
  route: JourneySearchRouteSummary
  fromStopId: string
  toStopId: string
  departureMin: number
  departureTime: string
  arrivalMin: number
  arrivalTime: string
}

export interface JourneySearchDirectOption {
  route: JourneySearchRouteSummary
  fromStopId: string
  toStopId: string
  departureMin: number
  departureTime: string
  arrivalMin: number
  arrivalTime: string
  durationMin: number
}

export interface JourneySearchConnectingOption {
  transferStopId: string
  waitMin: number
  totalDurationMin: number
  legs: JourneySearchLeg[]
}

export interface JourneySearchResponse {
  from: Stop
  to: Stop
  direct: JourneySearchDirectOption[]
  connecting: JourneySearchConnectingOption[]
}

export interface AppState {
  stops: Stop[]
  routes: BusRoute[]
  reports: Report[]
  users: AppUser[]
  announcements: Announcement[]
  districts: District[]
}
