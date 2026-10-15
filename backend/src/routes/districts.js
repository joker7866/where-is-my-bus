import { Router } from 'express'
import { db } from '../db.js'

const router = Router()

router.get('/', (req, res) => {
  const rows = db.prepare('SELECT name FROM districts ORDER BY name').all()
  res.json(rows.map((r) => r.name))
})

router.post('/', (req, res) => {
  const name = req.body.name?.trim()
  if (!name) {
    return res.status(400).json({ error: 'name is required' })
  }
  db.prepare('INSERT OR IGNORE INTO districts (name) VALUES (?)').run(name)
  res.status(201).json({ name })
})

router.delete('/:name', (req, res) => {
  const name = req.params.name
  const usedBy = [
    db.prepare('SELECT COUNT(*) AS n FROM stops WHERE district = ?').get(name).n,
    db.prepare('SELECT COUNT(*) AS n FROM routes WHERE district = ?').get(name).n,
  ]
  if (usedBy[0] > 0 || usedBy[1] > 0) {
    return res.status(409).json({ error: 'District is in use by stops or routes' })
  }
  db.prepare('DELETE FROM districts WHERE name = ?').run(name)
  res.status(204).end()
})

export default router
