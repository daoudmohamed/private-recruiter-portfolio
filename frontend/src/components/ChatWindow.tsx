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
    "Bonjour ! Je suis l'assistant IA de **Mohamed Daoud**, Tech Lead Backend spécialisé en architectures microservices Java/Spring Boot en environnements bancaires et assurantiels.\n\nJe peux vous parler de son parcours chez Malakoff Humanis, AXA France et EquensWorldline, de ses compétences techniques (Spring Boot 3, Kubernetes, RabbitMQ, OAuth2…) ou de ses certifications AWS & Kubernetes.\n\nQue souhaitez-vous savoir ?",
};

const SUGGESTIONS = [
  'Quel est son poste actuel et ses responsabilités ?',
  'Quelles sont ses compétences en architecture microservices ?',
  'Quelle est son expérience en environnements bancaires ?',
  'Quelles certifications cloud possède-t-il ?',
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

  const showThinking = isLoading && (messages.length === 0 || messages[messages.length - 1]?.content === '');

  return (
    <div className="flex flex-col h-[600px] w-full max-w-4xl mx-auto bg-slate-900/50 backdrop-blur-xl rounded-3xl border border-slate-700/50 shadow-2xl overflow-hidden relative">
      {/* Header */}
      <div className="bg-slate-900/80 p-4 border-b border-slate-700/50 flex items-center justify-between z-10">
        <div className="flex items-center gap-3">
          <div className="w-3 h-3 bg-green-500 rounded-full animate-pulse" />
          <div>
            <h3 className="text-white font-semibold text-sm md:text-base flex items-center gap-2">
              Assistant Virtuel <Sparkles className="w-4 h-4 text-cyan-400" />
            </h3>
            <p className="text-slate-400 text-xs">En ligne | Répond instantanément</p>
          </div>
        </div>
        <button
          onClick={onNewSession}
          className="p-2 hover:bg-slate-800 rounded-full transition-colors text-slate-400 hover:text-white"
          title="Nouvelle session"
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {/* Messages Area */}
      <div
        ref={messagesContainerRef}
        className="flex-1 overflow-y-auto overscroll-y-contain p-4 md:p-6"
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
      <div className="px-4 pb-2 flex gap-2 overflow-x-auto">
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
                className="whitespace-nowrap px-4 py-2 bg-slate-800 hover:bg-slate-700 text-cyan-300 text-xs md:text-sm rounded-full border border-slate-700 transition-all hover:border-cyan-500/50"
              >
                {suggestion}
              </motion.button>
            ))}
        </AnimatePresence>
      </div>

      {/* Input Area */}
      <div className="p-4 bg-slate-900/80 border-t border-slate-700/50 z-10">
        <div className="relative flex items-center gap-2">
          <input
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Posez une question sur vos documents..."
            className="w-full bg-slate-800/50 text-white placeholder-slate-400 rounded-xl pl-4 pr-12 py-3 border border-slate-700 focus:outline-none focus:border-cyan-500/50 focus:ring-1 focus:ring-cyan-500/20 transition-all"
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
          <p className="text-[10px] text-slate-500">
            L'IA peut parfois faire des erreurs. Vérifiez les informations importantes.
          </p>
        </div>
      </div>
    </div>
  );
}

export default ChatWindow;
