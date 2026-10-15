import type { AppState, AppUser, BusRoute, Announcement, District, JourneySearchResponse, ReportStatus, Stop } from './types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:4000/api'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error || `Request failed: ${res.status}`)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export async function fetchAll(): Promise<AppState> {
  const [stops, routes, reports, users, announcements, districts] = await Promise.all([
    request<Stop[]>('/stops'),
    request<BusRoute[]>('/routes'),
    request<AppState['reports']>('/reports'),
    request<AppUser[]>('/users'),
    request<Announcement[]>('/announcements'),
    request<District[]>('/districts'),
  ])
  return { stops, routes, reports, users, announcements, districts }
}

export function checkHealth(): Promise<{ status: string }> {
  return request('/health')
}

export function searchJourney(from: string, to: string): Promise<JourneySearchResponse> {
  return request(`/search?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`)
}

export const api = {
  upsertStop: (stop: Stop) => request<Stop>(`/stops/${stop.id}`, { method: 'PUT', body: JSON.stringify(stop) }),
  deleteStop: (id: string) => request<void>(`/stops/${id}`, { method: 'DELETE' }),
  importStops: (stops: Stop[]) => request<{ imported: number }>('/stops/import', { method: 'POST', body: JSON.stringify({ stops }) }),

  upsertRoute: (route: BusRoute) => request<BusRoute>(`/routes/${route.id}`, { method: 'PUT', body: JSON.stringify(route) }),
  deleteRoute: (id: string) => request<void>(`/routes/${id}`, { method: 'DELETE' }),
  importRoutes: (routes: BusRoute[]) => request<{ imported: number }>('/routes/import', { method: 'POST', body: JSON.stringify({ routes }) }),

  setReportStatus: (id: string, status: ReportStatus) =>
    request(`/reports/${id}/status`, { method: 'PUT', body: JSON.stringify({ status }) }),

  upsertUser: (user: AppUser) => request<AppUser>(`/users/${user.id}`, { method: 'PUT', body: JSON.stringify(user) }),
  deleteUser: (id: string) => request<void>(`/users/${id}`, { method: 'DELETE' }),

  upsertAnnouncement: (a: Announcement) =>
    request<Announcement>(`/announcements/${a.id}`, { method: 'PUT', body: JSON.stringify(a) }),
  deleteAnnouncement: (id: string) => request<void>(`/announcements/${id}`, { method: 'DELETE' }),

  addDistrict: (name: District) =>
    request<{ name: string }>('/districts', { method: 'POST', body: JSON.stringify({ name }) }),
  deleteDistrict: (name: District) => request<void>(`/districts/${encodeURIComponent(name)}`, { method: 'DELETE' }),
}
