import { useEffect, useState } from 'react'
import { api } from '../api'
import ErrorMessage from '../components/ErrorMessage'
import Poster from '../components/Poster'

// How long typing must pause before a search is sent.
const SEARCH_DELAY_MS = 350
const MIN_QUERY_LENGTH = 2

// Films announced for next year are allowed, matching the server's rule.
const LATEST_YEAR = new Date().getFullYear() + 1

export default function Lobby({ session, group, refresh }) {
  const [error, setError] = useState(null)
  const { you, members, picks, pickedCount } = group
  const waitingFor = members.length - pickedCount
  const canStart = waitingFor === 0 && members.length >= 2

  // Runs one action against the API, shows its error if it fails, then reloads the group.
  async function run(action) {
    setError(null)
    try {
      await action()
    } catch (err) {
      setError(err.message)
    }
    await refresh()
  }

  // Toggles the caller's "I've seen it" on a film. The server removes the film once
  // two people have seen it.
  function toggleSeen(pick, seen) {
    run(() => (seen ? api.unseen(session, pick.id) : api.seen(session, pick.id)))
  }

  function start() {
    if (window.confirm('Start the marathon? Picks are locked and nobody else can join.')) {
      run(() => api.start(session))
    }
  }

  return (
    <div className="stack">
      <section className="card center">
        <p className="muted">Join code</p>
        <p className="join-code">{group.code}</p>
        <p className="muted">
          {members.length} {members.length === 1 ? 'person' : 'people'} here: {members.join(', ')}
        </p>
      </section>

      <ErrorMessage>{error}</ErrorMessage>

      <section className="card stack">
        <h2>Your pick</h2>
        {you.pickVetoed && (
          <p className="banner warning">
            Your pick was removed because two people have seen it. Choose another film.
          </p>
        )}
        {you.pick ? (
          <FilmRow pick={you.pick} note="Only you can see that this one is yours." />
        ) : (
          <p className="muted">Pick one horror film you think nobody here has seen.</p>
        )}
        <PickForm session={session} hasPick={Boolean(you.pick)} onPicked={refresh} />
      </section>

      <section className="card stack">
        <h2>
          Picked so far: {pickedCount} of {members.length}
        </h2>
        <p className="muted small-text">
          Seen one of these? Say so. If two people have seen a film, it is removed and whoever picked it chooses
          again.
        </p>
        {picks.length === 0 && <p className="muted">Nothing yet.</p>}
        {picks.map((pick) => {
          const seen = you.seenPickIds.includes(pick.id)
          return (
            <FilmRow key={pick.id} pick={pick} note={pick.seenCount > 0 ? pick.seenCount === 1 ? "1 person has seen it" : `${pick.seenCount} people have seen it` : null}>
              {pick.id !== you.pick?.id && (
                <button className="small" aria-pressed={seen} onClick={() => toggleSeen(pick, seen)}>
                  {seen ? 'Seen ✓' : 'I’ve seen it'}
                </button>
              )}
            </FilmRow>
          )
        })}
      </section>

      <button className="primary" disabled={!canStart} onClick={start}>
        {canStart
          ? 'Start the marathon'
          : members.length < 2
            ? 'Waiting for more people'
            : `Waiting for ${waitingFor} more ${waitingFor === 1 ? 'pick' : 'picks'}`}
      </button>
    </div>
  )
}

function FilmRow({ pick, note, children }) {
  return (
    <div className="film-row">
      <Poster url={pick.posterUrl} title={pick.title} />
      <div className="film-text">
        <p className="film-title">{pick.title}</p>
        <p className="muted">{pick.releaseYear}</p>
        {note && <p className="muted small-text">{note}</p>}
      </div>
      {children}
    </div>
  )
}

