import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { LockKeyhole } from 'lucide-react'
import { AdminDocumentsPage } from '../features/admin-documents/components/AdminDocumentsPage'
import { useChatSession } from '../features/chat/hooks/useChatSession'
import { PortfolioLanding } from '../features/portfolio/components/PortfolioLanding'
import { ContactSection } from '../features/portfolio/components/ContactSection'
import { AccessGate } from '../features/recruiter-access/components/AccessGate'
import { useRecruiterAccess } from '../features/recruiter-access/hooks/useRecruiterAccess'
import { AppNavbar } from '../shared/ui/AppNavbar'
import { ErrorBanner } from '../shared/ui/ErrorBanner'
import { useThemeMode } from './hooks/useThemeMode'

function PortfolioAppContent() {
  const [connectionError, setConnectionError] = useState<string | null>(null)
  const { themeMode, toggleTheme } = useThemeMode()
  const { sessionId, messages, isLoading, createSession, sendMessage, clearConversation } = useChatSession(setConnectionError)
  const { accessState, accessStatusMessage, logout } = useRecruiterAccess(setConnectionError)

  useEffect(() => {
    if (!accessState.isChecking && accessState.authenticated && !sessionId) {
      void createSession()
    }
  }, [accessState.authenticated, accessState.isChecking, createSession, sessionId])

  const handleLogout = async () => {
    try {
      await logout()
    } catch (error) {
      setConnectionError(error instanceof Error ? error.message : 'Erreur inconnue')
    } finally {
      clearConversation()
    }
  }

  const showAccessGate = accessState.enabled && !accessState.authenticated
  const accessExpiryLabel = useMemo(() => {
    if (!accessState.expiresAt) {
      return null
    }

    return new Date(accessState.expiresAt).toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }, [accessState.expiresAt])

  let mainContent: ReactNode

  if (accessState.isChecking) {
    mainContent = (
      <div className="min-h-[calc(100vh-140px)] flex items-center justify-center">
        <div className="theme-panel rounded-3xl border px-8 py-10 text-center max-w-md w-full">
          <div className="theme-badge mx-auto mb-4 w-14 h-14 rounded-2xl border flex items-center justify-center">
            <LockKeyhole className="w-7 h-7" />
          </div>
          <h2 className="theme-text-primary text-xl font-semibold mb-2">Vérification de votre accès</h2>
          <p className="theme-text-muted">Nous validons votre session recruteur en cours.</p>
        </div>
      </div>
    )
  } else if (showAccessGate) {
    mainContent = (
      <AccessGate
        accessState={accessState}
        accessStatusMessage={accessStatusMessage}
        onError={setConnectionError}
      />
    )
  } else {
    mainContent = (
      <>
        <PortfolioLanding
          accessExpiryLabel={accessExpiryLabel}
          messages={messages}
          isLoading={isLoading}
          onSendMessage={sendMessage}
          onNewSession={createSession}
        />
        <ContactSection />
      </>
    )
  }

  return (
    <div className="app-shell min-h-screen font-sans overflow-x-hidden" style={{ WebkitFontSmoothing: 'antialiased' }}>
      <div className="fixed inset-0 z-0 pointer-events-none">
        <div
          className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] rounded-full blur-[120px] animate-pulse"
          style={{ animationDuration: '4s', background: 'var(--orb-a)' }}
        />
        <div
          className="absolute bottom-[-20%] right-[-10%] w-[50%] h-[50%] rounded-full blur-[120px] animate-pulse"
          style={{ animationDuration: '7s', background: 'var(--orb-b)' }}
        />
        <div className="absolute top-[40%] left-[30%] w-[30%] h-[30%] rounded-full blur-[100px]" style={{ background: 'var(--orb-c)' }} />
      </div>

      <AppNavbar
        themeMode={themeMode}
        onToggleTheme={toggleTheme}
        canLogout={accessState.enabled && accessState.authenticated}
        onLogout={handleLogout}
      />

      <ErrorBanner message={connectionError} onDismiss={() => setConnectionError(null)} />

      <main className="relative z-10 pt-20 pb-12 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
        {mainContent}
      </main>
    </div>
  )
}

function AdminAppContent() {
  const { themeMode, toggleTheme } = useThemeMode()

  return (
    <div className="app-shell min-h-screen font-sans overflow-x-hidden" style={{ WebkitFontSmoothing: 'antialiased' }}>
      <div className="fixed inset-0 z-0 pointer-events-none">
        <div
          className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] rounded-full blur-[120px] animate-pulse"
          style={{ animationDuration: '4s', background: 'var(--orb-a)' }}
        />
        <div
          className="absolute bottom-[-20%] right-[-10%] w-[50%] h-[50%] rounded-full blur-[120px] animate-pulse"
          style={{ animationDuration: '7s', background: 'var(--orb-b)' }}
        />
        <div className="absolute top-[40%] left-[30%] w-[30%] h-[30%] rounded-full blur-[100px]" style={{ background: 'var(--orb-c)' }} />
      </div>

      <AppNavbar
        themeMode={themeMode}
        onToggleTheme={toggleTheme}
        canLogout={false}
        onLogout={() => {}}
      />

      <main className="relative z-10 pt-20 pb-12 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
        <AdminDocumentsPage />
      </main>
    </div>
  )
}

function App() {
  const isAdminDocumentsRoute = globalThis.location.pathname === '/admin/documents'
  return isAdminDocumentsRoute ? <AdminAppContent /> : <PortfolioAppContent />
}

export default App
