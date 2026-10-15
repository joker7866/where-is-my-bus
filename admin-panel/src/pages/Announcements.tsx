import { useState } from 'react'
import { useStore } from '../data/store.tsx'
import { useDistrict } from '../data/DistrictContext.tsx'
import { formatRelativeTime } from '../data/format'
import type { Announcement, AnnouncementSeverity } from '../data/types'

function emptyAnnouncement(district: string): Announcement {
  return { id: '', title: '', body: '', severity: 'INFO', district, createdAt: new Date().toISOString() }
}

export default function Announcements() {
  const { state, dispatch } = useStore()
  const { district } = useDistrict()
  const [draft, setDraft] = useState<Announcement | null>(null)

  const announcements = state.announcements
    .filter((a) => !district || a.district === district)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))

  const knownDistricts = state.districts

  function save(a: Announcement) {
    dispatch({ type: 'UPSERT_ANNOUNCEMENT', announcement: a })
    setDraft(null)
  }
  function remove(id: string) {
    if (confirm('Delete this announcement?')) dispatch({ type: 'DELETE_ANNOUNCEMENT', id })
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1 className="page-title">News & updates</h1>
          <p className="page-subtitle">
            Posts shown in the rider app's News feed{district ? ` for ${district}` : ''}
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setDraft(emptyAnnouncement(district || knownDistricts[0] || ''))}>
          + Post update
        </button>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {announcements.map((a) => (
          <div key={a.id} className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                  <span className={`badge ${a.severity === 'WARNING' ? 'recent' : 'live'}`}>
                    {a.severity === 'WARNING' ? 'Warning' : 'Info'}
                  </span>
                  <span className="badge timetable">{a.district}</span>
                  <span className="muted" style={{ fontSize: 12 }}>{formatRelativeTime(a.createdAt)}</span>
                </div>
                <div style={{ fontWeight: 700, fontSize: 16 }}>{a.title}</div>
                <div style={{ fontSize: 13, color: '#444', marginTop: 4, maxWidth: 640 }}>{a.body}</div>
              </div>
              <div style={{ whiteSpace: 'nowrap' }}>
                <button className="btn btn-outline btn-sm" onClick={() => setDraft(a)}>
                  Edit
                </button>{' '}
                <button className="btn btn-danger btn-sm" onClick={() => remove(a.id)}>
                  Delete
                </button>
              </div>
            </div>
          </div>
        ))}
        {announcements.length === 0 && (
          <div className="empty-state">No updates posted{district ? ` for ${district}` : ''} yet.</div>
        )}
      </div>

      {draft && (
        <AnnouncementModal
          initial={draft}
          existingIds={state.announcements.map((a) => a.id)}
          knownDistricts={knownDistricts}
          onCancel={() => setDraft(null)}
          onSave={save}
        />
      )}
    </>
  )
}

function AnnouncementModal({
  initial,
  existingIds,
  knownDistricts,
  onCancel,
  onSave,
}: {
  initial: Announcement
  existingIds: string[]
  knownDistricts: string[]
  onCancel: () => void
  onSave: (a: Announcement) => void
}) {
  const [draft, setDraft] = useState<Announcement>(initial)
  const isNew = !existingIds.includes(initial.id)
  const canSave = draft.title.trim() && draft.body.trim() && draft.district.trim()

  function handleSave() {
    const id = draft.id.trim() || `ANN-${Date.now()}`
    onSave({ ...draft, id })
  }

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>{isNew ? 'Post update' : 'Edit update'}</h2>
        <p className="modal-subtitle">Shown to riders in the app's News & updates feed, newest first.</p>

        <div className="form-grid single">
          <div className="field">
            <label>Title</label>
            <input
              value={draft.title}
              onChange={(e) => setDraft({ ...draft, title: e.target.value })}
              placeholder="e.g. Route 70A running late today"
            />
          </div>
          <div className="field">
            <label>Details</label>
            <textarea
              rows={4}
              value={draft.body}
              onChange={(e) => setDraft({ ...draft, body: e.target.value })}
              placeholder="What riders need to know"
            />
          </div>
          <div className="field">
            <label>District</label>
            <input
              list="announcement-districts"
              value={draft.district}
              onChange={(e) => setDraft({ ...draft, district: e.target.value })}
              placeholder="e.g. Coimbatore"
            />
            <datalist id="announcement-districts">
              {knownDistricts.map((d) => (
                <option key={d} value={d} />
              ))}
            </datalist>
          </div>
          <div className="field">
            <label>Severity</label>
            <select
              value={draft.severity}
              onChange={(e) => setDraft({ ...draft, severity: e.target.value as AnnouncementSeverity })}
            >
              <option value="INFO">Info</option>
              <option value="WARNING">Warning</option>
            </select>
          </div>
        </div>

        <div className="modal-actions">
          <button className="btn btn-outline" onClick={onCancel}>
            Cancel
          </button>
          <button className="btn btn-primary" disabled={!canSave} onClick={handleSave}>
            Save update
          </button>
        </div>
      </div>
    </div>
  )
}
