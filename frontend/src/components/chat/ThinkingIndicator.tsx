import { motion } from 'motion/react';
import { Bot } from 'lucide-react';

export const ThinkingIndicator = () => {
  return (
    <div className="flex w-full mb-4 gap-3 justify-start">
      <div className="w-8 h-8 rounded-full bg-gradient-to-br from-cyan-400 to-blue-600 flex items-center justify-center flex-shrink-0 shadow-lg shadow-cyan-500/20 border border-cyan-500/30">
        <Bot className="w-4 h-4 text-white" />
      </div>
      <div className="bg-slate-800/80 backdrop-blur-md px-4 py-3 rounded-2xl rounded-tl-none border border-slate-700/50 flex items-center gap-1">
        {[0, 1, 2].map((i) => (
          <motion.div
            key={i}
            className="w-2 h-2 bg-cyan-400 rounded-full"
            animate={{ y: [0, -5, 0] }}
            transition={{
              duration: 0.6,
              repeat: Infinity,
              delay: i * 0.2,
              ease: 'easeInOut',
            }}
          />
        ))}
      </div>
    </div>
  );
};
