import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import App from './App'
import { ApiError } from './utils/api'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const apiMocks = vi.hoisted(() => ({
  getRecruiterAccessSession: vi.fn(),
  requestRecruiterInvitation: vi.fn(),
  createSession: vi.fn(),
  consumeRecruiterAccessToken: vi.fn(),
  logoutRecruiterAccess: vi.fn(),
  sendChatMessage: vi.fn(),
  listAdminDocuments: vi.fn(),
  uploadAdminDocument: vi.fn(),
  scanAdminDocuments: vi.fn(),
  deleteAdminDocument: vi.fn(),
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
    listAdminDocuments: apiMocks.listAdminDocuments,
    uploadAdminDocument: apiMocks.uploadAdminDocument,
    scanAdminDocuments: apiMocks.scanAdminDocuments,
    deleteAdminDocument: apiMocks.deleteAdminDocument,
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

function buildAccessSession(overrides: Partial<{
  enabled: boolean
  authenticated: boolean
  requestInvitationEnabled: boolean
  captchaSiteKey: string
  captchaAction: string
  expiresAt: string
}> = {}) {
  return {
    enabled: true,
    authenticated: false,
    requestInvitationEnabled: true,
    captchaSiteKey: 'site-key',
    captchaAction: 'request_invitation',
    ...overrides,
  }
}

function createDeferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void

  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })

  return { promise, resolve, reject }
}

