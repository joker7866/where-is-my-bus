import { Router } from 'express'
import { db, userFromRow } from '../db.js'

const router = Router()

const upsertStmt = db.prepare(`
  INSERT INTO users (id, name, email, role, tripsShared, joinedAt, active)
  VALUES (@id, @name, @email, @role, @tripsShared, @joinedAt, @active)
  ON CONFLICT(id) DO UPDATE SET
    name=excluded.name, email=excluded.email, role=excluded.role,
    tripsShared=excluded.tripsShared, joinedAt=excluded.joinedAt, active=excluded.active
`)

router.get('/', (req, res) => {
  res.json(db.prepare('SELECT * FROM users ORDER BY name').all().map(userFromRow))
})

router.put('/:id', (req, res) => {
  const body = req.body
  if (!body.name || !body.email) {
    return res.status(400).json({ error: 'name and email are required' })
  }
  upsertStmt.run({
    id: req.params.id,
    name: body.name,
    email: body.email,
    role: body.role ?? 'SHARER',
    tripsShared: Number(body.tripsShared) || 0,
    joinedAt: body.joinedAt ?? new Date().toISOString().slice(0, 10),
    active: body.active === false ? 0 : 1,
  })
  res.json(userFromRow(db.prepare('SELECT * FROM users WHERE id = ?').get(req.params.id)))
})

router.delete('/:id', (req, res) => {
  db.prepare('DELETE FROM users WHERE id = ?').run(req.params.id)
  res.status(204).end()
})

export default router
