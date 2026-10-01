import { useState } from 'react'
import { api } from '../api'
import ErrorMessage from '../components/ErrorMessage'
import Poster from '../components/Poster'

// The shuffled watch order. The order comes from the server, which stored it once at start.
export default function Watch({ session, group, refresh, onRate }) {
  const [error, setError] = useState(null)
  const ratedIds = new Set(group.you.ratings.map((rating) => rating.pickId))
  const allRated = ratedIds.size === group.picks.length

  async function finish() {
    if (!window.confirm('End rating for everyone and show the results now? Missing ratings will not be counted.')) {
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
          You have rated {ratedIds.size} of {group.picks.length}. Rate every film, including your own pick, so nobody
          can tell which one is yours. Your rating of your own pick is not counted.
        </p>
        {group.picks.map((pick) => (
          <div className="film-row" key={pick.id}>
            <span className="order">{pick.watchOrder}</span>
            <Poster url={pick.posterUrl} title={pick.title} />
            <div className="film-text">
              <p className="film-title">{pick.title}</p>
              <p className="muted">{pick.releaseYear}</p>
            </div>
            <button className={ratedIds.has(pick.id) ? 'small' : 'small primary'} onClick={() => onRate(pick.id)}>
              {ratedIds.has(pick.id) ? 'Edit' : 'Rate'}
            </button>
          </div>
        ))}
      </section>

      {allRated && (
        <p className="banner">You are done. Results appear as soon as everyone has rated every film.</p>
      )}
      <ErrorMessage>{error}</ErrorMessage>
      <button className="link center" onClick={finish}>
        Someone can’t finish? End rating and show results
      </button>
    </div>
  )
}
