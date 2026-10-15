import { Router } from 'express'
import { db, registerDistrict, stopFromRow } from '../db.js'

const router = Router()

const PLACE_TYPES = ['VILLAGE', 'TOWN', 'CITY', 'BUS_STAND', 'LANDMARK']

const upsertStmt = db.prepare(`
  INSERT INTO stops (id, name, tamilName, lat, lng, district, city, village, placeType)
  VALUES (@id, @name, @tamilName, @lat, @lng, @district, @city, @village, @placeType)
  ON CONFLICT(id) DO UPDATE SET
    name=excluded.name, tamilName=excluded.tamilName, lat=excluded.lat, lng=excluded.lng,
    district=excluded.district, city=excluded.city, village=excluded.village, placeType=excluded.placeType
`)

router.get('/', (req, res) => {
  const rows = db.prepare('SELECT * FROM stops ORDER BY name').all()
  res.json(rows.map(stopFromRow))
})

router.put('/:id', (req, res) => {
  const stop = { ...req.body, id: req.params.id }
  if (!stop.name || typeof stop.lat !== 'number' || typeof stop.lng !== 'number' || !stop.district) {
    return res.status(400).json({ error: 'name, lat, lng and district are required' })
  }
  const placeType = PLACE_TYPES.includes(stop.placeType) ? stop.placeType : 'TOWN'
  upsertStmt.run({ tamilName: '', city: '', village: '', ...stop, placeType })
  registerDistrict(stop.district)
  res.json(stopFromRow(db.prepare('SELECT * FROM stops WHERE id = ?').get(stop.id)))
})

router.delete('/:id', (req, res) => {
  const usedBy = db.prepare("SELECT id, shortName FROM routes WHERE stopIds LIKE ?").all(`%"${req.params.id}"%`)
  if (usedBy.length > 0) {
    return res.status(409).json({ error: 'Stop is used by a route', routes: usedBy.map((r) => r.shortName) })
  }
  db.prepare('DELETE FROM stops WHERE id = ?').run(req.params.id)
  res.status(204).end()
})

router.post('/import', (req, res) => {
  const stops = Array.isArray(req.body.stops) ? req.body.stops : []
  let count = 0
  for (const stop of stops) {
    if (!stop.id || !stop.name) continue
    const placeType = PLACE_TYPES.includes(stop.placeType) ? stop.placeType : 'TOWN'
    const merged = { tamilName: '', city: '', village: '', ...stop, placeType }
    upsertStmt.run(merged)
    registerDistrict(merged.district)
    count++
  }
  res.json({ imported: count })
})

export default router
