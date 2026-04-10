import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ApiError,
  deleteAdminDocument,
  fetchWithTimeout,
  listAdminDocuments,
  scanAdminDocuments,
  uploadAdminDocument,
} from './api'

describe('fetchWithTimeout CSRF behavior', () => {
  const originalFetch = global.fetch
  const originalDocument = global.document

  beforeEach(() => {
    Object.defineProperty(global, 'document', {
      value: { cookie: '' },
      configurable: true,
      writable: true,
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()

    if (originalDocument === undefined) {
      // @ts-expect-error cleanup for test environment
      delete global.document
    } else {
      Object.defineProperty(global, 'document', {
        value: originalDocument,
        configurable: true,
        writable: true,
      })
    }

    global.fetch = originalFetch
  })

  it('adds X-XSRF-TOKEN on mutating requests when cookie exists', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 200 }))
    global.fetch = fetchMock as typeof fetch
    document.cookie = 'XSRF-TOKEN=test-token'

    await fetchWithTimeout('/api/test', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: '{}',
    })

    const [, requestInit] = fetchMock.mock.calls[0]
    const headers = new Headers(requestInit?.headers)
    expect(headers.get('X-XSRF-TOKEN')).toBe('test-token')
  })

  it('does not add X-XSRF-TOKEN on GET requests', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 200 }))
    global.fetch = fetchMock as typeof fetch
    document.cookie = 'XSRF-TOKEN=test-token'

    await fetchWithTimeout('/api/test', { method: 'GET' })

    const [, requestInit] = fetchMock.mock.calls[0]
    const headers = new Headers(requestInit?.headers)
    expect(headers.has('X-XSRF-TOKEN')).toBe(false)
  })

  it('keeps an explicit X-XSRF-TOKEN header when already provided', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 200 }))
    global.fetch = fetchMock as typeof fetch
    document.cookie = 'XSRF-TOKEN=test-token'

    await fetchWithTimeout('/api/test', {
      method: 'DELETE',
      headers: { 'X-XSRF-TOKEN': 'explicit-token' },
    })

    const [, requestInit] = fetchMock.mock.calls[0]
    const headers = new Headers(requestInit?.headers)
    expect(headers.get('X-XSRF-TOKEN')).toBe('explicit-token')
  })

  it('does not add X-XSRF-TOKEN when the cookie is missing', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 200 }))
    global.fetch = fetchMock as typeof fetch
    document.cookie = ''

    await fetchWithTimeout('/api/test', { method: 'PATCH' })

    const [, requestInit] = fetchMock.mock.calls[0]
    const headers = new Headers(requestInit?.headers)
    expect(headers.has('X-XSRF-TOKEN')).toBe(false)
  })
})

describe('admin document API helpers', () => {
  const originalFetch = global.fetch
  const originalDocument = global.document

  beforeEach(() => {
    Object.defineProperty(global, 'document', {
      value: { cookie: '' },
      configurable: true,
      writable: true,
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()

    if (originalDocument === undefined) {
      // @ts-expect-error cleanup for test environment
      delete global.document
    } else {
      Object.defineProperty(global, 'document', {
        value: originalDocument,
        configurable: true,
        writable: true,
      })
    }

    global.fetch = originalFetch
  })

  it('sends X-API-Key when listing admin documents', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      sources: ['cv.md'],
      count: 1,
    }), { status: 200 }))
    global.fetch = fetchMock as typeof fetch

    const result = await listAdminDocuments('admin-key')

    const [, requestInit] = fetchMock.mock.calls[0]
    const headers = new Headers(requestInit?.headers)
    expect(headers.get('X-API-Key')).toBe('admin-key')
    expect(result.sources).toEqual(['cv.md'])
  })

  it('throws an ApiError when admin document listing fails', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      message: 'Unauthorized',
      code: 'admin.unauthorized',
    }), { status: 401 }))
    global.fetch = fetchMock as typeof fetch

    await expect(listAdminDocuments('wrong-key')).rejects.toEqual(
      expect.objectContaining<ApiError>({
        message: 'Unauthorized',
        status: 401,
        code: 'admin.unauthorized',
      }),
    )
  })

  it('uploads admin documents with FormData and api key header', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      success: true,
      filename: 'README.md',
      message: 'Document ingested successfully',
      chunksCreated: 2,
    }), { status: 200 }))
    global.fetch = fetchMock as typeof fetch
    const file = new File(['hello'], 'README.md', { type: 'text/markdown' })

    const result = await uploadAdminDocument(file, 'admin-key')

    const [, requestInit] = fetchMock.mock.calls[0]
    const headers = new Headers(requestInit?.headers)
    expect(headers.get('X-API-Key')).toBe('admin-key')
    expect(requestInit?.body).toBeInstanceOf(FormData)
    expect(result.filename).toBe('README.md')
  })

  it('sends X-API-Key for admin scans', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      status: 'Scan terminé',
    }), { status: 200 }))
    global.fetch = fetchMock as typeof fetch

    const result = await scanAdminDocuments('admin-key')

    const [, requestInit] = fetchMock.mock.calls[0]
    const headers = new Headers(requestInit?.headers)
    expect(headers.get('X-API-Key')).toBe('admin-key')
    expect(result.status).toBe('Scan terminé')
  })

  it('encodes document sources when deleting admin documents', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      source: 'nested/path.md',
      deleted: true,
      message: 'Documents deleted',
    }), { status: 200 }))
    global.fetch = fetchMock as typeof fetch

    const result = await deleteAdminDocument('nested/path.md', 'admin-key')

    const [url, requestInit] = fetchMock.mock.calls[0]
    const headers = new Headers(requestInit?.headers)
    expect(url).toContain('/documents/nested/path.md')
    expect(headers.get('X-API-Key')).toBe('admin-key')
    expect(result.deleted).toBe(true)
  })
})