// Live search: results update as you type. Or add a film by hand when search cannot find it.
function PickForm({ session, hasPick, onPicked }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState(null) // null = nothing searched yet
  const [searching, setSearching] = useState(false)
  const [manual, setManual] = useState(false)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  // Runs after every change to the search box, but waits until typing pauses (a "debounce"),
  // so "hereditary" is one request to the server instead of ten.
  useEffect(() => {
    const text = query.trim()
    const tooShort = text.length < MIN_QUERY_LENGTH
    // Lets us cancel a request whose answer is no longer wanted.
    const controller = new AbortController()

    const timer = setTimeout(
      async () => {
        if (tooShort) {
          setResults(null)
          setSearching(false)
          return
        }
        setSearching(true)
        try {
          const found = await api.search(session, text, controller.signal)
          setResults(found)
          // Nothing found: open the manual form.
          setManual(found.length === 0)
          setError(null)
          setSearching(false)
        } catch (err) {
          if (err.name === 'AbortError') return
          setError(err.message)
          setSearching(false)
        }
      },
      tooShort ? 0 : SEARCH_DELAY_MS,
    )

    // Cleanup runs when the text changes again: drop the pending search and cancel one in
    // flight, so an older, slower answer can never replace a newer one.
    return () => {
      clearTimeout(timer)
      controller.abort()
    }
  }, [query, session])

  async function submitPick(pick) {
    setBusy(true)
    setError(null)
    try {
      await api.pick(session, pick)
      setQuery('')
      setManual(false)
      await onPicked()
    } catch (err) {
      setError(err.message)
    }
    setBusy(false)
  }

  return (
    <div className="stack">
      {/* A form only so the phone keyboard's "search" key has something harmless to submit. */}
      <form role="search" onSubmit={(event) => event.preventDefault()}>
        <input
          type="search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder={hasPick ? 'Type to change your pick' : 'Type a film title'}
          aria-label="Search for a film"
          maxLength={100}
          enterKeyHint="search"
          autoComplete="off"
        />
      </form>
      <ErrorMessage>{error}</ErrorMessage>

      {/* Always rendered, so the layout does not jump when searching starts and stops. */}
      <p className="muted small-text search-status" aria-live="polite">
        {searching ? 'Searching…' : results?.length === 0 ? 'No films found for that search.' : ''}
      </p>

      {/* Manual entry sits above the results, so it is reachable without scrolling past them. */}
      {manual ? (
        <ManualForm suggestedTitle={query.trim()} busy={busy} onSubmit={submitPick} />
      ) : (
        results && (
          <button className="link" onClick={() => setManual(true)}>
            Can’t find it? Add it manually
          </button>
        )
      )}

      {results?.length > 0 && (
        // The list scrolls inside its own box, so the page itself stays put.
        <div className={searching ? 'results stale' : 'results'} tabIndex={0} aria-label="Search results">
          {results.map((movie) => (
            <div className="film-row" key={movie.id}>
              <Poster url={movie.posterUrl} title={movie.title} />
              <div className="film-text">
                <p className="film-title">{movie.title}</p>
                <p className="muted">{movie.year ?? 'Year unknown'}</p>
              </div>
              <button className="small" disabled={busy} onClick={() => submitPick({ tmdbId: movie.id })}>
                Pick
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function ManualForm({ suggestedTitle, busy, onSubmit }) {
  // Follows the search text until the title is edited by hand; null means "not edited yet".
  const [editedTitle, setEditedTitle] = useState(null)
  const title = editedTitle ?? suggestedTitle
  const [year, setYear] = useState('')
  const [posterUrl, setPosterUrl] = useState('')

  function submit(event) {
    event.preventDefault()
    onSubmit({ title, year: Number(year), posterUrl: posterUrl.trim() || null })
  }

  return (
    <form className="stack manual" onSubmit={submit}>
      <h3>Can’t find it? Add it manually</h3>
      <label>
        Title
        <input value={title} onChange={(event) => setEditedTitle(event.target.value)} maxLength={200} required />
      </label>
      <label>
        Year
        <input
          type="number"
          inputMode="numeric"
          value={year}
          onChange={(event) => setYear(event.target.value)}
          min={1888}
          max={LATEST_YEAR}
          required
        />
      </label>
      <label>
        Poster link (optional, must start with https://)
        <input
          type="url"
          value={posterUrl}
          onChange={(event) => setPosterUrl(event.target.value)}
          maxLength={500}
          placeholder="https://"
        />
      </label>
      <button disabled={busy}>Pick this film</button>
    </form>
  )
}
