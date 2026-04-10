import { useEffect, useState } from 'react'

export type ThemeMode = 'dark' | 'light'

function detectInitialTheme(): ThemeMode {
  const saved = globalThis.localStorage?.getItem('prp-theme')
  if (saved === 'light' || saved === 'dark') {
    return saved
  }

  return globalThis.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
}

export function useThemeMode() {
  const [themeMode, setThemeMode] = useState<ThemeMode>(detectInitialTheme)

  useEffect(() => {
    document.documentElement.classList.toggle('theme-light', themeMode === 'light')
    document.documentElement.style.colorScheme = themeMode
    globalThis.localStorage?.setItem('prp-theme', themeMode)
  }, [themeMode])

  const toggleTheme = () => {
    setThemeMode((current) => (current === 'dark' ? 'light' : 'dark'))
  }

  return {
    themeMode,
    toggleTheme,
  }
}
