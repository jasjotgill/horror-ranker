import { useCallback, useEffect, useState } from 'react'
import { api } from './api'

const POLL_MS = 3000

// Fetches the group state now and then every few seconds, so everyone sees
// joins, picks and the start without refreshing. Polling, not WebSockets.
export function useGroup(session, onLost) {
  const [group, setGroup] = useState(null)
  const [offline, setOffline] = useState(false)

  const refresh = useCallback(
    () =>
      api.state(session).then(
        (state) => {
          setGroup(state)
          setOffline(false)
        },
        (error) => {
          // The token or group no longer exists (for example the test data was deleted):
          // drop the saved session and go back to the home screen.
          if ([401, 403, 404].includes(error.status)) onLost()
          else setOffline(true)
        },
      ),
    [session, onLost],
  )

  useEffect(() => {
    refresh()
    const timer = setInterval(refresh, POLL_MS)
    // Cleanup: stop the timer when the screen goes away or the session changes.
    return () => clearInterval(timer)
  }, [refresh])

  return { group, offline, refresh }
}
