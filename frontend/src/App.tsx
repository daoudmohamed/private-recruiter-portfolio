import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Settings, X, AlertCircle, Database, Github, Linkedin, Mail, Briefcase, Code, Cpu, Globe } from 'lucide-react';
import ChatWindow from './components/ChatWindow';
import ErrorBoundary from './components/ErrorBoundary';
import { SkillsSection } from './components/sections/SkillsSection';
import { ExperienceSection } from './components/sections/ExperienceSection';
import { createSession as apiCreateSession, sendChatMessage } from './utils/api';
import {
  getApiKey, setApiKey as storeApiKey,
  getPersistedSession, persistSession,
  getPersistedMessages, persistMessages,
  validateMessage,
  type Message,
} from './utils/security';

function App() {
  const [sessionId, setSessionId] = useState<string | null>(() => getPersistedSession());
  const [messages, setMessages] = useState<Message[]>(() => getPersistedMessages());
  const [isLoading, setIsLoading] = useState(false);
  const [apiKey, setApiKeyState] = useState<string>(() => getApiKey());
  const [showSettings, setShowSettings] = useState(false);
  const [connectionError, setConnectionError] = useState<string | null>(null);

  const handleApiKeyChange = (value: string) => {
    setApiKeyState(value);
    storeApiKey(value);
  };

  const createSession = async (): Promise<string | null> => {
    try {
      setConnectionError(null);
      const session = await apiCreateSession(apiKey);
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

    let fullResponse = '';
    const assistantMessage: Message = { id: Date.now() + 1, role: 'assistant', content: '' };

    try {
      const response = await sendChatMessage(sessionId, message, apiKey);
      const reader = response.body!.getReader();
      const decoder = new TextDecoder();

      setMessages(prev => [...prev, assistantMessage]);

      try {
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          const chunk = decoder.decode(value);
          const lines = chunk.split('\n');

          for (const line of lines) {
            if (line.startsWith('data:')) {
              try {
                const jsonStr = line.substring(5).trim();
                if (!jsonStr) continue;

                const data = JSON.parse(jsonStr);
                const content: string = data.content || data.text || '';

                if (content && data.type !== 'DONE') {
                  fullResponse += content;
                  setMessages(prev => {
                    const updated = [...prev];
                    updated[updated.length - 1] = { ...updated[updated.length - 1], content: fullResponse };
                    return updated;
                  });
                }
              } catch {
                // ignore parse errors for incomplete chunks
              }
            }
          }
        }
      } catch {
        if (fullResponse) {
          fullResponse += '\n\n---\n*Réponse interrompue. Veuillez réessayer.*';
          setMessages(prev => {
            const updated = [...prev];
            updated[updated.length - 1] = { ...updated[updated.length - 1], content: fullResponse };
            return updated;
          });
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
    if (!sessionId) createSession();
  }, []);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-200 font-sans overflow-x-hidden" style={{ WebkitFontSmoothing: 'antialiased' }}>

      {/* Background Effects — exact Untitled */}
      <div className="fixed inset-0 z-0 pointer-events-none">
        <div
          className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] bg-purple-500/20 rounded-full blur-[120px] mix-blend-screen animate-pulse"
          style={{ animationDuration: '4s' }}
        />
        <div
          className="absolute bottom-[-20%] right-[-10%] w-[50%] h-[50%] bg-cyan-500/20 rounded-full blur-[120px] mix-blend-screen animate-pulse"
          style={{ animationDuration: '7s' }}
        />
        <div className="absolute top-[40%] left-[30%] w-[30%] h-[30%] bg-blue-500/10 rounded-full blur-[100px] mix-blend-screen" />
      </div>

      {/* Navigation — exact Untitled */}
      <nav className="fixed top-0 w-full z-50 border-b border-white/5 bg-slate-950/50 backdrop-blur-lg">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-400 to-blue-600 flex items-center justify-center font-bold text-white">
              <Database className="w-4 h-4" />
            </div>
            <span className="font-semibold text-lg tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-white to-slate-400">
              Knowledge Base
            </span>
          </div>

          <div className="flex items-center gap-3">
            <a href="https://github.com/daoudmohamed" target="_blank" rel="noreferrer" className="p-2 hover:bg-white/5 rounded-full transition-colors text-slate-400 hover:text-white" title="GitHub">
              <Github className="w-5 h-5" />
            </a>
            <a href="https://www.linkedin.com/in/daoudmohamed/" target="_blank" rel="noreferrer" className="p-2 hover:bg-white/5 rounded-full transition-colors text-slate-400 hover:text-white" title="LinkedIn">
              <Linkedin className="w-5 h-5" />
            </a>
            <a href="mailto:daoud.mohamed.tn@gmail.com" className="p-2 hover:bg-white/5 rounded-full transition-colors text-slate-400 hover:text-white" title="Email">
              <Mail className="w-5 h-5" />
            </a>
            <a href="/cv_fr.json" download="CV_Mohamed_Daoud.json" className="hidden md:block px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white text-sm font-medium rounded-full border border-slate-700 transition-colors">
              Télécharger CV
            </a>
            <button
              onClick={() => setShowSettings(!showSettings)}
              className="p-2 hover:bg-white/5 rounded-full transition-colors text-slate-400 hover:text-white"
              title="Paramètres"
            >
              <Settings className="w-5 h-5" />
            </button>
          </div>
        </div>
      </nav>

      {/* Settings dropdown */}
      <AnimatePresence>
        {showSettings && (
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            className="fixed top-16 right-4 z-50 w-72 bg-slate-900/90 backdrop-blur-xl border border-slate-700/50 rounded-2xl shadow-xl p-4"
          >
            <div className="flex items-center justify-between mb-3">
              <span className="text-sm font-medium text-slate-200">Paramètres</span>
              <button onClick={() => setShowSettings(false)} className="text-slate-400 hover:text-white transition-colors">
                <X className="w-4 h-4" />
              </button>
            </div>
            <label className="text-xs text-slate-400 block mb-1">Clé API</label>
            <input
              type="password"
              placeholder="Entrez votre clé API..."
              value={apiKey}
              onChange={(e) => handleApiKeyChange(e.target.value)}
              className="w-full bg-slate-800/50 text-white placeholder-slate-500 rounded-xl px-3 py-2 text-sm border border-slate-700 focus:outline-none focus:border-cyan-500/50 transition-all"
            />
          </motion.div>
        )}
      </AnimatePresence>

      {/* Error banner */}
      <AnimatePresence>
        {connectionError && (
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            className="fixed top-16 left-0 right-0 z-40 flex items-center gap-2 px-4 py-2.5 bg-red-950/80 backdrop-blur-md border-b border-red-700/40 text-red-300 text-sm"
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
        <div className="flex flex-col items-center justify-center min-h-[calc(100vh-140px)]">

          {/* Hero */}
          <div className="text-center mb-12 max-w-2xl px-4">
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.5 }}
              className="inline-block mb-4 px-4 py-1.5 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 text-sm font-medium"
            >
              Tech Lead Backend · Banque & Assurance · 8 ans d'XP
            </motion.div>
            <motion.h1
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1, duration: 0.5 }}
              className="text-4xl md:text-6xl font-bold mb-6 bg-clip-text text-transparent bg-gradient-to-r from-white via-slate-200 to-slate-400 tracking-tight"
            >
              Discutez avec le{' '}
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-600">
                Double Numérique
              </span>{' '}
              de Mohamed
            </motion.h1>
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5 }}
              className="text-lg text-slate-400 leading-relaxed"
            >
              Plus besoin de lire un CV statique. Posez vos questions directement à l'IA
              pour découvrir son expérience, ses compétences et ses projets.
            </motion.p>
          </div>

          {/* Chat card */}
          <motion.div
            initial={{ opacity: 0, y: 40 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3, duration: 0.6 }}
            className="w-full max-w-4xl px-2 md:px-0 mb-20 z-10"
          >
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
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 w-full max-w-4xl px-4 md:px-0 mb-32">
            {[
              { icon: Briefcase, label: "Années d'expérience",   value: '8+',  delay: 0.4 },
              { icon: Code,      label: 'Certifications cloud', value: '5',   delay: 0.5 },
              { icon: Cpu,       label: 'Technologies maît.',   value: '20+', delay: 0.6 },
              { icon: Globe,     label: 'Clients grands comptes', value: '4+', delay: 0.7 },
            ].map(({ icon: Icon, label, value, delay }) => (
              <motion.div
                key={label}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay, duration: 0.5 }}
                className="bg-slate-900/40 backdrop-blur-md border border-slate-700/50 p-6 rounded-2xl flex flex-col items-center justify-center text-center group hover:bg-slate-800/60 transition-colors"
              >
                <div className="mb-3 p-3 bg-slate-800 rounded-full group-hover:bg-cyan-500/20 group-hover:text-cyan-400 transition-colors text-slate-400">
                  <Icon className="w-6 h-6" />
                </div>
                <h4 className="text-2xl font-bold text-white mb-1">{value}</h4>
                <p className="text-sm text-slate-400">{label}</p>
              </motion.div>
            ))}
          </div>
        </div>

        {/* Sections Skills + Experience */}
        <div className="w-full bg-slate-950/50 backdrop-blur-sm border-t border-slate-800/50 flex flex-col items-center">
          <SkillsSection />
          <ExperienceSection />
        </div>

        {/* Footer */}
        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="text-center text-slate-500 text-sm pb-8 pt-20"
        >
          <p>© 2026 Mohamed Daoud · Tech Lead Backend · Paris</p>
        </motion.div>
      </main>
    </div>
  );
}

export default App;
