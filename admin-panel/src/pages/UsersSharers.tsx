import { useState } from 'react'
import { useStore } from '../data/store.tsx'
import type { AppUser, UserRole } from '../data/types'

function emptyUser(): AppUser {
  return { id: '', name: '', email: '', role: 'SHARER', tripsShared: 0, joinedAt: new Date().toISOString().slice(0, 10), active: true }
}

const ROLE_LABEL: Record<UserRole, string> = {
  SUPER_ADMIN: 'Super admin',
  ADMIN: 'Admin',
  SHARER: 'Sharer',
}

export default function UsersSharers() {
  const { state, dispatch } = useStore()
  const [query, setQuery] = useState('')
  const [draft, setDraft] = useState<AppUser | null>(null)

  const users = state.users.filter(
    (u) => u.name.toLowerCase().includes(query.toLowerCase()) || u.email.toLowerCase().includes(query.toLowerCase()),
  )

  function toggleActive(u: AppUser) {
    dispatch({ type: 'UPSERT_USER', user: { ...u, active: !u.active } })
  }
  function deleteUser(id: string) {
    if (confirm('Remove this user? This cannot be undone.')) dispatch({ type: 'DELETE_USER', id })
  }
  function save(u: AppUser) {
    dispatch({ type: 'UPSERT_USER', user: u })
    setDraft(null)
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1 className="page-title">Users & sharers</h1>
          <p className="page-subtitle">Admins who manage data, and riders who share live location</p>
        </div>
      </div>

      <div className="toolbar">
        <input
          className="search-input"
          placeholder="Search by name or email…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button className="btn btn-primary" onClick={() => setDraft(emptyUser())}>
          + Add user
        </button>
      </div>

      <div className="card">
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Trips shared</th>
                <th>Joined</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td style={{ fontWeight: 600 }}>{u.name}</td>
                  <td className="muted">{u.email}</td>
                  <td>
                    <span className="badge timetable">{ROLE_LABEL[u.role]}</span>
                  </td>
                  <td>{u.tripsShared}</td>
                  <td className="muted">{u.joinedAt}</td>
                  <td>
                    <span className={`badge ${u.active ? 'resolved' : 'open'}`}>{u.active ? 'Active' : 'Disabled'}</span>
                  </td>
                  <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                    <button className="btn btn-outline btn-sm" onClick={() => setDraft(u)}>
                      Edit
                    </button>{' '}
                    <button className="btn btn-outline btn-sm" onClick={() => toggleActive(u)}>
                      {u.active ? 'Disable' : 'Enable'}
                    </button>{' '}
                    <button className="btn btn-danger btn-sm" onClick={() => deleteUser(u.id)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
              {users.length === 0 && (
                <tr>
                  <td colSpan={7} className="empty-state">
                    No users match.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {draft && <UserModal initial={draft} existingIds={state.users.map((u) => u.id)} onCancel={() => setDraft(null)} onSave={save} />}
    </>
  )
}

function UserModal({
  initial,
  existingIds,
  onCancel,
  onSave,
}: {
  initial: AppUser
  existingIds: string[]
  onCancel: () => void
  onSave: (u: AppUser) => void
}) {
  const [draft, setDraft] = useState<AppUser>(initial)
  const isNew = !existingIds.includes(initial.id)

  function handleSave() {
    const id = draft.id.trim() || `U${Date.now()}`
    onSave({ ...draft, id })
  }

  const canSave = draft.name.trim() && draft.email.trim()

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>{isNew ? 'Add user' : `Edit ${initial.name}`}</h2>
        <p className="modal-subtitle">Admins manage data; sharers broadcast live location from the rider app.</p>

        <div className="form-grid">
          <div className="field span-2">
            <label>Name</label>
            <input value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} />
          </div>
          <div className="field span-2">
            <label>Email</label>
            <input
              type="email"
              value={draft.email}
              onChange={(e) => setDraft({ ...draft, email: e.target.value })}
            />
          </div>
          <div className="field">
            <label>Role</label>
            <select value={draft.role} onChange={(e) => setDraft({ ...draft, role: e.target.value as UserRole })}>
              <option value="SHARER">Sharer</option>
              <option value="ADMIN">Admin</option>
              <option value="SUPER_ADMIN">Super admin</option>
            </select>
          </div>
          <div className="field">
            <label>Trips shared</label>
            <input
              type="number"
              min={0}
              value={draft.tripsShared}
              onChange={(e) => setDraft({ ...draft, tripsShared: Number(e.target.value) })}
            />
          </div>
        </div>

        <div className="modal-actions">
          <button className="btn btn-outline" onClick={onCancel}>
            Cancel
          </button>
          <button className="btn btn-primary" disabled={!canSave} onClick={handleSave}>
            Save user
          </button>
        </div>
      </div>
    </div>
  )
}
