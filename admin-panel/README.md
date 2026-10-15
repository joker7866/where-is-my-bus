# Where is my Bus — Admin panel

Web admin console for the "Where is my Bus" transit app: dashboard, route and
stop editing, bulk CSV upload, passenger report review, and user/sharer
management. Built with React + Vite + TypeScript, backed by the
[`../backend`](../backend) API — edits made here reach the companion Android
app (see [`../README.md`](../README.md)) the next time it refreshes.

The seed data (stops, routes, Live/Recent/Timetable status) mirrors the
Android app's bundled fallback data, so both start from the same fictional
network: the Coimbatore routes (70A, 45, 12, 96, all `LOCAL`/point-to-point)
and the longer example route 515, Tirupattur → Karaikudi via Pillayarpatti
— a `LONG_DISTANCE` route spanning the Sivaganga district.

## Features

- **Dashboard** — live-style stat cards (buses live, sharers, route
  coverage, open reports) and route-coverage / latest-reports panels,
  computed from the actual editable data, not hardcoded numbers.
- **Routes & stops** — a list page for quick add/edit/delete, plus two
  dedicated detail pages (click any row) modeled on train running-status
  apps:
  - **Route detail** — a start time and a stop-by-stop timetable: add,
    reorder, and remove stops, and set each one's travel time from the
    previous stop and halt (waiting) time. Arrival/departure clock times
    are computed live as you type, like a train timetable. Also sets the
    route's **type** — `Local` (point-to-point, within a district) or
    `Long-distance`, which reveals distance (km) and a free-text "via"
    field (e.g. "via Pillayarpatti") shown in the app.
  - **Stop detail** — every route serving that stop, with its computed
    arrival/departure time there and a link to its full timetable.
  - Every stop also has a **district** (the primary region, used for
    filtering everywhere), a **city/town**, and an optional **village**
    for rural stops — e.g. Pillayarpatti is a village near Karaikudi, in
    the Sivaganga district.
- **Upload data** — bulk-import stops or routes from a CSV file, with a
  downloadable template, row-by-row validation preview (missing fields,
  unknown stop IDs, bad coordinates), and an explicit confirm step that only
  imports valid rows.
- **Reports** — review passenger-submitted issues (stop fix, no show,
  breakdown, and feedback sent from the app's Feedback screen), filter by
  status, mark resolved / reopen.
- **News & updates** — post, edit, and delete announcements shown in the
  app's News tab (e.g. service alerts, new stops), with an Info/Warning
  severity.
- **Users & sharers** — manage admin accounts and rider sharers: add, edit
  role, enable/disable, delete.
- A **district selector** in the sidebar — populated dynamically from
  whatever districts actually exist in your stops, not a hardcoded list —
  filters the dashboard, routes & stops, reports, and news to that region.
  "All districts" shows everything.
- A **connection indicator** at the bottom of the sidebar shows whether
  you're actually talking to the backend ("Connected to server") or working
  offline against bundled seed data.

Every edit updates the screen immediately (optimistic UI) and fires the
matching API call in the background. If the backend isn't running, the
sidebar shows "Offline — local only" and edits stay local for that session
instead of silently vanishing.

## Project layout

```
admin-panel/src/
  data/
    types.ts        Stop/BusRoute/Report/AppUser/Announcement types
    mockData.ts      Bundled fallback seed (used if the backend is unreachable)
    api.ts           fetch() wrapper for the backend's REST API
    store.tsx        React Context — fetches from the backend on load, optimistic dispatch + sync
    DistrictContext.tsx  Selected-district filter, shared across pages (empty = all)
    csv.ts           Minimal CSV parse/write (no external dependency)
    format.ts        Date/label formatting helpers
    timing.ts        Route timetable math: start time + per-stop timing → arrival/departure clock times
  components/
    Sidebar.tsx      Nav + district selector + connection indicator
    StatusBadge.tsx
  pages/
    Dashboard.tsx
    RoutesStops.tsx
    RouteDetail.tsx  Per-route timetable editor
    StopDetail.tsx   Routes serving a stop, with computed timing
    UploadData.tsx
    Reports.tsx
    Announcements.tsx
    UsersSharers.tsx
```

## Run

Start the backend first (see [`../backend/README.md`](../backend/README.md)):

```
cd backend && npm install && npm start
```

Then, in another terminal:

```
cd admin-panel
npm install
npm run dev
```

Opens at `http://localhost:5173` (or the next free port). `npm run build`
type-checks and produces a production build in `dist/`. If you run the
backend somewhere other than `http://localhost:4000`, set `VITE_API_BASE_URL`
before starting the dev server.

## Next steps (post mock-data phase)

- Add authentication (the sidebar currently shows a hardcoded "Admin / Super
  admin" — there's no login, and the backend's API has no auth either).
- Swap the hand-rolled CSV parser for GTFS-format import if you need to
  ingest real transit agency data.
- Have the backend also own live bus GPS positions, not just the route/stop
  catalog — right now bus movement is still simulated locally in the app.
