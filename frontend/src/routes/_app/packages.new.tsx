import { useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useCreatePackage } from '@/hooks'

export const Route = createFileRoute('/_app/packages/new')({
  component: NewPackagePage,
})

function NewPackagePage() {
  const navigate = useNavigate()
  const createPackage = useCreatePackage()

  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [baseUrl, setBaseUrl] = useState('')
  const [openApiSpec, setOpenApiSpec] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {}

    if (!name.trim()) {
      newErrors.name = 'Name is required'
    } else if (name.length < 3) {
      newErrors.name = 'Name must be at least 3 characters'
    }

    if (!baseUrl.trim()) {
      newErrors.baseUrl = 'Base URL is required'
    } else {
      try {
        new URL(baseUrl)
      } catch {
        newErrors.baseUrl = 'Invalid URL format'
      }
    }

    if (!openApiSpec.trim()) {
      newErrors.openApiSpec = 'OpenAPI specification is required'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validate()) return

    try {
      const trimmedDescription = description.trim()
      const pkg = await createPackage.mutateAsync({
        name: name.trim(),
        openApiSpec: openApiSpec.trim(),
        baseUrl: baseUrl.trim(),
        ...(trimmedDescription && { description: trimmedDescription }),
      })

      void navigate({
        to: '/packages/$packageId',
        params: { packageId: pkg.id },
      })
    } catch (error) {
      console.error('Failed to create package:', error)
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <header className="mb-8">
        <h1 className="page-title">Create New Package</h1>
        <p className="text-secondary-400">
          Upload an OpenAPI specification to generate QA test scenarios
        </p>
      </header>

      <form onSubmit={(e) => {
        void handleSubmit(e)
      }} className="space-y-6">
        {/* Name */}
        <div>
          <label htmlFor="name" className="block text-sm font-medium text-white mb-2">
            Package Name *
          </label>
          <input
            id="name"
            type="text"
            value={name}
            onChange={(e) => {
              setName(e.target.value)
            }}
            placeholder="My API Package"
            className={`w-full px-4 py-2 bg-secondary-800 border rounded-lg text-white placeholder-secondary-500 focus:outline-none transition-colors ${
              errors.name
                ? 'border-red-500 focus:border-red-500'
                : 'border-secondary-700 focus:border-primary-500'
            }`}
          />
          {errors.name && <p className="mt-1 text-sm text-red-500">{errors.name}</p>}
        </div>

        {/* Description */}
        <div>
          <label htmlFor="description" className="block text-sm font-medium text-white mb-2">
            Description
          </label>
          <textarea
            id="description"
            value={description}
            onChange={(e) => {
              setDescription(e.target.value)
            }}
            placeholder="Optional description for this package..."
            rows={3}
            className="w-full px-4 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white placeholder-secondary-500 focus:outline-none focus:border-primary-500 transition-colors resize-none"
          />
        </div>

        {/* Base URL */}
        <div>
          <label htmlFor="baseUrl" className="block text-sm font-medium text-white mb-2">
            Base URL *
          </label>
          <input
            id="baseUrl"
            type="url"
            value={baseUrl}
            onChange={(e) => {
              setBaseUrl(e.target.value)
            }}
            placeholder="https://api.example.com"
            className={`w-full px-4 py-2 bg-secondary-800 border rounded-lg text-white placeholder-secondary-500 focus:outline-none transition-colors ${
              errors.baseUrl
                ? 'border-red-500 focus:border-red-500'
                : 'border-secondary-700 focus:border-primary-500'
            }`}
          />
          {errors.baseUrl && <p className="mt-1 text-sm text-red-500">{errors.baseUrl}</p>}
          <p className="mt-2 text-sm text-secondary-500">
            The base URL where your API is hosted
          </p>
        </div>

        {/* OpenAPI Spec */}
        <div>
          <label htmlFor="openApiSpec" className="block text-sm font-medium text-white mb-2">
            OpenAPI Specification *
          </label>
          <textarea
            id="openApiSpec"
            value={openApiSpec}
            onChange={(e) => {
              setOpenApiSpec(e.target.value)
            }}
            placeholder='{"openapi": "3.0.0", ...}'
            rows={12}
            className={`w-full px-4 py-2 bg-secondary-800 border rounded-lg text-white placeholder-secondary-500 focus:outline-none transition-colors font-mono text-sm ${
              errors.openApiSpec
                ? 'border-red-500 focus:border-red-500'
                : 'border-secondary-700 focus:border-primary-500'
            }`}
          />
          {errors.openApiSpec && (
            <p className="mt-1 text-sm text-red-500">{errors.openApiSpec}</p>
          )}
          <p className="mt-2 text-sm text-secondary-500">
            Paste your OpenAPI 3.0+ specification in JSON or YAML format
          </p>
        </div>

        {/* Error message */}
        {createPackage.isError && (
          <div className="p-4 bg-red-500/10 border border-red-500/50 rounded-lg">
            <p className="text-red-500">
              {createPackage.error instanceof Error
                ? createPackage.error.message
                : 'Failed to create package'}
            </p>
          </div>
        )}

        {/* Actions */}
        <div className="flex gap-4 pt-4">
          <button
            type="submit"
            disabled={createPackage.isPending}
            className="btn btn-primary disabled:opacity-50"
          >
            {createPackage.isPending ? 'Creating...' : 'Create Package'}
          </button>
          <button
            type="button"
            onClick={() => {
              void navigate({ to: '/packages' })
            }}
            className="btn btn-ghost"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
