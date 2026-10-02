import { useState } from 'react'

// Copies text to the clipboard. Shows a tick for a moment so the tap is acknowledged.
export default function CopyButton({ text, label }) {
  const [copied, setCopied] = useState(false)

  async function copy() {
    try {
      await navigator.clipboard.writeText(text)
    } catch {
      // The clipboard API needs https; this older method covers plain http on a local network.
      const field = document.createElement('textarea')
      field.value = text
      document.body.appendChild(field)
      field.select()
      document.execCommand('copy')
      field.remove()
    }
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  return (
    <button className="icon" onClick={copy} aria-label={copied ? 'Copied' : label} title={label}>
      {copied ? (
        <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
          <path d="M5 13l4 4L19 7" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      ) : (
        // The usual "copy" symbol: one sheet in front of another.
        <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
          <rect x="9" y="9" width="11" height="11" rx="2" fill="none" stroke="currentColor" strokeWidth="2" />
          <path d="M5 15V6a2 2 0 0 1 2-2h9" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        </svg>
      )}
    </button>
  )
}
