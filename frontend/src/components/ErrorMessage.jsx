export default function ErrorMessage({ children }) {
  if (!children) return null
  return (
    <p className="banner error" role="alert">
      {children}
    </p>
  )
}
