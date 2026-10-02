import { useCallback, useState } from 'react'
import { api, clearSession, loadSession, saveSession } from './api'
import RejoinCode from './components/RejoinCode'
import { useGroup } from './useGroup'
import Home from './screens/Home'
import Lobby from './screens/Lobby'
import Rate from './screens/Rate'
import Results from './screens/Results'
import Ticket from './screens/Ticket'
import Watch from './screens/Watch'

export default function App() {
  const [session, setSession] = useState(loadSession)
  // Shown on the home screen after being logged out by the server.
  const [notice, setNotice] = useState(null)

  const enter = useCallback((newSession) => {
    saveSession(newSession)
    setNotice(null)
    setSession(newSession)
  }, [])

  const leave = useCallback(() => {
    clearSession()
    setSession(null)
  }, [])

  // The server no longer knows this session: the member was removed, left on another
  // device, or rejoined somewhere else.
  const lost = useCallback(() => {
    clearSession()
    setNotice('You are no longer signed in to that group. If you still belong in it, use Rejoin with your rejoin code.')
    setSession(null)
  }, [])

  return (
    <div className="app">
      <header className="app-header">
        <h1>Horror Marathon Ranker</h1>
      </header>
      {/* key: a different session gets a fresh Marathon with fresh state. */}
      {session ? (
        <Marathon key={session.token} session={session} onLeave={leave} onLost={lost} />
      ) : (
        <Home onEnter={enter} notice={notice} />
      )}
      <footer className="app-footer">
        This product uses the TMDB API but is not endorsed or certified by TMDB.
      </footer>
    </div>
  )
}

// There is no router: which screen shows is decided by the group's status on the server,
// so everyone moves from lobby to watching to results together.
function Marathon({ session, onLeave, onLost }) {
  const { group, offline, refresh } = useGroup(session, onLost)
  const [ratingPickId, setRatingPickId] = useState(null)

  if (!group) {
    return <p className="muted center">{offline ? 'Cannot reach the server. Retrying…' : 'Loading…'}</p>
  }

  async function leave() {
    const finished = group.status === 'DONE'
    const warning = finished
      ? 'Leave this group on this device?'
      : group.status === 'LOBBY'
        ? 'Leave the group? You and your pick will be removed.'
        : 'Leave the group? You, your ratings and your golden ticket will be removed. Your film stays in the marathon.'
    if (!window.confirm(warning)) return
    // After the results are in, leaving only signs this browser out.
    if (!finished) await api.leave(session).catch(() => {})
    onLeave()
  }

  const ratingPick = group.picks.find((pick) => pick.id === ratingPickId)
  let screen
  if (group.status === 'LOBBY') {
    screen = <Lobby session={session} group={group} refresh={refresh} />
  } else if (group.status === 'DONE') {
    screen = <Results session={session} onLeave={onLeave} />
  } else if (group.status === 'TICKETS') {
    screen = <Ticket session={session} group={group} refresh={refresh} />
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
          {group.you.nickname} · <button className="link" onClick={leave}>Leave</button>
        </span>
      </div>
      {offline && <p className="banner warning">Connection problem. Retrying…</p>}
      {screen}
      {group.status !== 'DONE' && !ratingPick && <RejoinCode group={group} />}
    </main>
  )
}
