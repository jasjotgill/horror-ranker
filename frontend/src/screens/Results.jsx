import { useEffect, useState } from 'react'
import { api } from '../api'
import ErrorMessage from '../components/ErrorMessage'
import Poster from '../components/Poster'

export default function Results({ session, onLeave }) {
  const [results, setResults] = useState(null)
  const [error, setError] = useState(null)
  // Which leaderboard is showing: with the golden ticket applied, or ratings alone.
  const [withTicket, setWithTicket] = useState(true)

  // Results are final once the group is DONE, so one fetch is enough.
  useEffect(() => {
    let ignore = false
    api
      .results(session)
      .then((data) => {
        if (!ignore) setResults(data)
      })
      .catch((err) => {
        if (!ignore) setError(err.message)
      })
    return () => {
      ignore = true
    }
  }, [session])

  if (error) return <ErrorMessage>{error}</ErrorMessage>
  if (!results) return <p className="muted center">Counting the votes…</p>

  const { winners, boost } = results.goldenTicket
  const ticketAwarded = winners.length > 0
  const films = ticketAwarded && !withTicket ? results.filmsWithoutTicket : results.films

  return (
    <div className="stack">
      {ticketAwarded ? (
        <>
          <p className="banner gold-banner">
            🎫 Golden ticket: {joinNames(winners)}.{' '}
            {winners.length === 1
              ? `Their film gets +${boost.toFixed(2)}.`
              : `They tied, so their films get +${boost.toFixed(2)} each.`}
          </p>
          <div className="tabs two" role="tablist">
            <button role="tab" aria-selected={withTicket} onClick={() => setWithTicket(true)}>
              With golden ticket
            </button>
            <button role="tab" aria-selected={!withTicket} onClick={() => setWithTicket(false)}>
              Ratings only
            </button>
          </div>
        </>
      ) : (
        <p className="banner">🎫 Golden ticket: everyone tied, so no film gets the extra point.</p>
      )}

      <section className="card stack">
        <h2>{ticketAwarded && !withTicket ? 'Ranking on ratings only' : 'Final ranking'}</h2>
        {films.map((film) => (
          <div className={film.rank === 1 ? 'film-row winner' : 'film-row'} key={film.pickId}>
            <span className="order">{film.rank}</span>
            <Poster url={film.posterUrl} title={film.title} />
            <div className="film-text">
              <p className="film-title">{film.title}</p>
              <p className="muted">{film.releaseYear}</p>
              <p className="muted small-text">
                {film.score === null
                  ? 'No ratings'
                  : `Enjoyment ${film.enjoyment.toFixed(1)} · ${film.raterCount} ${film.raterCount === 1 ? 'rating' : 'ratings'}`}
              </p>
              {film.ticketBoost > 0 && (
                <p className="gold small-text">🎫 Golden ticket +{film.ticketBoost.toFixed(2)}</p>
              )}
            </div>
            <span className="final-score">{film.score === null ? '–' : film.score.toFixed(2)}</span>
          </div>
        ))}
      </section>
      <p className="muted center small-text">
        Scores are out of 10: the average of everyone’s five category scores. Nobody rates their own film. Ties are
        broken by enjoyment.
      </p>
      <button onClick={onLeave}>Start a new marathon</button>
    </div>
  )
}

// "Sam", "Sam and Ria", "Sam, Ria and Kai"
function joinNames(names) {
  if (names.length < 2) return names.join('')
  return `${names.slice(0, -1).join(', ')} and ${names[names.length - 1]}`
}
