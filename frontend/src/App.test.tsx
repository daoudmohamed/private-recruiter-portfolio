import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import App from './App'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const apiMocks = vi.hoisted(() => ({
  getRecruiterAccessSession: vi.fn(),
  requestRecruiterInvitation: vi.fn(),
  createSession: vi.fn(),
  consumeRecruiterAccessToken: vi.fn(),
  logoutRecruiterAccess: vi.fn(),
  sendChatMessage: vi.fn(),
}))

vi.mock('./utils/api', async () => {
  const actual = await vi.importActual<typeof import('./utils/api')>('./utils/api')
  return {
    ...actual,
    getRecruiterAccessSession: apiMocks.getRecruiterAccessSession,
    requestRecruiterInvitation: apiMocks.requestRecruiterInvitation,
    createSession: apiMocks.createSession,
    consumeRecruiterAccessToken: apiMocks.consumeRecruiterAccessToken,
    logoutRecruiterAccess: apiMocks.logoutRecruiterAccess,
    sendChatMessage: apiMocks.sendChatMessage,
  }
})

vi.mock('./components/ChatWindow', () => ({
  default: () => <div>chat-window</div>,
}))

vi.mock('./components/ErrorBoundary', () => ({
  default: ({ children }: { children: ReactNode }) => <>{children}</>,
}))

vi.mock('./components/sections/SkillsSection', () => ({
  SkillsSection: () => <div>skills-section</div>,
}))

vi.mock('./components/sections/ExperienceSection', () => ({
  ExperienceSection: () => <div>experience-section</div>,
}))

describe('App recruiter captcha flow', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    window.history.replaceState({}, '', '/')
    document.body.innerHTML = ''
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation(() => ({
        matches: false,
        media: '',
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    })
    apiMocks.createSession.mockResolvedValue({ id: 'session-1' })
    apiMocks.consumeRecruiterAccessToken.mockResolvedValue(undefined)
    apiMocks.logoutRecruiterAccess.mockResolvedValue(undefined)
    apiMocks.sendChatMessage.mockResolvedValue(new Response(''))
  })

  it('blocks invitation submission while recaptcha v3 is still loading', async () => {
    apiMocks.getRecruiterAccessSession.mockResolvedValue({
      enabled: true,
      authenticated: false,
      requestInvitationEnabled: true,
      captchaSiteKey: 'site-key',
      captchaAction: 'request_invitation',
    })

    render(<App />)

    const emailInput = await screen.findByPlaceholderText('prenom.nom@entreprise.com')
    fireEvent.change(emailInput, { target: { value: 'recruteur@example.com' } })
    fireEvent.click(screen.getByRole('button', { name: 'Recevoir mon invitation' }))

    expect(await screen.findByText('Le captcha est en cours de chargement. Veuillez reessayer dans quelques secondes.')).toBeTruthy()
    expect(apiMocks.requestRecruiterInvitation).not.toHaveBeenCalled()
  })

  it('executes recaptcha v3 with the configured action before requesting invitation', async () => {
    apiMocks.getRecruiterAccessSession.mockResolvedValue({
      enabled: true,
      authenticated: false,
      requestInvitationEnabled: true,
      captchaSiteKey: 'site-key',
      captchaAction: 'request_invitation',
    })
    apiMocks.requestRecruiterInvitation.mockResolvedValue({
      accepted: true,
      message: 'Invitation envoyee',
    })

    const execute = vi.fn().mockResolvedValue('captcha-token')
    window.grecaptcha = {
      ready: (callback: () => void) => callback(),
      execute,
    }
    const existingScript = document.createElement('script')
    existingScript.dataset.recaptcha = 'true'
    document.body.appendChild(existingScript)

    render(<App />)

    const emailInput = await screen.findByPlaceholderText('prenom.nom@entreprise.com')
    fireEvent.change(emailInput, { target: { value: 'recruteur@example.com' } })

    await waitFor(() => expect(screen.queryByText('Initialisation de la protection captcha...')).toBeNull())

    fireEvent.click(screen.getByRole('button', { name: 'Recevoir mon invitation' }))

    await waitFor(() => {
      expect(execute).toHaveBeenCalledWith('site-key', { action: 'request_invitation' })
      expect(apiMocks.requestRecruiterInvitation).toHaveBeenCalledWith('recruteur@example.com', 'captcha-token')
    })
  })

  it('loads the recaptcha script and surfaces a readiness error if recaptcha disappears before submit', async () => {
    apiMocks.getRecruiterAccessSession.mockResolvedValue({
      enabled: true,
      authenticated: false,
      requestInvitationEnabled: true,
      captchaSiteKey: 'site-key',
      captchaAction: 'request_invitation',
    })

    window.grecaptcha = {
      ready: (callback: () => void) => {
        callback()
        window.grecaptcha = undefined
      },
      execute: vi.fn(),
    }

    render(<App />)

    await waitFor(() => {
      expect(document.querySelector('script[data-recaptcha="true"]')).not.toBeNull()
    })
    const script = document.querySelector('script[data-recaptcha="true"]') as HTMLScriptElement
    expect(script.src).toContain('https://www.google.com/recaptcha/api.js?render=site-key')
    await act(async () => {
      script.onload?.(new Event('load'))
    })

    const emailInput = await screen.findByPlaceholderText('prenom.nom@entreprise.com')
    fireEvent.change(emailInput, { target: { value: 'recruteur@example.com' } })
    fireEvent.click(screen.getByRole('button', { name: 'Recevoir mon invitation' }))

    expect(await screen.findByText('Le service captcha n est pas encore pret. Veuillez reessayer dans quelques secondes.')).toBeTruthy()
    expect(apiMocks.requestRecruiterInvitation).not.toHaveBeenCalled()
  })
})
