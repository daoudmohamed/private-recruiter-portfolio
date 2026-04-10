import { act, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../utils/api'
import { useAdminDocuments } from './useAdminDocuments'

const apiMocks = vi.hoisted(() => ({
  listAdminDocuments: vi.fn(),
  uploadAdminDocument: vi.fn(),
  scanAdminDocuments: vi.fn(),
  deleteAdminDocument: vi.fn(),
}))

vi.mock('../../../utils/api', async () => {
  const actual = await vi.importActual<typeof import('../../../utils/api')>('../../../utils/api')
  return {
    ...actual,
    listAdminDocuments: apiMocks.listAdminDocuments,
    uploadAdminDocument: apiMocks.uploadAdminDocument,
    scanAdminDocuments: apiMocks.scanAdminDocuments,
    deleteAdminDocument: apiMocks.deleteAdminDocument,
  }
})

describe('useAdminDocuments', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('authenticates and loads sources with a valid admin api key', async () => {
    apiMocks.listAdminDocuments.mockResolvedValue({ sources: ['cv.md'], count: 1 })
    const { result } = renderHook(() => useAdminDocuments())

    await act(async () => {
      expect(await result.current.authenticate('admin-key')).toBe(true)
    })

    expect(apiMocks.listAdminDocuments).toHaveBeenCalledWith('admin-key')
    expect(result.current.adminApiKey).toBe('admin-key')
    expect(result.current.sources).toEqual(['cv.md'])
    expect(result.current.count).toBe(1)
    expect(result.current.statusMessage).toBe('Accès admin validé.')
  })

  it('clears in-memory access when authentication is unauthorized', async () => {
    apiMocks.listAdminDocuments.mockRejectedValue(new ApiError('forbidden', 401))
    const { result } = renderHook(() => useAdminDocuments())

    await act(async () => {
      expect(await result.current.authenticate('wrong-key')).toBe(false)
    })

    expect(result.current.adminApiKey).toBeNull()
    expect(result.current.sources).toEqual([])
    expect(result.current.errorMessage).toContain('Clé admin invalide')
  })

  it('refreshes after a successful upload mutation', async () => {
    apiMocks.listAdminDocuments
      .mockResolvedValueOnce({ sources: ['cv.md'], count: 1 })
      .mockResolvedValueOnce({ sources: ['cv.md', 'README.md'], count: 2 })
    apiMocks.uploadAdminDocument.mockResolvedValue({ message: 'Document importé', chunksCreated: 2 })

    const { result } = renderHook(() => useAdminDocuments())

    await act(async () => {
      await result.current.authenticate('admin-key')
    })

    const file = new File(['content'], 'README.md', { type: 'text/markdown' })

    await act(async () => {
      expect(await result.current.uploadDocument(file)).toBe(true)
    })

    expect(apiMocks.uploadAdminDocument).toHaveBeenCalledWith(file, 'admin-key')
    expect(result.current.sources).toEqual(['cv.md', 'README.md'])
    expect(result.current.statusMessage).toBe('Document importé')
  })

  it('drops access when a mutation returns unauthorized', async () => {
    apiMocks.listAdminDocuments.mockResolvedValue({ sources: ['cv.md'], count: 1 })
    apiMocks.deleteAdminDocument.mockRejectedValue(new ApiError('unauthorized', 401))

    const { result } = renderHook(() => useAdminDocuments())

    await act(async () => {
      await result.current.authenticate('admin-key')
    })

    await act(async () => {
      expect(await result.current.deleteDocument('cv.md')).toBe(false)
    })

    expect(result.current.adminApiKey).toBeNull()
    expect(result.current.sources).toEqual([])
    expect(result.current.errorMessage).toContain('Clé admin invalide')
  })

  it('exposes non-auth errors without clearing the current access state', async () => {
    apiMocks.listAdminDocuments.mockResolvedValue({ sources: ['cv.md'], count: 1 })
    apiMocks.scanAdminDocuments.mockRejectedValue(new Error('backend scan failed'))

    const { result } = renderHook(() => useAdminDocuments())

    await act(async () => {
      await result.current.authenticate('admin-key')
    })

    await act(async () => {
      expect(await result.current.scanDocuments()).toBe(false)
    })

    expect(result.current.adminApiKey).toBe('admin-key')
    expect(result.current.errorMessage).toBe('backend scan failed')
  })

  it('returns false when refresh is requested without an authenticated admin key', async () => {
    const { result } = renderHook(() => useAdminDocuments())

    await act(async () => {
      expect(await result.current.refreshDocuments()).toBe(false)
    })

    expect(apiMocks.listAdminDocuments).not.toHaveBeenCalled()
  })

  it('surfaces non-authentication errors during admin authentication', async () => {
    apiMocks.listAdminDocuments.mockRejectedValue(new Error('admin backend unavailable'))
    const { result } = renderHook(() => useAdminDocuments())

    await act(async () => {
      expect(await result.current.authenticate('admin-key')).toBe(false)
    })

    expect(result.current.adminApiKey).toBeNull()
    expect(result.current.errorMessage).toBe('admin backend unavailable')
  })

  it('clears current admin access explicitly', async () => {
    apiMocks.listAdminDocuments.mockResolvedValue({ sources: ['cv.md'], count: 1 })
    const { result } = renderHook(() => useAdminDocuments())

    await act(async () => {
      await result.current.authenticate('admin-key')
    })

    act(() => {
      result.current.clearAccess('manual reset')
    })

    expect(result.current.adminApiKey).toBeNull()
    expect(result.current.sources).toEqual([])
    expect(result.current.count).toBe(0)
    expect(result.current.errorMessage).toBe('manual reset')
  })
})
