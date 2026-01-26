import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { Modal } from '@/components/ui/Modal'
import { useCreatePackage } from '@/hooks'

interface CreatePackageModalProps {
  isOpen: boolean
  onClose: () => void
}

interface FormData {
  name: string
  description: string
  baseUrl: string
  openApiSpec: string
}

const initialFormData: FormData = {
  name: '',
  description: '',
  baseUrl: '',
  openApiSpec: '',
}

export function CreatePackageModal({ isOpen, onClose }: CreatePackageModalProps) {
  const navigate = useNavigate()
  const createPackage = useCreatePackage()

  const [formData, setFormData] = useState<FormData>(initialFormData)
  const [errors, setErrors] = useState<Record<string, string>>({})

  const resetForm = useCallback(() => {
    setFormData(initialFormData)
    setErrors({})
  }, [])

  // Reset form when modal closes
  useEffect(() => {
    if (!isOpen) {
      resetForm()
      createPackage.reset()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- createPackage.reset is stable, but object reference changes
  }, [isOpen, resetForm])

  const updateField = (field: keyof FormData, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }))
    // Clear error when user starts typing
    if (errors[field]) {
      setErrors((prev) => {
        const { [field]: _, ...rest } = prev
        return rest
      })
    }
  }

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {}

    if (!formData.name.trim()) {
      newErrors.name = 'Name is required'
    } else if (formData.name.length < 3) {
      newErrors.name = 'Name must be at least 3 characters'
    }

    if (!formData.baseUrl.trim()) {
      newErrors.baseUrl = 'Base URL is required'
    } else {
      try {
        new URL(formData.baseUrl)
      } catch {
        newErrors.baseUrl = 'Invalid URL format'
      }
    }

    if (!formData.openApiSpec.trim()) {
      newErrors.openApiSpec = 'OpenAPI specification is required'
    } else {
      // Try to parse as JSON to validate format
      try {
        JSON.parse(formData.openApiSpec)
      } catch {
        // Could be YAML, which is also valid - don't enforce JSON-only
      }
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validate()) return

    try {
      const trimmedDescription = formData.description.trim()
      const pkg = await createPackage.mutateAsync({
        name: formData.name.trim(),
        openApiSpec: formData.openApiSpec.trim(),
        baseUrl: formData.baseUrl.trim(),
        ...(trimmedDescription && { description: trimmedDescription }),
      })

      onClose()
      void navigate({
        to: '/packages/$packageId',
        params: { packageId: pkg.id },
      })
    } catch (error) {
      console.error('Failed to create package:', error)
    }
  }

  const inputClassName = (fieldName: string) =>
    `w-full px-4 py-2 bg-secondary-800 border rounded-lg text-white placeholder-secondary-500 focus:outline-none transition-colors ${
      errors[fieldName]
        ? 'border-red-500 focus:border-red-500'
        : 'border-secondary-700 focus:border-primary-500'
    }`

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Create New Package" size="xl">
      <form
        onSubmit={(e) => {
          void handleSubmit(e)
        }}
        className="space-y-5"
      >
        {/* Name */}
        <div>
          <label htmlFor="modal-name" className="block text-sm font-medium text-white mb-2">
            Package Name <span className="text-red-500">*</span>
          </label>
          <input
            id="modal-name"
            type="text"
            value={formData.name}
            onChange={(e) => {
              updateField('name', e.target.value)
            }}
            placeholder="My API Package"
            className={inputClassName('name')}
            autoFocus
          />
          {errors.name && <p className="mt-1 text-sm text-red-500">{errors.name}</p>}
        </div>

        {/* Description */}
        <div>
          <label htmlFor="modal-description" className="block text-sm font-medium text-white mb-2">
            Description
          </label>
          <textarea
            id="modal-description"
            value={formData.description}
            onChange={(e) => {
              updateField('description', e.target.value)
            }}
            placeholder="Optional description for this package..."
            rows={2}
            className="w-full px-4 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white placeholder-secondary-500 focus:outline-none focus:border-primary-500 transition-colors resize-none"
          />
        </div>

        {/* Base URL */}
        <div>
          <label htmlFor="modal-baseUrl" className="block text-sm font-medium text-white mb-2">
            Base URL <span className="text-red-500">*</span>
          </label>
          <input
            id="modal-baseUrl"
            type="url"
            value={formData.baseUrl}
            onChange={(e) => {
              updateField('baseUrl', e.target.value)
            }}
            placeholder="https://api.example.com"
            className={inputClassName('baseUrl')}
          />
          {errors.baseUrl && <p className="mt-1 text-sm text-red-500">{errors.baseUrl}</p>}
          <p className="mt-1 text-xs text-secondary-500">The base URL where your API is hosted</p>
        </div>

        {/* OpenAPI Spec */}
        <div>
          <label htmlFor="modal-openApiSpec" className="block text-sm font-medium text-white mb-2">
            OpenAPI Specification <span className="text-red-500">*</span>
          </label>
          <textarea
            id="modal-openApiSpec"
            value={formData.openApiSpec}
            onChange={(e) => {
              updateField('openApiSpec', e.target.value)
            }}
            placeholder='{"openapi": "3.0.0", ...}'
            rows={8}
            className={`${inputClassName('openApiSpec')} font-mono text-sm resize-none`}
          />
          {errors.openApiSpec && <p className="mt-1 text-sm text-red-500">{errors.openApiSpec}</p>}
          <p className="mt-1 text-xs text-secondary-500">
            Paste your OpenAPI 3.0+ specification in JSON or YAML format
          </p>
        </div>

        {/* Error message */}
        {createPackage.isError && (
          <div className="p-4 bg-red-500/10 border border-red-500/50 rounded-lg">
            <p className="text-red-500 text-sm">
              {createPackage.error instanceof Error
                ? createPackage.error.message
                : 'Failed to create package. Please try again.'}
            </p>
          </div>
        )}

        {/* Actions */}
        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={onClose}
            disabled={createPackage.isPending}
            className="btn btn-ghost"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={createPackage.isPending}
            className="btn btn-primary disabled:opacity-50"
          >
            {createPackage.isPending ? (
              <span className="flex items-center gap-2">
                <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24">
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                    fill="none"
                  />
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                  />
                </svg>
                Creating...
              </span>
            ) : (
              'Create Package'
            )}
          </button>
        </div>
      </form>
    </Modal>
  )
}
