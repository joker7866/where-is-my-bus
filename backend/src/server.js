import express from 'express'
import stopsRouter from './routes/stops.js'
import routesRouter from './routes/routes.js'
import reportsRouter from './routes/reports.js'
import usersRouter from './routes/users.js'
import announcementsRouter from './routes/announcements.js'
import districtsRouter from './routes/districts.js'
import searchRouter from './routes/search.js'

const PORT = process.env.PORT || 4000

const app = express()
app.use(express.json())

// Hand-rolled CORS: the admin panel (Vite dev server) and the Android app both
// call this API from a different origin, so every response needs these headers.
app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type')
  if (req.method === 'OPTIONS') return res.sendStatus(204)
  next()
})

app.get('/api/health', (req, res) => res.json({ status: 'ok', time: new Date().toISOString() }))

app.use('/api/stops', stopsRouter)
app.use('/api/routes', routesRouter)
app.use('/api/reports', reportsRouter)
app.use('/api/users', usersRouter)
app.use('/api/announcements', announcementsRouter)
app.use('/api/districts', districtsRouter)
app.use('/api/search', searchRouter)

app.use((err, req, res, next) => {
  console.error(err)
  res.status(500).json({ error: 'internal server error' })
})

app.listen(PORT, () => {
  console.log(`Where is my Bus backend listening on http://localhost:${PORT}`)
  console.log(`Android emulator should use http://10.0.2.2:${PORT}`)
})
