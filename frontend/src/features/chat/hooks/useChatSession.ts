import { useCallback, useState, type Dispatch, type SetStateAction } from 'react'
import { createSession as apiCreateSession, sendChatMessage } from '../../../utils/api'
import {
  clearPersistedConversation,
  getPersistedMessages,
  getPersistedSession,
  persistMessages,
  persistSession,
  validateMessage,
  type Message,
} from '../../../utils/security'

type AssistantUpdater = (content: string) => void

function applyStreamChunk(
  line: string,
  fullResponseRef: { current: string },
  updateAssistantMessage: AssistantUpdater,
) {
  if (!line.startsWith('data:')) return

  const jsonStr = line.substring(5).trim()
  if (!jsonStr) return

  const data = JSON.parse(jsonStr)
  const content: string = data.content || data.text || ''

  if (content && data.type !== 'DONE') {
    fullResponseRef.current += content
    updateAssistantMessage(fullResponseRef.current)
  }
}

function appendUserMessage(messages: Message[], message: string) {
  const userMessage: Message = { id: Date.now(), role: 'user', content: message }
  const updatedMessages = [...messages, userMessage]
  persistMessages(updatedMessages)
  return updatedMessages
}

function createAssistantUpdater(setMessages: Dispatch<SetStateAction<Message[]>>): AssistantUpdater {
  return (content: string) => {
    setMessages((previous) => {
      const updated = [...previous]
      updated[updated.length - 1] = { ...updated[updated.length - 1], content }
      return updated
    })
  }
}

async function consumeResponseStream(
  response: Response,
  fullResponseRef: { current: string },
  updateAssistantMessage: AssistantUpdater,
) {
  const reader = response.body!.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    buffer += decoder.decode(value ?? new Uint8Array(), { stream: !done })

    const lines = buffer.split('\n')
    buffer = lines.pop() ?? ''

    for (const line of lines) {
      try {
        applyStreamChunk(line, fullResponseRef, updateAssistantMessage)
      } catch {
        // Ignore malformed event lines and keep consuming the stream.
      }
    }

    if (done) {
      break
    }
  }

  if (buffer.trim()) {
    try {
      applyStreamChunk(buffer, fullResponseRef, updateAssistantMessage)
    } catch {
      // Ignore trailing malformed payload.
    }
  }
}

function handleInterruptedStream(
  fullResponseRef: { current: string },
  updateAssistantMessage: AssistantUpdater,
  onError: (message: string | null) => void,
) {
  if (fullResponseRef.current) {
    fullResponseRef.current += '\n\n---\n*Réponse interrompue. Veuillez réessayer.*'
    updateAssistantMessage(fullResponseRef.current)
  }
  onError('Le flux de réponse a été interrompu.')
}

function buildAssistantErrorMessage(): Message {
  return {
    id: Date.now() + 2,
    role: 'assistant',
    content: 'Erreur de connexion. Veuillez relancer le backend.',
    isError: true,
  }
}

export function useChatSession(onError: (message: string | null) => void) {
  const [sessionId, setSessionId] = useState<string | null>(() => getPersistedSession())
  const [messages, setMessages] = useState<Message[]>(() => getPersistedMessages())
  const [isLoading, setIsLoading] = useState(false)

  const createSession = useCallback(async (): Promise<string | null> => {
    try {
      onError(null)
      const session = await apiCreateSession()
      setSessionId(session.id)
      persistSession(session.id)
      setMessages([])
      persistMessages([])
      return session.id
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Erreur inconnue')
      return null
    }
  }, [onError])

  const sendMessage = async (message: string) => {
    if (!sessionId) return

    const { valid, error } = validateMessage(message)
    if (!valid) {
      onError(error)
      return
    }

    setIsLoading(true)
    onError(null)

    const updatedWithUser = appendUserMessage(messages, message)
    setMessages(updatedWithUser)

    const fullResponseRef = { current: '' }
    const assistantMessage: Message = { id: Date.now() + 1, role: 'assistant', content: '' }
    const updateAssistantMessage = createAssistantUpdater(setMessages)

    try {
      const response = await sendChatMessage(sessionId, message)
      setMessages((previous) => [...previous, assistantMessage])

      try {
        await consumeResponseStream(response, fullResponseRef, updateAssistantMessage)
      } catch {
        handleInterruptedStream(fullResponseRef, updateAssistantMessage, onError)
      }
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Erreur inconnue')
      setMessages((previous) => [...previous, buildAssistantErrorMessage()])
    } finally {
      setIsLoading(false)
      setMessages((previous) => {
        persistMessages(previous)
        return previous
      })
    }
  }

  const clearConversation = useCallback(() => {
    clearPersistedConversation()
    setSessionId(null)
    setMessages([])
  }, [])

  return {
    sessionId,
    messages,
    isLoading,
    createSession,
    sendMessage,
    clearConversation,
  }
}
