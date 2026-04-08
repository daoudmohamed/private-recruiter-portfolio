import { Github, Linkedin, LogOut, Mail, Moon, Sun } from 'lucide-react'
import type { ThemeMode } from '../../app/hooks/useThemeMode'

type AppNavbarProps = {
  themeMode: ThemeMode
  onToggleTheme: () => void
  canLogout: boolean
  onLogout: () => void
}

export function AppNavbar({ themeMode, onToggleTheme, canLogout, onLogout }: AppNavbarProps) {
  return (
    <nav className="theme-nav fixed top-0 w-full z-50 border-b backdrop-blur-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 min-h-16 py-2 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <img
            src="/brand-mark.svg"
            alt="Monogramme Mohamed Daoud"
            className="w-8 h-8 rounded-lg shadow-[0_10px_30px_rgba(8,145,178,0.28)]"
          />
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
            onClick={onToggleTheme}
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
          {canLogout && (
            <button
              onClick={onLogout}
              className="theme-icon-button p-2 rounded-full transition-colors"
              title="Se déconnecter"
            >
              <LogOut className="w-5 h-5" />
            </button>
          )}
        </div>
      </div>
    </nav>
  )
}
