import { randomUUID } from 'node:crypto'
import { Router } from 'express'
import { db } from '../db.js'

const router = Router()

const VALID_TYPES = ['STOP_FIX', 'NO_SHOW', 'BREAKDOWN', 'FEEDBACK']

router.get('/', (req, res) => {
  res.json(db.prepare('SELECT * FROM reports ORDER BY createdAt DESC').all())
})

router.post('/', (req, res) => {
  const { type, message, routeId, stopId } = req.body
  if (!VALID_TYPES.includes(type)) {
    return res.status(400).json({ error: `type must be one of ${VALID_TYPES.join(', ')}` })
  }
  if (!message || !message.trim()) {
    return res.status(400).json({ error: 'message is required' })
  }
  const report = {
    id: `REP-${randomUUID().slice(0, 8)}`,
    type,
    message: message.trim(),
    routeId: routeId || null,
    stopId: stopId || null,
    createdAt: new Date().toISOString(),
    status: 'OPEN',
  }
  db.prepare(`
    INSERT INTO reports (id, type, message, routeId, stopId, createdAt, status)
    VALUES (@id, @type, @message, @routeId, @stopId, @createdAt, @status)
  `).run(report)
  res.status(201).json(report)
})

router.put('/:id/status', (req, res) => {
  const { status } = req.body
  if (status !== 'OPEN' && status !== 'RESOLVED') {
    return res.status(400).json({ error: 'status must be OPEN or RESOLVED' })
  }
  db.prepare('UPDATE reports SET status = ? WHERE id = ?').run(status, req.params.id)
  const row = db.prepare('SELECT * FROM reports WHERE id = ?').get(req.params.id)
  if (!row) return res.status(404).json({ error: 'not found' })
  res.json(row)
})

export default router