describe('App recruiter captcha flow', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    window.sessionStorage.clear()
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
    globalThis.IntersectionObserver = vi.fn().mockImplementation(() => ({
      observe: vi.fn(),
      unobserve: vi.fn(),
      disconnect: vi.fn(),
      takeRecords: vi.fn(() => []),
      root: null,
      rootMargin: '',
      thresholds: [],
    })) as unknown as typeof IntersectionObserver
    apiMocks.createSession.mockResolvedValue({ id: 'session-1' })
    apiMocks.consumeRecruiterAccessToken.mockResolvedValue(undefined)
    apiMocks.logoutRecruiterAccess.mockResolvedValue(undefined)
    apiMocks.sendChatMessage.mockResolvedValue(new Response(''))
    apiMocks.listAdminDocuments.mockResolvedValue({ sources: [], count: 0 })
    apiMocks.uploadAdminDocument.mockResolvedValue({ message: 'Document importé', chunksCreated: 2 })
    apiMocks.scanAdminDocuments.mockResolvedValue({ status: 'Scan terminé' })
    apiMocks.deleteAdminDocument.mockResolvedValue({ source: 'cv.md', deleted: true, message: 'Documents deleted' })
  })

  it('blocks invitation submission while recaptcha v3 is still loading', async () => {
    apiMocks.getRecruiterAccessSession.mockResolvedValue(buildAccessSession())

    render(<App />)

    const emailInput = await screen.findByPlaceholderText('prenom.nom@entreprise.com')
    fireEvent.change(emailInput, { target: { value: 'recruteur@example.com' } })
    fireEvent.click(screen.getByRole('button', { name: 'Recevoir mon invitation' }))

    expect(await screen.findByText('Le captcha est en cours de chargement. Veuillez reessayer dans quelques secondes.')).toBeTruthy()
    expect(apiMocks.requestRecruiterInvitation).not.toHaveBeenCalled()
  })

  it('executes recaptcha v3 with the configured action before requesting invitation', async () => {
    apiMocks.getRecruiterAccessSession.mockResolvedValue(buildAccessSession())
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
    apiMocks.getRecruiterAccessSession.mockResolvedValue(buildAccessSession())

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

  it('shows the access check screen until the recruiter session bootstrap resolves', async () => {
    const deferredSession = createDeferred<ReturnType<typeof buildAccessSession>>()
    apiMocks.getRecruiterAccessSession.mockReturnValue(deferredSession.promise)

    render(<App />)

    expect(screen.getByText('Vérification de votre accès')).toBeTruthy()

    deferredSession.resolve(buildAccessSession())

    expect(await screen.findByPlaceholderText('prenom.nom@entreprise.com')).toBeTruthy()
  })

  it('creates a chat session for authenticated recruiters and allows logout', async () => {
    apiMocks.getRecruiterAccessSession.mockResolvedValue(
      buildAccessSession({
        authenticated: true,
        expiresAt: '2026-04-10T08:15:00Z',
      }),
    )

    render(<App />)

    await waitFor(() => expect(apiMocks.createSession).toHaveBeenCalledTimes(1))
    expect((await screen.findAllByText((_, element) => element?.textContent?.includes("Accès actif jusqu'au") ?? false)).length).toBeGreaterThan(0)

    const themeToggle = screen.getByTitle('Activer le mode clair')
    fireEvent.click(themeToggle)
    expect(screen.getByTitle('Activer le mode sombre')).toBeTruthy()

    fireEvent.click(screen.getByTitle('Se déconnecter'))

    await waitFor(() => expect(apiMocks.logoutRecruiterAccess).toHaveBeenCalledTimes(1))
    expect(await screen.findByText('Accès recruteur requis')).toBeTruthy()
  })

  it('clears the local conversation even when recruiter logout fails', async () => {
    window.sessionStorage.setItem('kb_session', 'persisted-session')
    window.sessionStorage.setItem(
      'kb_messages',
      JSON.stringify([{ id: 1, role: 'user', content: 'Bonjour' }]),
    )
    apiMocks.getRecruiterAccessSession.mockResolvedValue(
      buildAccessSession({
        authenticated: true,
        expiresAt: '2026-04-10T08:15:00Z',
      }),
    )
    apiMocks.logoutRecruiterAccess.mockRejectedValue(new Error('Déconnexion serveur indisponible'))

    render(<App />)

    expect(await screen.findByTitle('Se déconnecter')).toBeTruthy()
    fireEvent.click(screen.getByTitle('Se déconnecter'))

    await waitFor(() => expect(apiMocks.logoutRecruiterAccess).toHaveBeenCalledTimes(1))
    expect(window.sessionStorage.getItem('kb_session')).toBeNull()
    expect(window.sessionStorage.getItem('kb_messages')).toBeNull()
    expect(await screen.findByText('Accès recruteur requis')).toBeTruthy()
    expect((await screen.findByRole('alert')).textContent).toContain('Déconnexion serveur indisponible')
  })

  it('does not create duplicate backend sessions during authenticated rerenders before the first session resolves', async () => {
    const deferredCreateSession = createDeferred<{ id: string }>()
    apiMocks.getRecruiterAccessSession.mockResolvedValue(
      buildAccessSession({
        authenticated: true,
        expiresAt: '2026-04-10T08:15:00Z',
      }),
    )
    apiMocks.createSession.mockReturnValue(deferredCreateSession.promise)

    render(<App />)

    await waitFor(() => expect(apiMocks.createSession).toHaveBeenCalledTimes(1))

    fireEvent.click(screen.getByTitle('Activer le mode clair'))

    await waitFor(() => expect(apiMocks.createSession).toHaveBeenCalledTimes(1))

    deferredCreateSession.resolve({ id: 'session-1' })

    expect(await screen.findByTitle('Activer le mode sombre')).toBeTruthy()
  })

  it('surfaces a recruiter access bootstrap error and lets the user dismiss it', async () => {
    apiMocks.getRecruiterAccessSession.mockRejectedValue(new Error('Backend indisponible'))

    render(<App />)

    expect((await screen.findByRole('alert')).textContent).toContain('Backend indisponible')

    fireEvent.click(screen.getByRole('button', { name: 'Fermer' }))

    await waitFor(() => expect(screen.queryByRole('alert')).toBeNull())
  })

  it('shows the mapped access-link error when token consumption fails for an unauthenticated browser', async () => {
    window.history.replaceState({}, '', '/?token=expired')
    apiMocks.consumeRecruiterAccessToken.mockRejectedValue(
      new ApiError('Lien expiré', 410, 'recruiter_access.link_expired'),
    )
    apiMocks.getRecruiterAccessSession.mockResolvedValue(buildAccessSession())

    render(<App />)

    expect(await screen.findByText('Ce lien a expire. Demandez un nouveau lien d acces.')).toBeTruthy()
    expect(window.location.search).toBe('')
  })

  it('falls back to the manual contact CTA when invitation self-service is disabled', async () => {
    apiMocks.getRecruiterAccessSession.mockResolvedValue(
      buildAccessSession({
        requestInvitationEnabled: false,
        captchaSiteKey: '',
        captchaAction: '',
      }),
    )

    render(<App />)

    const fallbackLink = await screen.findByRole('link', { name: 'Demander un accès' })
    expect(fallbackLink.getAttribute('href')).toContain('mailto:daoud.mohamed.tn@gmail.com')
  })

  it('renders the hidden admin documents route with in-memory api key auth', async () => {
    window.history.replaceState({}, '', '/admin/documents')
    apiMocks.listAdminDocuments.mockResolvedValue({
      sources: ['README.md', 'cv.pdf'],
      count: 2,
    })

    render(<App />)

    expect(await screen.findByText('Gestion documentaire')).toBeTruthy()
    fireEvent.change(screen.getByPlaceholderText('ADMIN_API_KEY'), { target: { value: 'admin-secret' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir l interface' }))

    await waitFor(() => expect(apiMocks.listAdminDocuments).toHaveBeenCalledWith('admin-secret'))
    expect(await screen.findByText('Base documentaire')).toBeTruthy()
    expect(screen.getByText('README.md')).toBeTruthy()
    expect(screen.getByText('cv.pdf')).toBeTruthy()
  })

  it('supports upload, scan, delete and local key clearing on the admin documents route', async () => {
    window.history.replaceState({}, '', '/admin/documents')
    apiMocks.listAdminDocuments
      .mockResolvedValueOnce({ sources: ['cv.md'], count: 1 })
      .mockResolvedValueOnce({ sources: ['cv.md', 'README.md'], count: 2 })
      .mockResolvedValueOnce({ sources: ['cv.md', 'README.md'], count: 2 })
      .mockResolvedValueOnce({ sources: ['README.md'], count: 1 })
    const confirmSpy = vi.spyOn(globalThis, 'confirm').mockReturnValue(true)

    render(<App />)

    fireEvent.change(await screen.findByPlaceholderText('ADMIN_API_KEY'), { target: { value: 'admin-secret' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir l interface' }))

    expect(await screen.findByText('cv.md')).toBeTruthy()

    const file = new File(['hello'], 'README.md', { type: 'text/markdown' })
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement
    await act(async () => {
      fireEvent.change(fileInput, { target: { files: [file] } })
    })

    await waitFor(() => expect(apiMocks.uploadAdminDocument).toHaveBeenCalledWith(file, 'admin-secret'))
    expect(await screen.findByText('README.md')).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: 'Lancer un scan' }))
    await waitFor(() => expect(apiMocks.scanAdminDocuments).toHaveBeenCalledWith('admin-secret'))

    fireEvent.click(screen.getAllByRole('button', { name: 'Supprimer' })[0])
    await waitFor(() => expect(apiMocks.deleteAdminDocument).toHaveBeenCalledWith('cv.md', 'admin-secret'))
    expect(confirmSpy).toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Effacer la clé' }))
    expect(await screen.findByText('Gestion documentaire')).toBeTruthy()
    expect(screen.getByText('Clé admin effacée localement.')).toBeTruthy()

    confirmSpy.mockRestore()
  })
})
