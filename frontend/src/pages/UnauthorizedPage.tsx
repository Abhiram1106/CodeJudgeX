import { Link } from 'react-router-dom'

export default function UnauthorizedPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-gray-900 mb-2">403</h1>
        <p className="text-gray-500 mb-6">You don&apos;t have permission to view this page.</p>
        <Link to="/contests" className="text-blue-600 hover:underline text-sm">
          Go to contests
        </Link>
      </div>
    </div>
  )
}
