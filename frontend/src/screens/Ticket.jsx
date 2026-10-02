import { useState } from 'react'
import { api } from '../api'
import ErrorMessage from '../components/ErrorMessage'
import People from '../components/People'

// After rating: everyone gives their one golden ticket to somebody else.
export default function Ticket({ session, group, refresh }) {
  const [error, setError] = useState(null)
  const { you, members, ticketsGiven } = group
  const others = members.filter((nickname) => nickname !== you.nickname)

  async function run(action) {
    setError(null)
    try {
      await action()
    } catch (err) {
      setError(err.message)
    }
    await refresh()
  }

  function finish() {
    if (window.confirm('Show the results now? Golden tickets not yet given will not be counted.')) {
      run(() => api.finish(session))
    }
  }

  return (
    <div className="stack">
      <section className="card stack">
        <h2>
          <span className="gold">🎫 Golden ticket</span>
        </h2>
        <p className="muted">
          Rating is finished. Give your one golden ticket to someone else. Whoever gets the most tickets has 1.0
          added to their film’s score, up to a maximum of 10. A tie splits the point between them. If everyone
          ties, nobody gets it.
        </p>
        <ErrorMessage>{error}</ErrorMessage>
        {others.map((nickname) => (
          <button
            key={nickname}
            className={you.ticketFor === nickname ? 'ticket-choice chosen' : 'ticket-choice'}
            aria-pressed={you.ticketFor === nickname}
            onClick={() => run(() => api.giveTicket(session, nickname))}
          >
            {nickname}
            {you.ticketFor === nickname && <span aria-hidden="true"> 🎫</span>}
          </button>
        ))}
        <p className="muted small-text">
          {ticketsGiven} of {members.length} have given theirs.{' '}
          {you.ticketFor
            ? `Yours goes to ${you.ticketFor}. You can change it until everyone has chosen.`
            : 'Nobody sees who you chose.'}
        </p>
      </section>

      <People session={session} group={group} refresh={refresh} />
      <button className="link center" onClick={finish}>
        Someone can’t choose? Show results now
      </button>
    </div>
  )
}
