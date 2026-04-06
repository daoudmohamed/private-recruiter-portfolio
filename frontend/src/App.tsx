import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, AlertCircle, Database, Github, Linkedin, Mail, Briefcase, Code, Cpu, Globe, LockKeyhole, LogOut, RefreshCcw, ArrowRight, BadgeCheck, ShieldCheck, MessageSquareMore, Moon, Sun } from 'lucide-react';
import ChatWindow from './components/ChatWindow';
import ErrorBoundary from './components/ErrorBoundary';
import { SkillsSection } from './components/sections/SkillsSection';
import { ExperienceSection } from './components/sections/ExperienceSection';
import {
  ApiError,
  consumeRecruiterAccessToken,
  createSession as apiCreateSession,
  getRecruiterAccessSession,
  logoutRecruiterAccess,
  requestRecruiterInvitation,
  sendChatMessage,
} from './utils/api';
import {
  getPersistedSession, persistSession,
  getPersistedMessages, persistMessages,
  clearPersistedConversation,
  validateEmail,
  validateMessage,
  type Message,
} from './utils/security';

declare global {
  interface Window {
    grecaptcha?: {
      ready: (callback: () => void) => void
      execute: (siteKey: string, options: { action: string }) => Promise<string>
    }
  }
}

type AccessState = {
  enabled: boolean;
  authenticated: boolean;
  requestInvitationEnabled: boolean;
  captchaSiteKey: string | null;
  captchaAction: string | null;
  expiresAt: string | null;
  isChecking: boolean;
};

type ThemeMode = 'dark' | 'light';

