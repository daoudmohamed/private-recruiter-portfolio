import { motion } from 'motion/react';
import { clsx } from 'clsx';
import { User, Bot } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import rehypeSanitize from 'rehype-sanitize';
import type { Message } from '../../utils/security';

interface MessageBubbleProps {
  message: Message;
  isLast?: boolean;
}

export const MessageBubble: React.FC<MessageBubbleProps> = ({ message }) => {
  const isBot = message.role === 'assistant';

  return (
    <motion.div
      initial={{ opacity: 0, y: 10, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ duration: 0.3 }}
      className={clsx(
        'flex w-full mb-4 gap-3',
        isBot ? 'justify-start' : 'justify-end'
      )}
    >
      {isBot && (
        <div className="w-8 h-8 rounded-full bg-gradient-to-br from-cyan-400 to-blue-600 flex items-center justify-center flex-shrink-0 shadow-lg shadow-cyan-500/20 border border-cyan-500/30">
          <Bot className="w-4 h-4 text-white" />
        </div>
      )}

      <div
        className={clsx(
          'max-w-[80%] p-4 rounded-2xl text-sm md:text-base leading-relaxed shadow-sm',
          isBot
            ? clsx(
                'theme-surface backdrop-blur-md theme-text-secondary rounded-tl-none border',
                message.isError && 'theme-danger'
              )
            : 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-tr-none shadow-blue-500/10'
        )}
      >
        {isBot ? (
          <div className="theme-message-markdown max-w-none">
            <ReactMarkdown rehypePlugins={[rehypeSanitize]}>
              {message.content || '...'}
            </ReactMarkdown>
          </div>
        ) : (
          <p className="whitespace-pre-wrap">{message.content}</p>
        )}
      </div>

      {!isBot && (
        <div className="theme-surface-soft w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 border">
          <User className="w-5 h-5 theme-text-muted" />
        </div>
      )}
    </motion.div>
  );
};
