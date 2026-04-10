import { useState } from 'react'
import { LockKeyhole, Mail, RefreshCcw } from 'lucide-react'
import { requestRecruiterInvitation } from '../../../utils/api'
import { validateEmail } from '../../../utils/security'
import { useRecaptcha } from '../hooks/useRecaptcha'
import type { AccessState } from '../types'

type AccessGateProps = Readonly<{
  accessState: AccessState
  accessStatusMessage: string | null
  onError: (message: string | null) => void
}>

export function AccessGate({ accessState, accessStatusMessage, onError }: AccessGateProps) {
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRequestMessage, setInviteRequestMessage] = useState<string | null>(null)
  const [isSubmittingInvite, setIsSubmittingInvite] = useState(false)
  const { requiresCaptcha, captchaReady, createCaptchaToken } = useRecaptcha(
    accessState.enabled && !accessState.authenticated,
    accessState.captchaSiteKey,
    accessState.captchaAction,
  )

  const handleInvitationRequest = async () => {
    const { valid, error } = validateEmail(inviteEmail)
    if (!valid) {
      onError(error)
      return
    }

    if (requiresCaptcha && !captchaReady) {
      onError('Le captcha est en cours de chargement. Veuillez reessayer dans quelques secondes.')
      return
    }

    setIsSubmittingInvite(true)
    onError(null)
    setInviteRequestMessage(null)

    try {
      const captchaToken = await createCaptchaToken()
      const result = await requestRecruiterInvitation(inviteEmail.trim(), captchaToken)
      setInviteRequestMessage(result.message)
      setInviteEmail('')
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Erreur inconnue')
    } finally {
      setIsSubmittingInvite(false)
    }
  }

  return (
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
            onChange={(event) => setInviteEmail(event.target.value)}
            placeholder="prenom.nom@entreprise.com"
            className="theme-input w-full rounded-2xl border px-4 py-3 focus:outline-none"
          />
          {requiresCaptcha && accessState.requestInvitationEnabled && !captchaReady && (
            <div className="flex justify-center">
              <p className="text-xs uppercase tracking-[0.24em] text-[var(--text-muted)]">
                Initialisation de la protection captcha...
              </p>
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
            onClick={() => globalThis.location.reload()}
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
  )
}
