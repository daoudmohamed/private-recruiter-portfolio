import { useCallback, useState } from 'react'
import {
  ApiError,
  deleteAdminDocument,
  listAdminDocuments,
  scanAdminDocuments,
  uploadAdminDocument,
  type DocumentsListResponse,
  type DocumentDeleteResult,
  type ScanResult,
  type UploadResult,
} from '../../../utils/api'

type AdminDocumentsState = {
  adminApiKey: string | null
  sources: string[]
  count: number
  isLoading: boolean
  isMutating: boolean
  statusMessage: string | null
  errorMessage: string | null
}

const INITIAL_STATE: AdminDocumentsState = {
  adminApiKey: null,
  sources: [],
  count: 0,
  isLoading: false,
  isMutating: false,
  statusMessage: null,
  errorMessage: null,
}

function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401
}

function unauthorizedMessage(): string {
  return 'Clé admin invalide ou expirée. Renseignez-la de nouveau.'
}

export function useAdminDocuments() {
  const [state, setState] = useState<AdminDocumentsState>(INITIAL_STATE)

  const clearAccess = useCallback((message: string = unauthorizedMessage()) => {
    setState({
      ...INITIAL_STATE,
      errorMessage: message,
    })
  }, [])

  const applyDocuments = useCallback((response: DocumentsListResponse, adminApiKey: string) => {
    setState((previous) => ({
      ...previous,
      adminApiKey,
      sources: response.sources,
      count: response.count,
      isLoading: false,
      errorMessage: null,
    }))
  }, [])

  const authenticate = useCallback(async (adminApiKey: string) => {
    setState((previous) => ({
      ...previous,
      isLoading: true,
      errorMessage: null,
      statusMessage: null,
    }))

    try {
      const response = await listAdminDocuments(adminApiKey)
      applyDocuments(response, adminApiKey)
      setState((previous) => ({
        ...previous,
        statusMessage: 'Accès admin validé.',
      }))
      return true
    } catch (error) {
      if (isUnauthorized(error)) {
        clearAccess()
        return false
      }

      setState((previous) => ({
        ...previous,
        isLoading: false,
        errorMessage: error instanceof Error ? error.message : 'Erreur inconnue',
      }))
      return false
    }
  }, [applyDocuments, clearAccess])

  const refreshDocuments = useCallback(async () => {
    if (!state.adminApiKey) {
      return false
    }

    setState((previous) => ({
      ...previous,
      isLoading: true,
      errorMessage: null,
    }))

    try {
      const response = await listAdminDocuments(state.adminApiKey)
      applyDocuments(response, state.adminApiKey)
      return true
    } catch (error) {
      if (isUnauthorized(error)) {
        clearAccess()
        return false
      }

      setState((previous) => ({
        ...previous,
        isLoading: false,
        errorMessage: error instanceof Error ? error.message : 'Erreur inconnue',
      }))
      return false
    }
  }, [applyDocuments, clearAccess, state.adminApiKey])

  const runMutation = useCallback(async <T,>(
    action: (adminApiKey: string) => Promise<T>,
    successMessage: (result: T) => string,
  ) => {
    if (!state.adminApiKey) {
      return false
    }

    setState((previous) => ({
      ...previous,
      isMutating: true,
      errorMessage: null,
      statusMessage: null,
    }))

    try {
      const result = await action(state.adminApiKey)
      const response = await listAdminDocuments(state.adminApiKey)
      applyDocuments(response, state.adminApiKey)
      setState((previous) => ({
        ...previous,
        isMutating: false,
        statusMessage: successMessage(result),
      }))
      return true
    } catch (error) {
      if (isUnauthorized(error)) {
        clearAccess()
        return false
      }

      setState((previous) => ({
        ...previous,
        isMutating: false,
        errorMessage: error instanceof Error ? error.message : 'Erreur inconnue',
      }))
      return false
    }
  }, [applyDocuments, clearAccess, state.adminApiKey])

  const uploadDocument = useCallback((file: File) => runMutation<UploadResult>(
    (adminApiKey) => uploadAdminDocument(file, adminApiKey),
    (result) => result.message ?? `${file.name} importé avec succès.`,
  ), [runMutation])

  const scanDocuments = useCallback(() => runMutation<ScanResult>(
    (adminApiKey) => scanAdminDocuments(adminApiKey),
    (result) => result.status ?? 'Analyse des documents terminée.',
  ), [runMutation])

  const deleteDocument = useCallback((source: string) => runMutation<DocumentDeleteResult>(
    (adminApiKey) => deleteAdminDocument(source, adminApiKey),
    (result) => result.message,
  ), [runMutation])

  return {
    ...state,
    authenticate,
    refreshDocuments,
    uploadDocument,
    scanDocuments,
    deleteDocument,
    clearAccess,
  }
}
