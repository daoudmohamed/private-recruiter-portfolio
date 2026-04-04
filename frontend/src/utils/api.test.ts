import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchWithTimeout } from './api'

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
