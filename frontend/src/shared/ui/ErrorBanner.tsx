import { AnimatePresence, motion } from 'motion/react'
import { AlertCircle, X } from 'lucide-react'

type ErrorBannerProps = {
  message: string | null
  onDismiss: () => void
}

export function ErrorBanner({ message, onDismiss }: ErrorBannerProps) {
  return (
    <AnimatePresence>
      {message && (
        <motion.div
          initial={{ opacity: 0, y: -8 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -8 }}
          className="theme-danger fixed top-16 left-0 right-0 z-40 flex items-center gap-2 px-4 py-2.5 backdrop-blur-md border-b text-sm"
          role="alert"
        >
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span className="flex-1">{message}</span>
          <button
            onClick={onDismiss}
            className="p-0.5 rounded hover:text-red-100 transition-colors"
            aria-label="Fermer"
          >
            <X className="w-4 h-4" />
          </button>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
