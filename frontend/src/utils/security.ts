export const MAX_MESSAGE_LENGTH = 4000
export const MAX_FILE_SIZE = 10 * 1024 * 1024 // 10 Mo

const ALLOWED_EXTENSIONS = ['.pdf', '.txt', '.md']
const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'text/plain',
  'text/markdown',
  'text/x-markdown',
]

const SESSION_KEY = 'kb_session'
const MESSAGES_KEY = 'kb_messages'
const API_KEY_KEY = 'kb_api_key'
const MAX_PERSISTED_MESSAGES = 50

export interface Message {
  id: number
  role: 'user' | 'assistant'
  content: string
  isError?: boolean
}

export interface ValidationResult {
  valid: boolean
  error: string | null
}

// --- API Key (sessionStorage) ---

export function getApiKey(): string {
  try {
    return sessionStorage.getItem(API_KEY_KEY) || ''
  } catch {
    return ''
  }
}

export function setApiKey(key: string): void {
  try {
    if (key) {
      sessionStorage.setItem(API_KEY_KEY, key)
    } else {
      sessionStorage.removeItem(API_KEY_KEY)
    }
  } catch {
    // sessionStorage not available
  }
}

export function clearApiKey(): void {
  try {
    sessionStorage.removeItem(API_KEY_KEY)
  } catch {
    // sessionStorage not available
  }
}

// --- Session persistence (sessionStorage) ---

export function getPersistedSession(): string | null {
  try {
    return sessionStorage.getItem(SESSION_KEY) || null
  } catch {
    return null
  }
}

export function persistSession(sessionId: string | null): void {
  try {
    if (sessionId) {
      sessionStorage.setItem(SESSION_KEY, sessionId)
    } else {
      sessionStorage.removeItem(SESSION_KEY)
    }
  } catch {
    // sessionStorage not available
  }
}

export function getPersistedMessages(): Message[] {
  try {
    const raw = sessionStorage.getItem(MESSAGES_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export function persistMessages(messages: Message[]): void {
  try {
    const trimmed = messages.slice(-MAX_PERSISTED_MESSAGES)
    sessionStorage.setItem(MESSAGES_KEY, JSON.stringify(trimmed))
  } catch {
    // sessionStorage not available or quota exceeded
  }
}

// --- Input validation ---

export function validateMessage(msg: string): ValidationResult {
  if (!msg || typeof msg !== 'string') {
    return { valid: false, error: 'Le message ne peut pas etre vide.' }
  }
  const trimmed = msg.trim()
  if (trimmed.length === 0) {
    return { valid: false, error: 'Le message ne peut pas etre vide.' }
  }
  if (trimmed.length > MAX_MESSAGE_LENGTH) {
    return { valid: false, error: `Le message depasse la limite de ${MAX_MESSAGE_LENGTH} caracteres.` }
  }
  return { valid: true, error: null }
}

// --- File validation ---

export function validateFile(file: File): ValidationResult {
  if (!file) {
    return { valid: false, error: 'Aucun fichier fourni.' }
  }

  if (file.size > MAX_FILE_SIZE) {
    const sizeMb = (file.size / (1024 * 1024)).toFixed(1)
    return { valid: false, error: `${file.name} est trop volumineux (${sizeMb} Mo, max 10 Mo).` }
  }

  const ext = '.' + file.name.split('.').pop()!.toLowerCase()
  if (!ALLOWED_EXTENSIONS.includes(ext)) {
    return { valid: false, error: `${file.name} : extension "${ext}" non supportee (PDF, TXT, MD uniquement).` }
  }

  if (file.type && !ALLOWED_MIME_TYPES.includes(file.type)) {
    return { valid: false, error: `${file.name} : type MIME "${file.type}" non supporte.` }
  }

  return { valid: true, error: null }
}
