import { useState } from 'react'
import { api } from '../api'
import ErrorMessage from '../components/ErrorMessage'
import Poster from '../components/Poster'

const CATEGORIES = [
  { key: 'scariness', label: 'Scariness', hint: 'Overall fear factor, including how well the jump scares land.' },
  { key: 'atmosphere', label: 'Atmosphere', hint: 'Dread, tension and disturbing imagery.' },
  { key: 'story', label: 'Story', hint: 'Plot quality and coherence, and how memorable the characters are.' },
  { key: 'acting', label: 'Acting', hint: 'Performances and believability.' },
  { key: 'enjoyment', label: 'Enjoyment', hint: 'How much you enjoyed it, and whether you would watch it again.' },
]

const SCORES = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

export default function Rate({ session, pick, existing, onSaved, onBack }) {
  // Starts from the saved rating when editing, otherwise with nothing chosen:
  // there is no default score to accept by accident.
  const [scores, setScores] = useState(() => existing ?? {})
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const complete = CATEGORIES.every((category) => scores[category.key])

  async function save() {
    setBusy(true)
    setError(null)
    try {
      const { scariness, atmosphere, story, acting, enjoyment } = scores
      await api.rate(session, pick.id, { scariness, atmosphere, story, acting, enjoyment })
      await onSaved()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <div className="stack">
      <section className="card film-row">
        <Poster url={pick.posterUrl} title={pick.title} />
        <div className="film-text">
          <p className="film-title">{pick.title}</p>
          <p className="muted">{pick.releaseYear}</p>
        </div>
      </section>

      {CATEGORIES.map((category) => (
        <fieldset className="card" key={category.key}>
          <legend>{category.label}</legend>
          <p className="muted small-text">{category.hint}</p>
          <div className="score-row">
            {SCORES.map((score) => (
              <button
                type="button"
                key={score}
                className="score"
                aria-pressed={scores[category.key] === score}
                aria-label={`${category.label} ${score}`}
                onClick={() => setScores({ ...scores, [category.key]: score })}
              >
                {score}
              </button>
            ))}
          </div>
        </fieldset>
      ))}

      <ErrorMessage>{error}</ErrorMessage>
      <button className="primary" disabled={!complete || busy} onClick={save}>
        {complete ? 'Save rating' : 'Score all five to save'}
      </button>
      <button className="link center" onClick={onBack}>
        Back to watch order
      </button>
    </div>
  )
}
