# Where is my Bus — Backend

Small REST API that both the [admin panel](../admin-panel) and the
[Android app](../app) now talk to, so route/stop edits made in the admin
panel actually reach the app. Node + Express + SQLite (via Node's built-in
`node:sqlite` — no native dependency to install, no `better-sqlite3` build
step).

This is still a mock-data system, just a shared one now: there's no auth,
and the SQLite file is a single local database, not a production deployment.

## Run

```
cd backend
npm install
npm start          # or: npm run dev (auto-restarts on file changes)
```

Listens on `http://localhost:4000`. On first run it creates
`data/wheresmybus.sqlite` and seeds it with the same stops/routes the
Android app and admin panel ship with by default, so everything starts in
sync.

- Admin panel (Vite dev server) calls it at `http://localhost:4000/api` by
  default — override with a `VITE_API_BASE_URL` env var if you run the
  backend elsewhere.
- Android app (emulator) calls it at `http://10.0.2.2:4000/api` — that's the
  emulator's alias for the host machine's localhost. On a physical device,
  change `RemoteConfig.BASE_URL` in the app to your machine's LAN IP.

## API

| Method | Path                  | Notes                                   |
|--------|-----------------------|------------------------------------------|
| GET    | `/api/health`         | Liveness check                           |
| GET    | `/api/stops`          | All stops                                |
| PUT    | `/api/stops/:id`      | Create or replace a stop                 |
| DELETE | `/api/stops/:id`      | 409 if a route still references it       |
| POST   | `/api/stops/import`   | Bulk upsert, body `{ stops: Stop[] }`    |
| GET    | `/api/routes`         | All routes                               |
| PUT    | `/api/routes/:id`     | Create or replace a route                |
| DELETE | `/api/routes/:id`     |                                           |
| POST   | `/api/routes/import`  | Bulk upsert, body `{ routes: Route[] }`  |
| GET    | `/api/reports`        | All passenger reports (stop fixes, no-shows, breakdowns, feedback) |
| POST   | `/api/reports`        | Create one — the app's Feedback screen uses this, body `{ type, message, routeId?, stopId? }` |
| PUT    | `/api/reports/:id/status` | Body `{ status: "OPEN" \| "RESOLVED" }` |
| GET    | `/api/users`          | All users/sharers                        |
| PUT    | `/api/users/:id`      | Create or replace a user                 |
| DELETE | `/api/users/:id`      |                                           |
| GET    | `/api/announcements`  | All news/update posts, newest first      |
| PUT    | `/api/announcements/:id` | Create or replace a post              |
| DELETE | `/api/announcements/:id` |                                        |
| GET    | `/api/districts`      | All district names, sorted               |
| POST   | `/api/districts`      | Add one, body `{ name }` (idempotent)    |
| DELETE | `/api/districts/:name` | 409 if a stop or route still uses it    |

Districts are also auto-registered the first time a stop or route is saved
with a new one — the explicit `POST`/sidebar "+" button is for naming a
district *before* anything uses it yet.

`Stop`/`Route`/`Report`/`User` shapes match `admin-panel/src/data/types.ts`,
and `liveMode` is one of `LIVE` / `RECENT` / `TIMETABLE` (matching the
Android app's Kotlin enum exactly, no translation needed).

A route's stop sequence has two representations in the API response:
- **`stops`** — the source of truth, an ordered array of
  `{ stopId, travelMinFromPrev, haltMin }`. This is what the admin panel's
  Route Detail page edits: travel time from the previous stop and halt
  (waiting) time at each stop, like a train timetable. Sending a route via
  `PUT`/`POST .../import` should include `stops`; `stopIds` is ignored on
  write and always re-derived server-side.
- **`stopIds`** — a plain ordered array of stop IDs, derived from `stops`.
  Kept for the Android app, which only needs the sequence (its ETA timeline
  is computed from live bus position + distance, not from this timetable).

**Geography**: every stop has a `district` (the primary filter/grouping
field — e.g. "Coimbatore", "Sivaganga"), a `city`/town name, and an
optional `village` for rural stops. A route has its own `district` too
(its primary region — a route can only belong to one, even a long one that
physically crosses towns within that district) plus `routeType`
(`LOCAL` or `LONG_DISTANCE`), `distanceKm`, and `viaTowns` (free text, e.g.
"via Pillayarpatti") — the fields that distinguish a short point-to-point
town route from a longer intercity one. `announcements` also carry
`district` so news posts can be scoped the same way.

Older databases had a single `city` column on `routes`/`announcements` that
was really being used as a district — it's renamed to `district` in place
on first start (see `renameColumnIfNeeded` in `src/db.js`), so no existing
data is lost.
Routes written before `stops` existed get a synthesized default timetable
on first read (5 min travel / 1 min halt between stops) so there's always
something to view and fine-tune.

## What's intentionally simple

- `stopIds`/`stops` are stored as JSON text columns on the route row rather
  than a proper join table — fine at this scale, would need normalizing for
  a real multi-route-per-stop-position query workload.
- CORS is wide open (`Access-Control-Allow-Origin: *`) — there's no auth to
  protect yet.
- No real migration tool — new columns are added with a small
  `ensureColumn` helper in `src/db.js` that checks `PRAGMA table_info` and
  runs `ALTER TABLE ... ADD COLUMN` if missing, so existing
  `data/wheresmybus.sqlite` files upgrade in place on next start.
