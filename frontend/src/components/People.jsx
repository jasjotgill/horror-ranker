import { useState } from 'react'
import { api } from '../api'
import ErrorMessage from './ErrorMessage'

// Who is in the group. The host also gets a Remove button next to everyone else.
export default function People({ session, group, refresh }) {
  const [error, setError] = useState(null)
  const started = group.status !== 'LOBBY'

  async function remove(nickname) {
    const consequence = started
      ? 'Their film stays in the marathon, but their ratings are removed.'
      : 'Their pick is removed too.'
    if (!window.confirm(`Remove ${nickname} from the group? ${consequence}`)) return
    setError(null)
    try {
      await api.kick(session, nickname)
    } catch (err) {
      setError(err.message)
    }
    await refresh()
  }

  return (
    <section className="card stack">
      <h2>
        {group.members.length} {group.members.length === 1 ? 'person' : 'people'}
      </h2>
      <ErrorMessage>{error}</ErrorMessage>
      {group.members.map((nickname) => (
        <div className="person" key={nickname}>
          <span className="person-name">{nickname}</span>
          {nickname === group.host && <span className="tag">Host</span>}
          {nickname === group.you.nickname && <span className="muted small-text">you</span>}
          {group.you.host && nickname !== group.you.nickname && (
            <button className="small" onClick={() => remove(nickname)}>
              Remove
            </button>
          )}
        </div>
      ))}
    </section>
  )
}
