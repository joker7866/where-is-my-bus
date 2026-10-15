import { createContext, useContext, useEffect, useMemo, useReducer, useState } from 'react'
import type { ReactNode } from 'react'
import type { Announcement, AppState, AppUser, BusRoute, District, ReportStatus, Stop } from './types'
import { initialState } from './mockData'
import { api, fetchAll } from './api'

type Action =
  | { type: 'REPLACE_ALL'; state: AppState }
  | { type: 'UPSERT_STOP'; stop: Stop }
  | { type: 'DELETE_STOP'; id: string }
  | { type: 'UPSERT_ROUTE'; route: BusRoute }
  | { type: 'DELETE_ROUTE'; id: string }
  | { type: 'SET_REPORT_STATUS'; id: string; status: ReportStatus }
  | { type: 'UPSERT_USER'; user: AppUser }
  | { type: 'DELETE_USER'; id: string }
  | { type: 'IMPORT_STOPS'; stops: Stop[] }
  | { type: 'IMPORT_ROUTES'; routes: BusRoute[] }
  | { type: 'UPSERT_ANNOUNCEMENT'; announcement: Announcement }
  | { type: 'DELETE_ANNOUNCEMENT'; id: string }
  | { type: 'ADD_DISTRICT'; name: District }
  | { type: 'DELETE_DISTRICT'; name: District }
  | { type: 'RESET' }

function upsertById<T extends { id: string }>(list: T[], item: T): T[] {
  const index = list.findIndex((existing) => existing.id === item.id)
  if (index === -1) return [...list, item]
  const next = [...list]
  next[index] = item
  return next
}

function importById<T extends { id: string }>(list: T[], items: T[]): T[] {
  let next = list
  for (const item of items) next = upsertById(next, item)
  return next
}

function reducer(state: AppState, action: Action): AppState {
  switch (action.type) {
    case 'REPLACE_ALL':
      return action.state
    case 'UPSERT_STOP':
      return { ...state, stops: upsertById(state.stops, action.stop) }
    case 'DELETE_STOP':
      return { ...state, stops: state.stops.filter((s) => s.id !== action.id) }
    case 'UPSERT_ROUTE':
      return { ...state, routes: upsertById(state.routes, action.route) }
    case 'DELETE_ROUTE':
      return { ...state, routes: state.routes.filter((r) => r.id !== action.id) }
    case 'SET_REPORT_STATUS':
      return {
        ...state,
        reports: state.reports.map((r) => (r.id === action.id ? { ...r, status: action.status } : r)),
      }
    case 'UPSERT_USER':
      return { ...state, users: upsertById(state.users, action.user) }
    case 'DELETE_USER':
      return { ...state, users: state.users.filter((u) => u.id !== action.id) }
    case 'IMPORT_STOPS':
      return { ...state, stops: importById(state.stops, action.stops) }
    case 'IMPORT_ROUTES':
      return { ...state, routes: importById(state.routes, action.routes) }
    case 'UPSERT_ANNOUNCEMENT':
      return { ...state, announcements: upsertById(state.announcements, action.announcement) }
    case 'DELETE_ANNOUNCEMENT':
      return { ...state, announcements: state.announcements.filter((a) => a.id !== action.id) }
    case 'ADD_DISTRICT':
      return state.districts.includes(action.name)
        ? state
        : { ...state, districts: [...state.districts, action.name].sort() }
    case 'DELETE_DISTRICT':
      return { ...state, districts: state.districts.filter((d) => d !== action.name) }
    case 'RESET':
      return initialState
    default:
      return state
  }
}

/** Fires the matching backend call for an action already applied optimistically to local state. */
async function syncToBackend(action: Action): Promise<void> {
  switch (action.type) {
    case 'UPSERT_STOP':
      await api.upsertStop(action.stop)
      return
    case 'DELETE_STOP':
      await api.deleteStop(action.id)
      return
    case 'UPSERT_ROUTE':
      await api.upsertRoute(action.route)
      return
    case 'DELETE_ROUTE':
      await api.deleteRoute(action.id)
      return
    case 'SET_REPORT_STATUS':
      await api.setReportStatus(action.id, action.status)
      return
    case 'UPSERT_USER':
      await api.upsertUser(action.user)
      return
    case 'DELETE_USER':
      await api.deleteUser(action.id)
      return
    case 'IMPORT_STOPS':
      await api.importStops(action.stops)
      return
    case 'IMPORT_ROUTES':
      await api.importRoutes(action.routes)
      return
    case 'UPSERT_ANNOUNCEMENT':
      await api.upsertAnnouncement(action.announcement)
      return
    case 'DELETE_ANNOUNCEMENT':
      await api.deleteAnnouncement(action.id)
      return
    case 'ADD_DISTRICT':
      await api.addDistrict(action.name)
      return
    case 'DELETE_DISTRICT':
      await api.deleteDistrict(action.name)
      return
    default:
      return
  }
}

export type ConnectionStatus = 'connecting' | 'online' | 'offline'

interface StoreContextValue {
  state: AppState
  dispatch: (action: Action) => void
  connection: ConnectionStatus
  lastError: string | null
}

const StoreContext = createContext<StoreContextValue | null>(null)

export function StoreProvider({ children }: { children: ReactNode }) {
  const [state, rawDispatch] = useReducer(reducer, initialState)
  const [connection, setConnection] = useState<ConnectionStatus>('connecting')
  const [lastError, setLastError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    fetchAll()
      .then((remote) => {
        if (cancelled) return
        rawDispatch({ type: 'REPLACE_ALL', state: remote })
        setConnection('online')
      })
      .catch(() => {
        if (cancelled) return
        setConnection('offline')
      })
    return () => {
      cancelled = true
    }
  }, [])

  function dispatch(action: Action) {
    rawDispatch(action) // optimistic local update, so the UI never waits on the network
    if (action.type === 'REPLACE_ALL' || action.type === 'RESET') return
    syncToBackend(action)
      .then(() => {
        setConnection('online')
        setLastError(null)
      })
      .catch((err) => {
        setConnection('offline')
        setLastError(err instanceof Error ? err.message : 'Sync failed')
      })
  }

  const value = useMemo(
    () => ({ state, dispatch, connection, lastError }),
    [state, connection, lastError],
  )
  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>
}

export function useStore(): StoreContextValue {
  const ctx = useContext(StoreContext)
  if (!ctx) throw new Error('useStore must be used within a StoreProvider')
  return ctx
}
