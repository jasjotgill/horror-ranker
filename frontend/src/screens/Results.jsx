import { useEffect, useState } from 'react'
import { api } from '../api'
import ErrorMessage from '../components/ErrorMessage'
import Poster from '../components/Poster'

export default function Results({ session, onLeave }) {
  const [results, setResults] = useState(null)
  const [error, setError] = useState(null)

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

  return (
    <div className="stack">
      <section className="card stack">
        <h2>Final ranking</h2>
        {results.films.map((film) => (
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
            </div>
            <span className="final-score">{film.score === null ? '–' : film.score.toFixed(2)}</span>
          </div>
        ))}
      </section>
      <p className="muted center small-text">
        Scores are out of 10: the average of everyone’s five category scores, not counting each picker’s rating of
        their own film. Ties are broken by enjoyment.
      </p>
      <button onClick={onLeave}>Start a new marathon</button>
    </div>
  )
}
