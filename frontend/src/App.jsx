import { useCallback, useState } from 'react'
import { clearSession, loadSession, saveSession } from './api'
import { useGroup } from './useGroup'
import Home from './screens/Home'
import Lobby from './screens/Lobby'
import Rate from './screens/Rate'
import Results from './screens/Results'
import Watch from './screens/Watch'

export default function App() {
  const [session, setSession] = useState(loadSession)

  const enter = useCallback((newSession) => {
    saveSession(newSession)
    setSession(newSession)
  }, [])

  const leave = useCallback(() => {
    clearSession()
    setSession(null)
  }, [])

  return (
    <div className="app">
      <header className="app-header">
        <h1>Horror Marathon Ranker</h1>
      </header>
      {/* key: a different session gets a fresh Marathon with fresh state. */}
      {session ? <Marathon key={session.token} session={session} onLeave={leave} /> : <Home onEnter={enter} />}
      <footer className="app-footer">
        This product uses the TMDB API but is not endorsed or certified by TMDB.
      </footer>
    </div>
  )
}

// There is no router: which screen shows is decided by the group's status on the server,
// so everyone moves from lobby to watching to results together.
function Marathon({ session, onLeave }) {
  const { group, offline, refresh } = useGroup(session, onLeave)
  const [ratingPickId, setRatingPickId] = useState(null)

  if (!group) {
    return <p className="muted center">{offline ? 'Cannot reach the server. Retrying…' : 'Loading…'}</p>
  }

  const ratingPick = group.picks.find((pick) => pick.id === ratingPickId)
  let screen
  if (group.status === 'LOBBY') {
    screen = <Lobby session={session} group={group} refresh={refresh} />
  } else if (group.status === 'DONE') {
    screen = <Results session={session} onLeave={onLeave} />
  } else if (ratingPick) {
    screen = (
      <Rate
        session={session}
        pick={ratingPick}
        existing={group.you.ratings.find((rating) => rating.pickId === ratingPick.id)}
        // Reload before closing, so the watch list already shows this film as rated.
        onSaved={async () => {
          await refresh()
          setRatingPickId(null)
        }}
        onBack={() => setRatingPickId(null)}
      />
    )
  } else {
    screen = <Watch session={session} group={group} refresh={refresh} onRate={setRatingPickId} />
  }

  return (
    <main>
      <div className="group-bar">
        <span className="group-name">{group.name}</span>
        <span className="muted">
          {group.you.nickname} · <button className="link" onClick={() => confirmLeave(onLeave)}>Leave</button>
        </span>
      </div>
      {offline && <p className="banner warning">Connection problem. Retrying…</p>}
      {screen}
    </main>
  )
}

function confirmLeave(onLeave) {
  if (window.confirm('Leave on this device? You will not be able to get back in as the same person.')) onLeave()
}