function App() {
  const recruiterSummary = [
    'Tech Lead Backend oriente Java / Spring Boot, avec un parcours recent en assurance, banque et paiements.',
    '8+ ans d experience sur des SI exposes a la production: microservices, BFF, securite, cloud et supervision.',
    'Experiences recentes chez Malakoff Humanis, AXA France et EquensWorldline sur des sujets a forte contrainte de fiabilite.',
    'Capacite a tenir a la fois l execution, les arbitrages techniques, la qualite logicielle et l accompagnement d equipe.',
  ];
  const credibilitySignals = [
    {
      title: 'Responsabilites recentes',
      text: 'Lead technique backend et mobile, coordination d une equipe pluridisciplinaire, arbitrages techniques et responsabilite de delivery.',
    },
    {
      title: 'Contextes metier',
      text: 'Banque, assurance et paiements, avec des applications en production ou la fiabilite, la securite et la maintenabilite sont determinantes.',
    },
    {
      title: 'Sujets differenciants',
      text: 'Microservices Spring Boot, BFF, securite OAuth2/OIDC, Kubernetes, reduction de dette technique et pilotage de la qualite.',
    },
  ];

  const [sessionId, setSessionId] = useState<string | null>(() => getPersistedSession());
  const [messages, setMessages] = useState<Message[]>(() => getPersistedMessages());
  const [isLoading, setIsLoading] = useState(false);
  const [connectionError, setConnectionError] = useState<string | null>(null);
  const [accessState, setAccessState] = useState<AccessState>({
    enabled: false,
    authenticated: false,
    requestInvitationEnabled: false,
    captchaSiteKey: null,
    captchaAction: null,
    expiresAt: null,
    isChecking: true,
  });
  const [accessStatusMessage, setAccessStatusMessage] = useState<string | null>(null);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRequestMessage, setInviteRequestMessage] = useState<string | null>(null);
  const [isSubmittingInvite, setIsSubmittingInvite] = useState(false);
  const [captchaReady, setCaptchaReady] = useState(false);
  const [themeMode, setThemeMode] = useState<ThemeMode>(() => {
    const saved = window.localStorage.getItem('prp-theme');
    if (saved === 'light' || saved === 'dark') {
      return saved;
    }
    return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  });
  const applyStreamChunk = (
    line: string,
    fullResponseRef: { current: string },
    updateAssistantMessage: (content: string) => void,
  ) => {
    if (!line.startsWith('data:')) return;

    const jsonStr = line.substring(5).trim();
    if (!jsonStr) return;

    const data = JSON.parse(jsonStr);
    const content: string = data.content || data.text || '';

    if (content && data.type !== 'DONE') {
      fullResponseRef.current += content;
      updateAssistantMessage(fullResponseRef.current);
    }
  };

  const refreshRecruiterAccess = async () => {
    const access = await getRecruiterAccessSession();
    setAccessState({
      enabled: access.enabled,
      authenticated: access.authenticated,
      requestInvitationEnabled: access.requestInvitationEnabled,
      captchaSiteKey: access.captchaSiteKey ?? null,
      captchaAction: access.captchaAction ?? null,
      expiresAt: access.expiresAt ?? null,
      isChecking: false,
    });
  };

  const messageForAccessError = (error: unknown): string => {
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

  const createSession = async (): Promise<string | null> => {
    try {
      setConnectionError(null);
      const session = await apiCreateSession();
      setSessionId(session.id);
      persistSession(session.id);
      setMessages([]);
      persistMessages([]);
      return session.id;
    } catch (error) {
      setConnectionError(error instanceof Error ? error.message : 'Erreur inconnue');
      return null;
    }
  };

  const sendMessage = async (message: string) => {
    if (!sessionId) return;

    const { valid, error } = validateMessage(message);
    if (!valid) {
      setConnectionError(error);
      return;
    }

    setIsLoading(true);
    setConnectionError(null);

    const userMessage: Message = { id: Date.now(), role: 'user', content: message };
    const updatedWithUser = [...messages, userMessage];
    setMessages(updatedWithUser);
    persistMessages(updatedWithUser);

    const fullResponseRef = { current: '' };
    const assistantMessage: Message = { id: Date.now() + 1, role: 'assistant', content: '' };

    try {
      const response = await sendChatMessage(sessionId, message);
      const reader = response.body!.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      setMessages(prev => [...prev, assistantMessage]);

      const updateAssistantMessage = (content: string) => {
        setMessages(prev => {
          const updated = [...prev];
          updated[updated.length - 1] = { ...updated[updated.length - 1], content };
          return updated;
        });
      };

      try {
        while (true) {
          const { done, value } = await reader.read();
          buffer += decoder.decode(value ?? new Uint8Array(), { stream: !done });

          const lines = buffer.split('\n');
          buffer = lines.pop() ?? '';

          for (const line of lines) {
            try {
              applyStreamChunk(line, fullResponseRef, updateAssistantMessage);
            } catch {
              // Ignore malformed event lines and keep consuming the stream.
            }
          }

          if (done) break;
        }

        if (buffer.trim()) {
          try {
            applyStreamChunk(buffer, fullResponseRef, updateAssistantMessage);
          } catch {
            // Ignore trailing malformed payload.
          }
        }
      } catch {
        if (fullResponseRef.current) {
          fullResponseRef.current += '\n\n---\n*Réponse interrompue. Veuillez réessayer.*';
          updateAssistantMessage(fullResponseRef.current);
        }
        setConnectionError('Le flux de réponse a été interrompu.');
      }
    } catch (error) {
      setConnectionError(error instanceof Error ? error.message : 'Erreur inconnue');
      setMessages(prev => [...prev, {
        id: Date.now() + 2,
        role: 'assistant',
        content: 'Erreur de connexion. Veuillez relancer le backend.',
        isError: true,
      }]);
    } finally {
      setIsLoading(false);
      setMessages(prev => {
        persistMessages(prev);
        return prev;
      });
    }
  };

  useEffect(() => {
    document.documentElement.classList.toggle('theme-light', themeMode === 'light');
    document.documentElement.style.colorScheme = themeMode;
    window.localStorage.setItem('prp-theme', themeMode);
  }, [themeMode]);

  useEffect(() => {
    const token = new URLSearchParams(window.location.search).get('token');

    const bootstrap = async () => {
      try {
        setConnectionError(null);
        setAccessStatusMessage(null);

        if (token) {
          try {
            const consumed = await consumeRecruiterAccessToken(token);
            window.history.replaceState({}, document.title, window.location.pathname);
            setAccessState({
              enabled: consumed.enabled,
              authenticated: consumed.authenticated,
              requestInvitationEnabled: consumed.requestInvitationEnabled,
              captchaSiteKey: consumed.captchaSiteKey ?? null,
              captchaAction: consumed.captchaAction ?? null,
              expiresAt: consumed.expiresAt ?? null,
              isChecking: false,
            });
          } catch (error) {
            window.history.replaceState({}, document.title, window.location.pathname);
            const session = await getRecruiterAccessSession();
            setAccessState({
              enabled: session.enabled,
              authenticated: session.authenticated,
              requestInvitationEnabled: session.requestInvitationEnabled,
              captchaSiteKey: session.captchaSiteKey ?? null,
              captchaAction: session.captchaAction ?? null,
              expiresAt: session.expiresAt ?? null,
              isChecking: false,
            });
            setAccessStatusMessage(
              session.authenticated
                ? 'Votre acces est deja actif sur ce navigateur.'
                : messageForAccessError(error),
            );
          }
          return;
        }

        await refreshRecruiterAccess();
      } catch (error) {
        setAccessState(prev => ({
          ...prev,
          enabled: true,
          authenticated: false,
          captchaAction: prev.captchaAction,
          isChecking: false,
        }));
        setConnectionError(error instanceof Error ? error.message : 'Erreur inconnue');
      }
    };

    bootstrap();
  }, []);

  useEffect(() => {
    if (!accessState.isChecking && accessState.authenticated && !sessionId) {
      createSession();
    }
  }, [accessState.isChecking, accessState.authenticated, sessionId]);

  const handleLogout = async () => {
    try {
      await logoutRecruiterAccess();
    } finally {
      clearPersistedConversation();
      setSessionId(null);
      setMessages([]);
      setAccessState({
        enabled: true,
        authenticated: false,
        requestInvitationEnabled: accessState.requestInvitationEnabled,
        captchaSiteKey: accessState.captchaSiteKey,
        captchaAction: accessState.captchaAction,
        expiresAt: null,
        isChecking: false,
      });
    }
  };

  const showAccessGate = accessState.enabled && !accessState.authenticated;
  const requiresCaptcha = (accessState.captchaSiteKey ?? '').length > 0;
  const captchaAction = accessState.captchaAction ?? 'request_invitation';
  const accessExpiryLabel = accessState.expiresAt
    ? new Date(accessState.expiresAt).toLocaleString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      })
    : null;

  const toggleTheme = () => {
    setThemeMode((current) => (current === 'dark' ? 'light' : 'dark'));
  };

  useEffect(() => {
    if (!showAccessGate || !requiresCaptcha) {
      setCaptchaReady(false);
      return;
    }

    const recaptcha = globalThis.grecaptcha;

    const existingScript = document.querySelector<HTMLScriptElement>('script[data-recaptcha="true"]');
    if (existingScript) {
      if (recaptcha) {
        recaptcha.ready(() => setCaptchaReady(true));
      }
      return;
    }

    const script = document.createElement('script');
    script.src = `https://www.google.com/recaptcha/api.js?render=${encodeURIComponent(accessState.captchaSiteKey ?? '')}`;
    script.async = true;
    script.defer = true;
    script.dataset.recaptcha = 'true';
    script.onload = () => {
      if (globalThis.grecaptcha) {
        globalThis.grecaptcha.ready(() => setCaptchaReady(true));
      }
    };
    document.body.appendChild(script);
  }, [showAccessGate, requiresCaptcha, accessState.captchaSiteKey]);

  const createCaptchaToken = async (): Promise<string | undefined> => {
    if (!requiresCaptcha) {
      return undefined;
    }

    if (!globalThis.grecaptcha || !accessState.captchaSiteKey) {
      throw new Error('Le service captcha n est pas encore pret. Veuillez reessayer dans quelques secondes.');
    }

    return globalThis.grecaptcha.execute(accessState.captchaSiteKey, { action: captchaAction });
  };

  const handleInvitationRequest = async () => {
    const { valid, error } = validateEmail(inviteEmail);
    if (!valid) {
      setConnectionError(error);
      return;
    }

    if (requiresCaptcha && !captchaReady) {
      setConnectionError('Le captcha est en cours de chargement. Veuillez reessayer dans quelques secondes.');
      return;
    }

    setIsSubmittingInvite(true);
    setConnectionError(null);
    setAccessStatusMessage(null);

    try {
      const captchaToken = await createCaptchaToken();
      const result = await requestRecruiterInvitation(inviteEmail.trim(), captchaToken);
      setInviteRequestMessage(result.message);
      setInviteEmail('');
    } catch (error) {
      setConnectionError(error instanceof Error ? error.message : 'Erreur inconnue');
    } finally {
      setIsSubmittingInvite(false);
    }
  };

  return (
    <div className="app-shell min-h-screen font-sans overflow-x-hidden" style={{ WebkitFontSmoothing: 'antialiased' }}>

      {/* Background Effects — exact Untitled */}
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

      {/* Navigation — exact Untitled */}
      <nav className="theme-nav fixed top-0 w-full z-50 border-b backdrop-blur-lg">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 min-h-16 py-2 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-400 to-blue-600 flex items-center justify-center font-bold text-white">
              <Database className="w-4 h-4" />
            </div>
            <div>
              <div className="theme-brand-gradient font-semibold text-sm md:text-base tracking-tight bg-clip-text text-transparent">
                Mohamed Daoud
              </div>
              <div className="theme-text-subtle text-[10px] uppercase tracking-[0.18em]">
                CV interactif prive
              </div>
            </div>
          </div>

          <div className="flex items-center gap-1 sm:gap-3">
            <button
              onClick={toggleTheme}
              className="theme-icon-button p-2 rounded-full transition-colors"
              title={themeMode === 'dark' ? 'Activer le mode clair' : 'Activer le mode sombre'}
            >
              {themeMode === 'dark' ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
            </button>
            <a href="https://github.com/daoudmohamed" target="_blank" rel="noreferrer" className="theme-icon-button p-2 rounded-full transition-colors" title="GitHub">
              <Github className="w-5 h-5" />
            </a>
            <a href="https://www.linkedin.com/in/daoudmohamed/" target="_blank" rel="noreferrer" className="theme-icon-button p-2 rounded-full transition-colors" title="LinkedIn">
              <Linkedin className="w-5 h-5" />
            </a>
            <a href="mailto:daoud.mohamed.tn@gmail.com" className="theme-icon-button p-2 rounded-full transition-colors" title="Email">
              <Mail className="w-5 h-5" />
            </a>
            {accessState.enabled && accessState.authenticated && (
              <button
                onClick={handleLogout}
                className="theme-icon-button p-2 rounded-full transition-colors"
                title="Se déconnecter"
              >
                <LogOut className="w-5 h-5" />
              </button>
            )}
          </div>
        </div>
      </nav>

      {/* Error banner */}
      <AnimatePresence>
        {connectionError && (
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            className="theme-danger fixed top-16 left-0 right-0 z-40 flex items-center gap-2 px-4 py-2.5 backdrop-blur-md border-b text-sm"
            role="alert"
          >
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span className="flex-1">{connectionError}</span>
            <button
              onClick={() => setConnectionError(null)}
              className="p-0.5 rounded hover:text-red-100 transition-colors"
              aria-label="Fermer"
            >
              <X className="w-4 h-4" />
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Main content — exact Untitled layout */}
      <main className="relative z-10 pt-20 pb-12 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
        {accessState.isChecking ? (
          <div className="min-h-[calc(100vh-140px)] flex items-center justify-center">
            <div className="theme-panel rounded-3xl border px-8 py-10 text-center max-w-md w-full">
              <div className="theme-badge mx-auto mb-4 w-14 h-14 rounded-2xl border flex items-center justify-center">
                <LockKeyhole className="w-7 h-7" />
              </div>
              <h2 className="theme-text-primary text-xl font-semibold mb-2">Vérification de votre accès</h2>
              <p className="theme-text-muted">Nous validons votre session recruteur en cours.</p>
            </div>
          </div>
        ) : showAccessGate ? (
          <div className="min-h-[calc(100vh-140px)] flex items-center justify-center">
            <div className="theme-panel rounded-3xl p-5 sm:p-8 max-w-xl w-full text-center shadow-2xl border backdrop-blur-xl">
              <div className="theme-badge mx-auto mb-5 w-16 h-16 rounded-2xl border flex items-center justify-center">
                <LockKeyhole className="w-8 h-8" />
              </div>
              <h1 className="theme-text-primary text-2xl sm:text-3xl font-bold mb-3">Accès recruteur requis</h1>
              <p className="theme-text-muted leading-relaxed mb-6">
                Cet espace prive permet de consulter une version synthetique et interactive du CV de Mohamed Daoud, puis d approfondir rapidement les points utiles avec une IA factuelle.
              </p>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-6 text-left">
                {[
                  {
                    title: 'Lecture rapide',
                    text: 'Accedez rapidement au parcours, a la stack dominante et aux experiences les plus pertinentes pour un poste backend senior ou lead.',
                  },
                  {
                    title: 'Assistant factuel',
                    text: 'Posez des questions precises sur les responsabilites, l architecture, les contextes metier ou les certifications.',
                  },
                  {
                    title: 'Acces temporaire',
                    text: 'Le lien est personnel, prive et limite dans le temps pour garder un cadre de consultation simple et controle.',
                  },
                ].map((item) => (
                  <div key={item.title} className="theme-surface rounded-2xl border p-4">
                    <div className="theme-text-accent text-sm font-medium mb-2">{item.title}</div>
                    <p className="theme-text-muted text-sm leading-relaxed">{item.text}</p>
                  </div>
                ))}
              </div>
              <div className="theme-surface rounded-2xl border p-4 text-left text-sm theme-text-muted mb-6">
                <p className="mb-2">
                  Renseignez votre email professionnel pour recevoir un lien d acces temporaire. Si un acces peut vous etre accorde, vous recevrez un lien prive pour consulter son parcours, ses responsabilites recentes et ses experiences cles.
                </p>
                <p>
                  Votre email est utilise uniquement pour gerer cet acces prive et n est pas destine a des communications marketing.
                </p>
              </div>
              <div className="space-y-4 mb-6">
                <input
                  type="email"
                  value={inviteEmail}
                  onChange={(e) => setInviteEmail(e.target.value)}
                  placeholder="prenom.nom@entreprise.com"
                  className="theme-input w-full rounded-2xl border px-4 py-3 focus:outline-none"
                />
                {requiresCaptcha && accessState.requestInvitationEnabled && (
                  <div className="flex justify-center">
                    {requiresCaptcha && !captchaReady && (
                      <p className="text-xs uppercase tracking-[0.24em] text-[var(--text-muted)]">
                        Initialisation de la protection captcha...
                      </p>
                    )}
                  </div>
                )}
                {inviteRequestMessage && (
                  <div className="theme-success rounded-2xl border px-4 py-3 text-sm">
                    {inviteRequestMessage}
                  </div>
                )}
                {accessStatusMessage && (
                  <div className="theme-warning rounded-2xl border px-4 py-3 text-sm">
                    {accessStatusMessage}
                  </div>
                )}
              </div>
              <div className="flex flex-col sm:flex-row gap-3 justify-center">
                {accessState.requestInvitationEnabled ? (
                  <button
                    onClick={handleInvitationRequest}
                    disabled={isSubmittingInvite}
                    className="btn-theme-primary inline-flex items-center justify-center gap-2 rounded-2xl disabled:opacity-60 disabled:cursor-not-allowed font-medium px-5 py-3 transition-colors"
                  >
                    <Mail className="w-4 h-4" />
                    {isSubmittingInvite ? 'Envoi en cours...' : 'Recevoir mon invitation'}
                  </button>
                ) : (
                  <a
                    href="mailto:daoud.mohamed.tn@gmail.com?subject=Demande%20d%27acces%20recruteur"
                    className="btn-theme-primary inline-flex items-center justify-center gap-2 rounded-2xl font-medium px-5 py-3 transition-colors"
                  >
                    <Mail className="w-4 h-4" />
                    Demander un accès
                  </a>
                )}
                <button
                  onClick={() => window.location.reload()}
                  className="btn-theme-secondary inline-flex items-center justify-center gap-2 rounded-2xl border px-5 py-3 transition-colors"
                >
                  <RefreshCcw className="w-4 h-4" />
                  Réessayer
                </button>
              </div>
              <p className="theme-text-subtle mt-5 text-xs">
                En cas de problème, vous pouvez aussi écrire à{' '}
                <a href="mailto:daoud.mohamed.tn@gmail.com" className="theme-text-secondary transition-colors hover:opacity-80">
                  daoud.mohamed.tn@gmail.com
                </a>.
              </p>
              <p className="theme-text-subtle mt-4 text-xs leading-relaxed">
                En soumettant votre email, vous l utilisez uniquement pour envoyer et gerer votre lien d acces temporaire.
                Les invitations et sessions expirent automatiquement. Les journaux de securite sont conserves de maniere limitee.
                Des sous-traitants techniques peuvent etre mobilises pour l email et, le cas echeant, la protection anti-bot.
              </p>
            </div>
          </div>
        ) : (
        <>
        <div className="flex flex-col items-center justify-center min-h-[calc(100vh-140px)]">

          {/* Hero */}
          <div className="text-center mb-8 md:mb-10 max-w-4xl px-2 sm:px-4">
            {accessExpiryLabel && (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.4 }}
                className="theme-surface rounded-full border inline-block mb-3 px-4 py-1.5 text-sm theme-text-secondary"
              >
                Accès actif jusqu'au {accessExpiryLabel}
              </motion.div>
            )}
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.5 }}
              className="theme-badge inline-block mb-4 px-4 py-1.5 rounded-full border text-sm font-medium"
            >
              Tech Lead Backend · Banque & Assurance · 8 ans d'XP
            </motion.div>
            <motion.h1
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1, duration: 0.5 }}
              className="theme-brand-gradient text-3xl sm:text-4xl md:text-6xl font-bold mb-5 md:mb-6 bg-clip-text text-transparent tracking-tight"
            >
              CV interactif prive pour{' '}
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-600">
                evaluer rapidement
              </span>{' '}
              le profil de Mohamed
            </motion.h1>
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5 }}
              className="theme-text-muted text-base md:text-lg leading-relaxed max-w-3xl mx-auto"
            >
              En quelques minutes, identifiez le niveau, le contexte metier, la stack dominante et les responsabilites recentes. Le chat sert ensuite a verifier ou approfondir un point precis du parcours.
            </motion.p>
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3, duration: 0.5 }}
              className="mt-6 md:mt-8 flex flex-col sm:flex-row gap-3 justify-center"
            >
              <a
                href="#experiences"
                className="btn-theme-secondary inline-flex items-center justify-center gap-2 rounded-2xl border px-5 py-3 transition-colors"
              >
                <ArrowRight className="w-4 h-4 text-cyan-400" />
                Voir les experiences
              </a>
              <a
                href="mailto:daoud.mohamed.tn@gmail.com?subject=Echange%20sur%20votre%20profil"
                className="btn-theme-primary inline-flex items-center justify-center gap-2 rounded-2xl font-semibold px-5 py-3 transition-colors"
              >
                <Mail className="w-4 h-4" />
                Contacter Mohamed
              </a>
            </motion.div>
          </div>

          {/* Recruiter summary */}
          <motion.div
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.35, duration: 0.5 }}
            className="w-full max-w-5xl px-0 md:px-0 mb-10 md:mb-12"
          >
            <div className="grid grid-cols-1 lg:grid-cols-[1.3fr_0.7fr] gap-4 md:gap-6">
              <div className="theme-panel rounded-3xl p-5 sm:p-6 md:p-8 border backdrop-blur-md">
                <div className="theme-text-accent flex items-center gap-2 text-sm font-medium mb-4">
                  <BadgeCheck className="w-4 h-4" />
                  Resume recruteur en 30 secondes
                </div>
                <h2 className="theme-text-primary text-xl sm:text-2xl md:text-3xl font-bold mb-4">
                  Profil backend senior, entre delivery, architecture et responsabilite technique
                </h2>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {recruiterSummary.map((item) => (
                    <div key={item} className="theme-surface rounded-2xl border px-4 py-3 text-sm theme-text-secondary leading-relaxed">
                      {item}
                    </div>
                  ))}
                </div>
              </div>

              <div className="theme-panel rounded-3xl p-5 sm:p-6 md:p-8 border backdrop-blur-md">
                <div className="theme-text-accent text-sm font-medium mb-4">Points de repere</div>
                <div className="space-y-4">
                  {[
                    { label: 'Poste recent', value: 'Tech Lead Backend & Mobile' },
                    { label: 'Secteurs', value: 'Banque, assurance, paiements' },
                    { label: 'Stack dominante', value: 'Java, Spring Boot, Kotlin, Kubernetes' },
                    { label: 'Differenciateurs', value: 'Microservices, securite, delivery, leadership technique' },
                  ].map((item) => (
                    <div key={item.label} className="border-b pb-4 last:border-b-0 last:pb-0" style={{ borderColor: 'var(--border)' }}>
                      <div className="theme-text-subtle text-xs uppercase tracking-[0.14em] mb-1">{item.label}</div>
                      <div className="theme-text-secondary text-sm leading-relaxed">{item.value}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.37, duration: 0.5 }}
            className="w-full max-w-5xl px-0 md:px-0 mb-8"
          >
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              {[
                { label: "Experience", value: '8+ ans', hint: 'Backend, mobile, delivery' },
                { label: 'Secteurs', value: '3 domaines', hint: 'Banque, assurance, paiements' },
                { label: 'Role recent', value: 'Tech Lead', hint: 'Backend et mobile' },
                { label: 'Valeur rapide', value: 'CV lisible', hint: 'Chat pour approfondir' },
              ].map((item) => (
                <div key={item.label} className="theme-panel-soft rounded-2xl border px-4 py-4 text-left min-h-[112px]">
                  <div className="theme-text-subtle text-[11px] uppercase tracking-[0.14em] mb-1">{item.label}</div>
                  <div className="theme-text-primary text-xl font-semibold mb-1">{item.value}</div>
                  <div className="theme-text-muted text-sm leading-relaxed">{item.hint}</div>
                </div>
              ))}
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.38, duration: 0.5 }}
            className="w-full max-w-5xl px-0 md:px-0 mb-8 md:mb-10"
            id="preuves"
          >
            <div className="theme-panel rounded-3xl border backdrop-blur-md p-5 sm:p-6 md:p-8">
              <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4 mb-6">
                <div>
                  <div className="theme-text-accent inline-flex items-center gap-2 text-sm font-medium mb-3">
                    <ShieldCheck className="w-4 h-4" />
                    Pourquoi ce profil est pertinent
                  </div>
                  <h2 className="theme-text-primary text-xl sm:text-2xl md:text-3xl font-bold mb-2">
                    Un profil utile quand il faut tenir la technique sans perdre la livraison
                  </h2>
                  <p className="theme-text-muted max-w-3xl leading-relaxed">
                    Le site met en avant les signaux utiles pour un recruteur: niveau technique, contextes metier, type de responsabilites et capacite a faire avancer des sujets critiques en production.
                  </p>
                </div>
                <a
                  href="mailto:daoud.mohamed.tn@gmail.com?subject=Echange%20sur%20un%20poste%20Backend%20Lead"
                  className="btn-theme-secondary inline-flex items-center gap-2 rounded-2xl border px-4 py-3 text-sm font-medium transition-colors"
                >
                  <ArrowRight className="w-4 h-4 text-cyan-400" />
                  Ouvrir un echange rapide
                </a>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3 md:gap-4">
                {credibilitySignals.map((item) => (
                  <div key={item.title} className="theme-surface rounded-2xl border p-5">
                    <div className="theme-text-accent text-sm font-medium mb-2">{item.title}</div>
                    <p className="theme-text-secondary text-sm leading-relaxed">{item.text}</p>
                  </div>
                ))}
              </div>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.39, duration: 0.45 }}
            className="w-full max-w-5xl px-0 md:px-0 mb-8"
          >
            <div className="theme-panel-soft rounded-3xl border px-4 py-4 sm:px-5 sm:py-5 md:px-6 md:py-6">
              <div className="grid grid-cols-1 md:grid-cols-[0.9fr_1.1fr] gap-4 items-start">
                <div>
                  <div className="theme-text-accent text-sm font-medium mb-2">Avant d utiliser le chat</div>
                  <h3 className="theme-text-primary text-xl font-semibold mb-2">L essentiel du profil est deja visible plus haut</h3>
                  <p className="theme-text-muted text-sm leading-relaxed">
                    Le chat est surtout utile pour verifier un point cible: contexte metier, responsabilites, niveau sur Spring Boot, microservices ou certifications.
                  </p>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {[
                    'Verifier une experience recente',
                    'Comparer le profil a un poste cible',
                    'Creuser la stack ou l architecture',
                    'Obtenir un resume factuel en quelques lignes',
                  ].map((item) => (
                    <div key={item} className="theme-surface rounded-2xl border px-4 py-3 text-sm theme-text-secondary">
                      {item}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </motion.div>

          {/* Chat card */}
          <motion.div
            initial={{ opacity: 0, y: 40 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3, duration: 0.6 }}
            className="w-full max-w-3xl px-0 md:px-0 mb-14 md:mb-16 z-10"
          >
            <div className="theme-panel-soft mb-4 rounded-3xl border backdrop-blur-sm px-5 py-4 text-left">
              <div className="theme-text-accent text-sm font-medium mb-1">Assistant de lecture du CV</div>
              <p className="theme-text-muted text-sm leading-relaxed">
                Utilisez le chat pour approfondir un point precis du profil: experience bancaire, architecture microservices, stack Spring Boot, responsabilites recentes ou certifications.
              </p>
            </div>
            <ErrorBoundary>
              <ChatWindow
                messages={messages}
                isLoading={isLoading}
                onSendMessage={sendMessage}
                onNewSession={createSession}
              />
            </ErrorBoundary>
          </motion.div>

          {/* Stats Grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 md:gap-4 w-full max-w-4xl px-0 md:px-0 mb-20 md:mb-24">
            {[
              { icon: Briefcase, label: "Annees d'experience", value: '8+', delay: 0.4 },
              { icon: Code, label: 'Certifications', value: '5', delay: 0.5 },
              { icon: Cpu, label: 'Technologies clees', value: '20+', delay: 0.6 },
              { icon: Globe, label: 'Contextes critiques', value: '4+', delay: 0.7 },
            ].map(({ icon: Icon, label, value, delay }) => (
              <motion.div
                key={label}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay, duration: 0.5 }}
                className="theme-panel-soft backdrop-blur-md border p-6 rounded-2xl flex flex-col items-center justify-center text-center group transition-colors"
              >
                <div className="theme-surface mb-3 p-3 rounded-full group-hover:text-cyan-400 transition-colors theme-text-muted border">
                  <Icon className="w-6 h-6" />
                </div>
                <h4 className="theme-text-primary text-2xl font-bold mb-1">{value}</h4>
                <p className="theme-text-muted text-sm">{label}</p>
              </motion.div>
            ))}
          </div>
        </div>

        {/* Sections Skills + Experience */}
        <div id="experiences" className="w-full border-t flex flex-col items-center scroll-mt-24" style={{ background: 'color-mix(in srgb, var(--panel-soft) 92%, transparent)', borderColor: 'var(--border)' }}>
          <section className="w-full max-w-6xl mx-auto px-4 pt-16 md:pt-20">
            <div className="theme-panel rounded-3xl border backdrop-blur-md p-5 sm:p-6 md:p-8">
              <div className="max-w-3xl mb-8">
                <div className="theme-text-accent text-sm font-medium mb-3">Ce que je peux prendre en charge rapidement</div>
                <h2 className="theme-text-primary text-xl sm:text-2xl md:text-3xl font-bold mb-3">
                  Des sujets backend, lead technique et fiabilite de delivery
                </h2>
                <p className="theme-text-muted leading-relaxed">
                  Cette section aide a faire le lien entre le parcours et un besoin concret. Elle ne remplace pas l echange, mais permet de voir rapidement les types de sujets sur lesquels Mohamed peut etre utile des les premieres semaines.
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {[
                  {
                    title: 'Prendre ou reprendre un backend Spring Boot',
                    text: 'Stabilisation, evolution, reduction de dette technique, cadrage d architecture et remise a niveau de la qualite.',
                  },
                  {
                    title: 'Tenir un role de Tech Lead Backend',
                    text: 'Arbitrages techniques, accompagnement d equipe, coordination delivery et mise en place de standards pragmatiques.',
                  },
                  {
                    title: 'Intervenir sur des contextes regulés ou sensibles',
                    text: 'Banque, assurance, paiements ou SI critiques avec exigences de securite, fiabilite et maintenabilite.',
                  },
                  {
                    title: 'Aider sur la production et la robustesse',
                    text: 'Incidents, observabilite, supervision, BFF, microservices, CI/CD et hygiene technique.',
                  },
                ].map((item) => (
                  <div key={item.title} className="theme-surface rounded-2xl border p-5">
                    <div className="theme-text-primary text-lg font-semibold mb-2">{item.title}</div>
                    <p className="theme-text-muted text-sm leading-relaxed">{item.text}</p>
                  </div>
                ))}
              </div>
            </div>
          </section>

          <section className="w-full max-w-6xl mx-auto px-4 pt-6 md:pt-8">
            <div className="theme-panel-soft rounded-3xl border backdrop-blur-md p-5 sm:p-6 md:p-8">
              <div className="max-w-3xl mb-8">
                <div className="theme-text-accent text-sm font-medium mb-3">Postes cibles les plus naturels</div>
                <h2 className="theme-text-primary text-xl sm:text-2xl md:text-3xl font-bold mb-3">
                  Les contextes ou le profil est le plus lisible et le plus coherent
                </h2>
                <p className="theme-text-muted leading-relaxed">
                  Ce repere n enferme pas le profil, mais il aide a comprendre plus vite dans quels cadres l experience et les responsabilites recentes sont le plus directement transposables.
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {[
                  {
                    title: 'Tech Lead Backend',
                    text: 'Pour tenir un role melant execution, arbitrages techniques, cadrage d architecture et accompagnement d equipe.',
                  },
                  {
                    title: 'Senior Backend Engineer',
                    text: 'Pour reprendre ou faire evoluer un backend Spring Boot en contexte de production avec exigence de qualite.',
                  },
                  {
                    title: 'Lead Engineer / Referent technique',
                    text: 'Pour des equipes qui veulent un profil capable d apporter structure, fiabilite et vision pragmatique sur la delivery.',
                  },
                ].map((item) => (
                  <div key={item.title} className="theme-surface rounded-2xl border p-5">
                    <div className="theme-text-primary text-lg font-semibold mb-2">{item.title}</div>
                    <p className="theme-text-muted text-sm leading-relaxed">{item.text}</p>
                  </div>
                ))}
              </div>
            </div>
          </section>

          <SkillsSection />
          <ExperienceSection />
        </div>

        <section id="contact" className="w-full max-w-5xl mx-auto px-4 md:px-0 pt-16 md:pt-20 scroll-mt-24">
          <div className="theme-panel rounded-3xl border backdrop-blur-md p-5 sm:p-6 md:p-8">
            <div className="grid grid-cols-1 lg:grid-cols-[1.15fr_0.85fr] gap-6">
              <div>
                <div className="theme-text-accent inline-flex items-center gap-2 text-sm font-medium mb-3">
                  <MessageSquareMore className="w-4 h-4" />
                  Passer du portfolio a l echange
                </div>
                <h2 className="theme-text-primary text-2xl md:text-3xl font-bold mb-3">
                  Si le profil semble pertinent, un echange direct sera plus utile qu une lecture plus longue
                </h2>
                <p className="theme-text-muted leading-relaxed mb-5">
                  Le site sert a accelerer la lecture du CV. Si vous souhaitez valider un contexte, discuter d un poste ou confronter le parcours a un besoin reel, le contact direct reste l etape la plus efficace.
                </p>
                <div className="flex flex-col sm:flex-row gap-3">
                  <a
                    href="mailto:daoud.mohamed.tn@gmail.com?subject=Prise%20de%20contact%20suite%20a%20votre%20portfolio"
                    className="btn-theme-primary inline-flex items-center justify-center gap-2 rounded-2xl font-semibold px-5 py-3 transition-colors"
                  >
                    <Mail className="w-4 h-4" />
                    Contacter par email
                  </a>
                  <a
                    href="https://www.linkedin.com/in/daoudmohamed/"
                    target="_blank"
                    rel="noreferrer"
                    className="btn-theme-secondary inline-flex items-center justify-center gap-2 rounded-2xl border px-5 py-3 transition-colors"
                  >
                    <Linkedin className="w-4 h-4" />
                    Ecrire sur LinkedIn
                  </a>
                </div>
              </div>
              <div className="theme-surface rounded-2xl border p-5">
                <div className="theme-text-accent text-sm font-medium mb-4">Quand prendre contact</div>
                <div className="space-y-3 text-sm theme-text-secondary leading-relaxed">
                  <div className="theme-surface-soft rounded-xl border px-4 py-3">
                    Vous cherchez un profil backend senior capable de tenir des sujets critiques en production.
                  </div>
                  <div className="theme-surface-soft rounded-xl border px-4 py-3">
                    Vous voulez valider une experience banque, assurance, paiements ou architecture microservices.
                  </div>
                  <div className="theme-surface-soft rounded-xl border px-4 py-3">
                    Vous souhaitez confronter rapidement le parcours a un poste de Tech Lead, Senior Backend ou Lead Engineer.
                  </div>
                </div>
                <div className="theme-badge mt-5 rounded-xl border px-4 py-3 text-sm theme-text-secondary">
                  Reponse attendue cote Mohamed: echange rapide, retour direct et discussion concrete sur le contexte, le poste et les attentes.
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Footer */}
        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="theme-text-subtle text-center text-sm pb-8 pt-20"
        >
          <p>© 2026 Mohamed Daoud · Tech Lead Backend · Paris</p>
        </motion.div>
        </>
        )}
      </main>
    </div>
  );
}

export default App;
