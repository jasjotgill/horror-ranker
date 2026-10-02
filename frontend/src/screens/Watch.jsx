import { useState } from 'react'
import { api } from '../api'
import ErrorMessage from '../components/ErrorMessage'
import People from '../components/People'
import Poster from '../components/Poster'

// The shuffled watch order. The order comes from the server, which stored it once at start.
export default function Watch({ session, group, refresh, onRate }) {
  const [error, setError] = useState(null)
  const ownPickId = group.you.pick?.id
  const ratedIds = new Set(group.you.ratings.map((rating) => rating.pickId))
  // Everything except your own pick, which you do not rate.
  const toRate = group.picks.filter((pick) => pick.id !== ownPickId)
  const ratedCount = toRate.filter((pick) => ratedIds.has(pick.id)).length
  const allRated = ratedCount === toRate.length

  async function finish() {
    if (!window.confirm('End rating for everyone and move on to golden tickets? Missing ratings will not be counted.')) {
      return
    }
    setError(null)
    try {
      await api.finish(session)
    } catch (err) {
      setError(err.message)
    }
    await refresh()
  }

  return (
    <div className="stack">
      <section className="card stack">
        <h2>Watch order</h2>
        <p className="muted">
          You have rated {ratedCount} of {toRate.length}. You do not rate your own pick.
        </p>
        {group.picks.map((pick, index) => (
          <div className="film-row" key={pick.id}>
            {/* Position in the list, so the numbers stay 1, 2, 3 even if a film was removed. */}
            <span className="order">{index + 1}</span>
            <Poster url={pick.posterUrl} title={pick.title} />
            <div className="film-text">
              <p className="film-title">{pick.title}</p>
              <p className="muted">{pick.releaseYear}</p>
            </div>
            {pick.id === ownPickId ? (
              <span className="tag">Your pick</span>
            ) : (
              <button className={ratedIds.has(pick.id) ? 'small' : 'small primary'} onClick={() => onRate(pick.id)}>
                {ratedIds.has(pick.id) ? 'Edit' : 'Rate'}
              </button>
            )}
          </div>
        ))}
      </section>

      {allRated && (
        <p className="banner">You are done. Golden tickets come next, as soon as everyone has rated every film.</p>
      )}
      <People session={session} group={group} refresh={refresh} />
      <ErrorMessage>{error}</ErrorMessage>
      <button className="link center" onClick={finish}>
        Someone can’t finish? End rating and move on
      </button>
    </div>
  )
}
