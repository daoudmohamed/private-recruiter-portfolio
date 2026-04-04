const API_BASE = import.meta.env.VITE_API_BASE || '/api/v1'

const DEFAULT_TIMEOUT = 30_000    // 30s
const STREAMING_TIMEOUT = 120_000 // 120s
const UPLOAD_TIMEOUT = 60_000     // 60s

const MAX_RETRIES = 3
const RETRY_BASE_DELAY = 1000 // 1s

export interface SessionResponse {
  id: string
}

export interface RecruiterAccessSessionResponse {
  enabled: boolean
  authenticated: boolean
  requestInvitationEnabled: boolean
  captchaSiteKey?: string
  expiresAt?: string
}

export interface RequestRecruiterInvitationResponse {
  accepted: boolean
  message: string
}

export interface UploadResult {
  chunksCreated: number
  status?: string
}

export interface ScanResult {
  status?: string
}

type ErrorBody = {
  message?: string
  code?: string
}

export class ApiError extends Error {
  status: number
  code?: string

  constructor(message: string, status: number, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

function buildJsonHeaders(): Record<string, string> {
  return { 'Content-Type': 'application/json' }
}

function errorMessageForStatus(status: number): string {
  switch (status) {
    case 401: return 'Acces temporaire requis ou expire.'
    case 403: return 'Acces refuse.'
    case 413: return 'Le contenu envoye est trop volumineux.'
    case 429: return 'Trop de requetes. Veuillez patienter.'
    default:  return `Erreur serveur (${status}).`
  }
}

async function buildError(response: Response): Promise<Error> {
  try {
    const body = await response.clone().json() as ErrorBody
    if (body?.message) {
      return new ApiError(body.message, response.status, body.code)
    }
  } catch {
    // Ignore non-JSON bodies.
  }

  return new ApiError(errorMessageForStatus(response.status), response.status)
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
      credentials: 'include',
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

export async function createSession(): Promise<SessionResponse> {
  const response = await fetchWithRetry(`${API_BASE}/sessions`, {
    method: 'POST',
    headers: buildJsonHeaders(),
    body: JSON.stringify({}),
  })

  if (!response.ok) {
    throw await buildError(response)
  }

  return response.json()
}

export async function sendChatMessage(sessionId: string, message: string): Promise<Response> {
  const response = await fetchWithTimeout(`${API_BASE}/chat`, {
    method: 'POST',
    headers: buildJsonHeaders(),
    body: JSON.stringify({ sessionId, message }),
  }, STREAMING_TIMEOUT)

  if (!response.ok) {
    throw await buildError(response)
  }

  return response
}

export async function uploadDocument(file: File): Promise<UploadResult> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await fetchWithTimeout(`${API_BASE}/documents/upload`, {
    method: 'POST',
    body: formData,
  }, UPLOAD_TIMEOUT)

  if (!response.ok) {
    throw await buildError(response)
  }

  return response.json()
}

export async function scanDocuments(): Promise<ScanResult> {
  const response = await fetchWithRetry(`${API_BASE}/documents/scan`, {
    method: 'POST',
  })

  if (!response.ok) {
    throw await buildError(response)
  }

  return response.json()
}

export async function getRecruiterAccessSession(): Promise<RecruiterAccessSessionResponse> {
  const response = await fetchWithRetry(`${API_BASE}/recruiter-access/session`, {
    method: 'GET',
  })

  if (!response.ok) {
    throw await buildError(response)
  }

  return response.json()
}

export async function requestRecruiterInvitation(
  email: string,
  captchaToken?: string
): Promise<RequestRecruiterInvitationResponse> {
  const response = await fetchWithRetry(`${API_BASE}/recruiter-access/request-invitation`, {
    method: 'POST',
    headers: buildJsonHeaders(),
    body: JSON.stringify({ email, captchaToken }),
  })

  if (!response.ok) {
    throw await buildError(response)
  }

  return response.json()
}

export async function consumeRecruiterAccessToken(token: string): Promise<RecruiterAccessSessionResponse> {
  const response = await fetchWithRetry(`${API_BASE}/recruiter-access/consume`, {
    method: 'POST',
    headers: buildJsonHeaders(),
    body: JSON.stringify({ token }),
  })

  if (!response.ok) {
    throw await buildError(response)
  }

  return response.json()
}

export async function logoutRecruiterAccess(): Promise<void> {
  const response = await fetchWithRetry(`${API_BASE}/recruiter-access/logout`, {
    method: 'POST',
  })

  if (!response.ok) {
    throw await buildError(response)
  }
}
