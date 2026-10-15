import { Router } from 'express'
import { db } from '../db.js'

const router = Router()

const upsertStmt = db.prepare(`
  INSERT INTO announcements (id, title, body, severity, district, createdAt)
  VALUES (@id, @title, @body, @severity, @district, @createdAt)
  ON CONFLICT(id) DO UPDATE SET title=excluded.title, body=excluded.body, severity=excluded.severity, district=excluded.district
`)

router.get('/', (req, res) => {
  res.json(db.prepare('SELECT * FROM announcements ORDER BY createdAt DESC').all())
})

router.put('/:id', (req, res) => {
  const { title, body, severity, district, createdAt } = req.body
  if (!title?.trim() || !body?.trim() || !district) {
    return res.status(400).json({ error: 'title, body and district are required' })
  }
  const existing = db.prepare('SELECT createdAt FROM announcements WHERE id = ?').get(req.params.id)
  upsertStmt.run({
    id: req.params.id,
    title: title.trim(),
    body: body.trim(),
    severity: severity === 'WARNING' ? 'WARNING' : 'INFO',
    district,
    createdAt: existing?.createdAt ?? createdAt ?? new Date().toISOString(),
  })
  res.json(db.prepare('SELECT * FROM announcements WHERE id = ?').get(req.params.id))
})

router.delete('/:id', (req, res) => {
  db.prepare('DELETE FROM announcements WHERE id = ?').run(req.params.id)
  res.status(204).end()
})

export default router
