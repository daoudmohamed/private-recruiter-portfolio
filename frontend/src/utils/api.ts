const API_BASE = import.meta.env.VITE_API_BASE || '/api/v1'

const DEFAULT_TIMEOUT = 30_000    // 30s
const STREAMING_TIMEOUT = 120_000 // 120s
const UPLOAD_TIMEOUT = 60_000     // 60s

const MAX_RETRIES = 3
const RETRY_BASE_DELAY = 1000 // 1s

export interface SessionResponse {
  id: string
}

export interface UploadResult {
  chunksCreated: number
  status?: string
}

export interface ScanResult {
  status?: string
}

function buildHeaders(apiKey: string): Record<string, string> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (apiKey) {
    headers['X-API-Key'] = apiKey
  }
  return headers
}

function errorMessageForStatus(status: number): string {
  switch (status) {
    case 401: return 'Cle API invalide ou manquante.'
    case 403: return 'Acces refuse.'
    case 413: return 'Le contenu envoye est trop volumineux.'
    case 429: return 'Trop de requetes. Veuillez patienter.'
    default:  return `Erreur serveur (${status}).`
  }
}

export async function fetchWithTimeout(
  url: string,
  options: RequestInit = {},
  timeout = DEFAULT_TIMEOUT
): Promise<Response> {
  const controller = new AbortController()
  const existingSignal = options.signal

  const timeoutId = setTimeout(() => controller.abort(), timeout)

  try {
    const response = await fetch(url, {
      ...options,
      signal: controller.signal,
    })
    return response
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') {
      if (existingSignal?.aborted) throw error
      throw new Error('La requete a expire. Verifiez votre connexion.')
    }
    throw error
  } finally {
    clearTimeout(timeoutId)
  }
}

export async function fetchWithRetry(
  url: string,
  options: RequestInit = {},
  { maxRetries = MAX_RETRIES, timeout = DEFAULT_TIMEOUT } = {}
): Promise<Response> {
  let lastError: Error | undefined

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      const response = await fetchWithTimeout(url, options, timeout)

      if (response.ok || response.status < 500) {
        return response
      }

      lastError = new Error(errorMessageForStatus(response.status))
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error))
      // Don't retry on abort or client errors
      if (error instanceof Error && error.name === 'AbortError') throw error
    }

    if (attempt < maxRetries) {
      const delay = RETRY_BASE_DELAY * Math.pow(2, attempt)
      await new Promise(resolve => setTimeout(resolve, delay))
    }
  }

  throw lastError
}

// --- API functions ---

export async function createSession(apiKey: string): Promise<SessionResponse> {
  const response = await fetchWithRetry(`${API_BASE}/sessions`, {
    method: 'POST',
    headers: buildHeaders(apiKey),
    body: JSON.stringify({}),
  })

  if (!response.ok) {
    throw new Error(errorMessageForStatus(response.status))
  }

  return response.json()
}

export async function sendChatMessage(sessionId: string, message: string, apiKey: string): Promise<Response> {
  const response = await fetchWithTimeout(`${API_BASE}/chat`, {
    method: 'POST',
    headers: buildHeaders(apiKey),
    body: JSON.stringify({ sessionId, message }),
  }, STREAMING_TIMEOUT)

  if (!response.ok) {
    throw new Error(errorMessageForStatus(response.status))
  }

  return response
}

export async function uploadDocument(file: File, apiKey: string): Promise<UploadResult> {
  const formData = new FormData()
  formData.append('file', file)

  const headers: Record<string, string> = {}
  if (apiKey) {
    headers['X-API-Key'] = apiKey
  }

  const response = await fetchWithTimeout(`${API_BASE}/documents/upload`, {
    method: 'POST',
    headers,
    body: formData,
  }, UPLOAD_TIMEOUT)

  if (!response.ok) {
    throw new Error(errorMessageForStatus(response.status))
  }

  return response.json()
}

export async function scanDocuments(apiKey: string): Promise<ScanResult> {
  const headers: Record<string, string> = {}
  if (apiKey) {
    headers['X-API-Key'] = apiKey
  }

  const response = await fetchWithRetry(`${API_BASE}/documents/scan`, {
    method: 'POST',
    headers,
  })

  if (!response.ok) {
    throw new Error(errorMessageForStatus(response.status))
  }

  return response.json()
}
