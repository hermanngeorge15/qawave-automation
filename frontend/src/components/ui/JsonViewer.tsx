interface JsonViewerProps {
  data: unknown
  className?: string
}

export function JsonViewer({ data, className = '' }: JsonViewerProps) {
  const formatted = JSON.stringify(data, null, 2)

  return (
    <pre className={`bg-secondary-900 p-4 rounded-lg overflow-x-auto text-sm font-mono ${className}`}>
      <code className="text-secondary-300">{formatted}</code>
    </pre>
  )
}
