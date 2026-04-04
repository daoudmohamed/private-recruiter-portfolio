import { useState, useRef, useEffect } from 'react';
import { Send, Sparkles, RefreshCw } from 'lucide-react';
import { MessageBubble } from './chat/MessageBubble';
import { ThinkingIndicator } from './chat/ThinkingIndicator';
import { motion, AnimatePresence } from 'motion/react';
import { validateMessage, type Message } from '../utils/security';

const INITIAL_MESSAGE: Message = {
  id: 0,
  role: 'assistant',
  content:
    "Je peux vous aider a verifier rapidement le profil de **Mohamed Daoud**.\n\nJe reponds de facon courte et factuelle sur son parcours, sa stack, ses contextes banque / assurance, ses responsabilites recentes et ses certifications.\n\nCommencez par une question precise ou utilisez une suggestion ci-dessous.",
};

const SUGGESTIONS = [
  'Quel est son poste actuel et ses responsabilites ?',
  'Quelle est son experience en banque, assurance et paiements ?',
  'Quel est son niveau sur Spring Boot et les microservices ?',
  'Quelles certifications possede-t-il ?',
  'Pourquoi ce profil est-il pertinent pour un poste de Tech Lead Backend ?',
  'Peux-tu me resumer son parcours en 5 lignes ?',
  'Quels points verifier avant un premier entretien ?',
  'Quels sujets peut-il prendre en charge rapidement ?',
];

interface Props {
  messages: Message[];
  isLoading: boolean;
  onSendMessage: (message: string) => Promise<void>;
  onNewSession: () => void;
}

function ChatWindow({ messages, isLoading, onSendMessage, onNewSession }: Props) {
  const [inputValue, setInputValue] = useState('');
  const messagesContainerRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    if (messagesContainerRef.current) {
      const { scrollHeight, clientHeight } = messagesContainerRef.current;
      messagesContainerRef.current.scrollTo({
        top: scrollHeight - clientHeight,
        behavior: 'smooth',
      });
    }
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading]);

  const handleSend = async (text: string) => {
    if (!text.trim()) return;
    const { valid } = validateMessage(text);
    if (!valid || isLoading) return;
    setInputValue('');
    await onSendMessage(text);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend(inputValue);
    }
  };

  const showThinking = isLoading;

  return (
    <div className="theme-panel flex flex-col h-[540px] sm:h-[560px] md:h-[600px] w-full max-w-4xl mx-auto rounded-3xl border shadow-2xl overflow-hidden relative backdrop-blur-xl">
      {/* Header */}
      <div className="theme-panel-strong p-3 sm:p-4 border-b flex items-center justify-between z-10 gap-3" style={{ borderColor: 'var(--border)' }}>
        <div className="flex items-center gap-3">
          <div className="w-3 h-3 bg-green-500 rounded-full animate-pulse" />
          <div>
            <h3 className="theme-text-primary font-semibold text-sm md:text-base flex items-center gap-2">
              Assistant de lecture <Sparkles className="w-4 h-4 text-cyan-400" />
            </h3>
            <p className="theme-text-muted text-xs">
              {isLoading ? 'En train de repondre...' : 'Questions precises, reponses courtes et factuelles'}
            </p>
          </div>
        </div>
        <button
          onClick={onNewSession}
          className="theme-icon-button p-2 rounded-full transition-colors"
          title="Nouvelle session"
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {/* Messages Area */}
      <div
        ref={messagesContainerRef}
        className="flex-1 overflow-y-auto overscroll-y-contain p-3 sm:p-4 md:p-6"
      >
        {/* Initial greeting — always shown, not persisted */}
        <MessageBubble message={INITIAL_MESSAGE} />

        {messages.map((msg, idx) => (
          <MessageBubble
            key={msg.id}
            message={msg}
            isLast={idx === messages.length - 1}
          />
        ))}
        {showThinking && <ThinkingIndicator />}
        <div />
      </div>

      {/* Suggestions */}
      <div className="px-3 sm:px-4 pt-1">
        <div className="theme-text-subtle text-[11px] uppercase tracking-[0.14em] mb-2">
          Questions utiles
        </div>
      </div>
      <div className="px-3 sm:px-4 pb-2 flex gap-2 overflow-x-auto">
        <AnimatePresence>
          {!isLoading && messages.length < 10 &&
            SUGGESTIONS.map((suggestion, index) => (
              <motion.button
                key={index}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.9 }}
                transition={{ delay: index * 0.1 }}
                onClick={() => handleSend(suggestion)}
                className="theme-surface whitespace-nowrap px-4 py-2 theme-text-accent text-xs md:text-sm rounded-full border transition-all hover:border-cyan-500/50"
              >
                {suggestion}
              </motion.button>
            ))}
        </AnimatePresence>
      </div>

      {/* Input Area */}
      <div className="theme-panel-strong p-3 sm:p-4 border-t z-10" style={{ borderColor: 'var(--border)' }}>
        {!isLoading && messages.length === 0 && (
          <div className="theme-surface mb-3 rounded-2xl border px-4 py-3 text-xs theme-text-muted leading-relaxed">
            Bon usage: demandez un resume, un contexte metier, un niveau sur une stack ou la pertinence du profil pour un type de poste.
          </div>
        )}
        <div className="relative flex items-center gap-2">
          <input
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ex. Pourquoi ce profil est-il pertinent pour un poste backend senior ?"
            className="theme-input w-full rounded-xl pl-4 pr-12 py-3 border focus:outline-none transition-all"
            disabled={isLoading}
          />
          <button
            onClick={() => handleSend(inputValue)}
            disabled={!inputValue.trim() || isLoading}
            className="absolute right-2 p-2 bg-gradient-to-r from-cyan-500 to-blue-600 rounded-lg text-white disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-lg hover:shadow-cyan-500/25 transition-all"
          >
            <Send className="w-4 h-4" />
          </button>
        </div>
        <div className="text-center mt-2">
          <p className="theme-text-subtle text-[10px]">
            L assistant repond a partir du profil et des documents disponibles. Utilisez-le pour verifier un point precis, pas pour remplacer la lecture d ensemble.
          </p>
          <p className="theme-text-subtle text-[10px] mt-1">
            Questions courtes recommandees pour obtenir un resume plus utile et moins couteux.
          </p>
        </div>
      </div>
    </div>
  );
}

export default ChatWindow;
