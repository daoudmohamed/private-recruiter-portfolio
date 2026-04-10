import { useCallback, useEffect, useState } from 'react'
import {
  ApiError,
  consumeRecruiterAccessToken,
  getRecruiterAccessSession,
  logoutRecruiterAccess,
} from '../../../utils/api'
import { INITIAL_ACCESS_STATE, type AccessState } from '../types'

function messageForAccessError(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'recruiter_access.link_already_used':
        return 'Ce lien a deja ete utilise. Demandez un nouveau lien ou reutilisez ce navigateur si votre acces est deja actif.'
      case 'recruiter_access.link_expired':
        return 'Ce lien a expire. Demandez un nouveau lien d acces.'
      case 'recruiter_access.invalid_link':
        return 'Ce lien d acces est invalide.'
      default:
        return error.message
    }
  }
  return error instanceof Error ? error.message : 'Erreur inconnue'
}

export function useRecruiterAccess(onError: (message: string | null) => void) {
  const [accessState, setAccessState] = useState<AccessState>(INITIAL_ACCESS_STATE)
  const [accessStatusMessage, setAccessStatusMessage] = useState<string | null>(null)

  const refreshRecruiterAccess = useCallback(async () => {
    const access = await getRecruiterAccessSession()
    setAccessState({
      enabled: access.enabled,
      authenticated: access.authenticated,
      requestInvitationEnabled: access.requestInvitationEnabled,
      captchaSiteKey: access.captchaSiteKey ?? null,
      captchaAction: access.captchaAction ?? null,
      expiresAt: access.expiresAt ?? null,
      isChecking: false,
    })
  }, [])

  useEffect(() => {
    const token = new URLSearchParams(globalThis.location.search).get('token')

    const bootstrap = async () => {
      try {
        onError(null)
        setAccessStatusMessage(null)

        if (token) {
          try {
            const consumed = await consumeRecruiterAccessToken(token)
            globalThis.history.replaceState({}, document.title, globalThis.location.pathname)
            setAccessState({
              enabled: consumed.enabled,
              authenticated: consumed.authenticated,
              requestInvitationEnabled: consumed.requestInvitationEnabled,
              captchaSiteKey: consumed.captchaSiteKey ?? null,
              captchaAction: consumed.captchaAction ?? null,
              expiresAt: consumed.expiresAt ?? null,
              isChecking: false,
            })
          } catch (error) {
            globalThis.history.replaceState({}, document.title, globalThis.location.pathname)
            const session = await getRecruiterAccessSession()
            setAccessState({
              enabled: session.enabled,
              authenticated: session.authenticated,
              requestInvitationEnabled: session.requestInvitationEnabled,
              captchaSiteKey: session.captchaSiteKey ?? null,
              captchaAction: session.captchaAction ?? null,
              expiresAt: session.expiresAt ?? null,
              isChecking: false,
            })
            setAccessStatusMessage(
              session.authenticated
                ? 'Votre acces est deja actif sur ce navigateur.'
                : messageForAccessError(error),
            )
          }
          return
        }

        await refreshRecruiterAccess()
      } catch (error) {
        setAccessState((previous) => ({
          ...previous,
          enabled: true,
          authenticated: false,
          captchaAction: previous.captchaAction,
          isChecking: false,
        }))
        onError(error instanceof Error ? error.message : 'Erreur inconnue')
      }
    }

    void bootstrap()
  }, [onError, refreshRecruiterAccess])

  const logout = async () => {
    try {
      await logoutRecruiterAccess()
    } finally {
      setAccessState((previous) => ({
        enabled: true,
        authenticated: false,
        requestInvitationEnabled: previous.requestInvitationEnabled,
        captchaSiteKey: previous.captchaSiteKey,
        captchaAction: previous.captchaAction,
        expiresAt: null,
        isChecking: false,
      }))
    }
  }

  return {
    accessState,
    accessStatusMessage,
    logout,
  }
}
