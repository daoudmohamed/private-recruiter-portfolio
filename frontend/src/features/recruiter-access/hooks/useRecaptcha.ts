import { useEffect, useMemo, useState } from 'react'

declare global {
  interface Window {
    grecaptcha?: {
      ready: (callback: () => void) => void
      execute: (siteKey: string, options: { action: string }) => Promise<string>
    }
  }
}

export function useRecaptcha(
  showAccessGate: boolean,
  captchaSiteKey: string | null,
  captchaAction: string | null,
) {
  const [captchaReady, setCaptchaReady] = useState(false)
  const requiresCaptcha = useMemo(() => (captchaSiteKey ?? '').length > 0, [captchaSiteKey])
  const action = captchaAction ?? 'request_invitation'

  useEffect(() => {
    if (!showAccessGate || !requiresCaptcha) {
      setCaptchaReady(false)
      return
    }

    const recaptcha = globalThis.grecaptcha
    const existingScript = document.querySelector<HTMLScriptElement>('script[data-recaptcha="true"]')
    if (existingScript) {
      if (recaptcha) {
        recaptcha.ready(() => setCaptchaReady(true))
      }
      return
    }

    const script = document.createElement('script')
    script.src = `https://www.google.com/recaptcha/api.js?render=${encodeURIComponent(captchaSiteKey ?? '')}`
    script.async = true
    script.defer = true
    script.dataset.recaptcha = 'true'
    script.onload = () => {
      if (globalThis.grecaptcha) {
        globalThis.grecaptcha.ready(() => setCaptchaReady(true))
      }
    }
    document.body.appendChild(script)
  }, [captchaSiteKey, requiresCaptcha, showAccessGate])

  const createCaptchaToken = async (): Promise<string | undefined> => {
    if (!requiresCaptcha) {
      return undefined
    }

    if (!globalThis.grecaptcha || !captchaSiteKey) {
      throw new Error('Le service captcha n est pas encore pret. Veuillez reessayer dans quelques secondes.')
    }

    return globalThis.grecaptcha.execute(captchaSiteKey, { action })
  }

  return {
    requiresCaptcha,
    captchaReady,
    createCaptchaToken,
  }
}
