import { motion } from 'motion/react'
import { ArrowRight, BadgeCheck, Briefcase, Code, Cpu, Globe, Mail, ShieldCheck } from 'lucide-react'
import ChatWindow from '../../../components/ChatWindow'
import ErrorBoundary from '../../../components/ErrorBoundary'
import { ExperienceSection } from '../../../components/sections/ExperienceSection'
import { SkillsSection } from '../../../components/sections/SkillsSection'
import type { Message } from '../../../utils/security'
import { credibilitySignals, recruiterSummary } from '../content'

type LabelValueItem = {
  label: string
  value: string
}

type MetricItem = LabelValueItem & {
  hint: string
}

type TitleTextItem = {
  title: string
  text: string
}

const keyReferenceItems: LabelValueItem[] = [
  { label: 'Poste recent', value: 'Tech Lead Backend & Mobile' },
  { label: 'Secteurs', value: 'Banque, assurance, paiements' },
  { label: 'Stack dominante', value: 'Java, Spring Boot, Kotlin, Kubernetes' },
  { label: 'Differenciateurs', value: 'Microservices, securite, delivery, leadership technique' },
]

const overviewMetrics: MetricItem[] = [
  { label: 'Experience', value: '8+ ans', hint: 'Backend, mobile, delivery' },
  { label: 'Secteurs', value: '3 domaines', hint: 'Banque, assurance, paiements' },
  { label: 'Role recent', value: 'Tech Lead', hint: 'Backend et mobile' },
  { label: 'Valeur rapide', value: 'CV lisible', hint: 'Chat pour approfondir' },
]

const chatPreparationItems = [
  'Verifier une experience recente',
  'Comparer le profil a un poste cible',
  'Creuser la stack ou l architecture',
  'Obtenir un resume factuel en quelques lignes',
]

const contributionTopics: TitleTextItem[] = [
  {
    title: 'Prendre ou reprendre un backend Spring Boot',
    text: 'Stabilisation, evolution, reduction de dette technique, cadrage d architecture et remise a niveau de la qualite.',
  },
  {
    title: 'Tenir un role de Tech Lead Backend',
    text: 'Arbitrages techniques, accompagnement d equipe, coordination delivery et mise en place de standards pragmatiques.',
  },
  {
    title: 'Intervenir sur des contextes regulés ou sensibles',
    text: 'Banque, assurance, paiements ou SI critiques avec exigences de securite, fiabilite et maintenabilite.',
  },
  {
    title: 'Aider sur la production et la robustesse',
    text: 'Incidents, observabilite, supervision, BFF, microservices, CI/CD et hygiene technique.',
  },
]

const targetRoleItems: TitleTextItem[] = [
  {
    title: 'Tech Lead Backend',
    text: 'Pour tenir un role melant execution, arbitrages techniques, cadrage d architecture et accompagnement d equipe.',
  },
  {
    title: 'Senior Backend Engineer',
    text: 'Pour reprendre ou faire evoluer un backend Spring Boot en contexte de production avec exigence de qualite.',
  },
  {
    title: 'Lead Engineer / Referent technique',
    text: 'Pour des equipes qui veulent un profil capable d apporter structure, fiabilite et vision pragmatique sur la delivery.',
  },
]

function SurfaceTextCard({ title, text }: TitleTextItem) {
  return (
    <div className="theme-surface rounded-2xl border p-5">
      <div className="theme-text-primary text-lg font-semibold mb-2">{title}</div>
      <p className="theme-text-muted text-sm leading-relaxed">{text}</p>
    </div>
  )
}

function KeyReferenceList({ items }: { items: LabelValueItem[] }) {
  return (
    <div className="space-y-4">
      {items.map((item) => (
        <div key={item.label} className="border-b pb-4 last:border-b-0 last:pb-0" style={{ borderColor: 'var(--border)' }}>
          <div className="theme-text-subtle text-xs uppercase tracking-[0.14em] mb-1">{item.label}</div>
          <div className="theme-text-secondary text-sm leading-relaxed">{item.value}</div>
        </div>
      ))}
    </div>
  )
}

function MetricGrid({ items }: { items: MetricItem[] }) {
  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
      {items.map((item) => (
        <div key={item.label} className="theme-panel-soft rounded-2xl border px-4 py-4 text-left min-h-[112px]">
          <div className="theme-text-subtle text-[11px] uppercase tracking-[0.14em] mb-1">{item.label}</div>
          <div className="theme-text-primary text-xl font-semibold mb-1">{item.value}</div>
          <div className="theme-text-muted text-sm leading-relaxed">{item.hint}</div>
        </div>
      ))}
    </div>
  )
}

function SurfaceTextGrid({ items, columnsClassName }: { items: TitleTextItem[]; columnsClassName: string }) {
  return (
    <div className={columnsClassName}>
      {items.map((item) => (
        <SurfaceTextCard key={item.title} {...item} />
      ))}
    </div>
  )
}

type PortfolioLandingProps = {
  accessExpiryLabel: string | null
  messages: Message[]
  isLoading: boolean
  onSendMessage: (message: string) => Promise<void>
  onNewSession: () => Promise<string | null>
}

export function PortfolioLanding({
  accessExpiryLabel,
  messages,
  isLoading,
  onSendMessage,
  onNewSession,
}: PortfolioLandingProps) {
  return (
    <>
      <div className="flex flex-col items-center justify-center min-h-[calc(100vh-140px)]">
        <div className="text-center mb-8 md:mb-10 max-w-4xl px-2 sm:px-4">
          {accessExpiryLabel && (
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.4 }}
              className="theme-surface rounded-full border inline-block mb-3 px-4 py-1.5 text-sm theme-text-secondary"
            >
              Accès actif jusqu'au {accessExpiryLabel}
            </motion.div>
          )}
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.5 }}
            className="theme-badge inline-block mb-4 px-4 py-1.5 rounded-full border text-sm font-medium"
          >
            Tech Lead Backend · Banque & Assurance · 8 ans d'XP
          </motion.div>
          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1, duration: 0.5 }}
            className="theme-brand-gradient text-3xl sm:text-4xl md:text-6xl font-bold mb-5 md:mb-6 bg-clip-text text-transparent tracking-tight"
          >
            CV interactif prive pour{' '}
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-600">
              evaluer rapidement
            </span>{' '}
            le profil de Mohamed
          </motion.h1>
          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2, duration: 0.5 }}
            className="theme-text-muted text-base md:text-lg leading-relaxed max-w-3xl mx-auto"
          >
            En quelques minutes, identifiez le niveau, le contexte metier, la stack dominante et les responsabilites recentes. Le chat sert ensuite a verifier ou approfondir un point precis du parcours.
          </motion.p>
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3, duration: 0.5 }}
            className="mt-6 md:mt-8 flex flex-col sm:flex-row gap-3 justify-center"
          >
            <a
              href="#experiences"
              className="btn-theme-secondary inline-flex items-center justify-center gap-2 rounded-2xl border px-5 py-3 transition-colors"
            >
              <ArrowRight className="w-4 h-4 text-cyan-400" />
              Voir les experiences
            </a>
            <a
              href="mailto:daoud.mohamed.tn@gmail.com?subject=Echange%20sur%20votre%20profil"
              className="btn-theme-primary inline-flex items-center justify-center gap-2 rounded-2xl font-semibold px-5 py-3 transition-colors"
            >
              <Mail className="w-4 h-4" />
              Contacter Mohamed
            </a>
          </motion.div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.35, duration: 0.5 }}
          className="w-full max-w-5xl px-0 md:px-0 mb-10 md:mb-12"
        >
          <div className="grid grid-cols-1 lg:grid-cols-[1.3fr_0.7fr] gap-4 md:gap-6">
            <div className="theme-panel rounded-3xl p-5 sm:p-6 md:p-8 border backdrop-blur-md">
              <div className="theme-text-accent flex items-center gap-2 text-sm font-medium mb-4">
                <BadgeCheck className="w-4 h-4" />
                Resume recruteur en 30 secondes
              </div>
              <h2 className="theme-text-primary text-xl sm:text-2xl md:text-3xl font-bold mb-4">
                Profil backend senior, entre delivery, architecture et responsabilite technique
              </h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {recruiterSummary.map((item) => (
                  <div key={item} className="theme-surface rounded-2xl border px-4 py-3 text-sm theme-text-secondary leading-relaxed">
                    {item}
                  </div>
                ))}
              </div>
            </div>

            <div className="theme-panel rounded-3xl p-5 sm:p-6 md:p-8 border backdrop-blur-md">
              <div className="theme-text-accent text-sm font-medium mb-4">Points de repere</div>
              <KeyReferenceList items={keyReferenceItems} />
            </div>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.37, duration: 0.5 }}
          className="w-full max-w-5xl px-0 md:px-0 mb-8"
        >
          <MetricGrid items={overviewMetrics} />
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.38, duration: 0.5 }}
          className="w-full max-w-5xl px-0 md:px-0 mb-8 md:mb-10"
          id="preuves"
        >
          <div className="theme-panel rounded-3xl border backdrop-blur-md p-5 sm:p-6 md:p-8">
            <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4 mb-6">
              <div>
                <div className="theme-text-accent inline-flex items-center gap-2 text-sm font-medium mb-3">
                  <ShieldCheck className="w-4 h-4" />
                  Pourquoi ce profil est pertinent
                </div>
                <h2 className="theme-text-primary text-xl sm:text-2xl md:text-3xl font-bold mb-2">
                  Un profil utile quand il faut tenir la technique sans perdre la livraison
                </h2>
                <p className="theme-text-muted max-w-3xl leading-relaxed">
                  Le site met en avant les signaux utiles pour un recruteur: niveau technique, contextes metier, type de responsabilites et capacite a faire avancer des sujets critiques en production.
                </p>
              </div>
              <a
                href="mailto:daoud.mohamed.tn@gmail.com?subject=Echange%20sur%20un%20poste%20Backend%20Lead"
                className="btn-theme-secondary inline-flex items-center gap-2 rounded-2xl border px-4 py-3 text-sm font-medium transition-colors"
              >
                <ArrowRight className="w-4 h-4 text-cyan-400" />
                Ouvrir un echange rapide
              </a>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3 md:gap-4">
              {credibilitySignals.map((item) => (
                <div key={item.title} className="theme-surface rounded-2xl border p-5">
                  <div className="theme-text-accent text-sm font-medium mb-2">{item.title}</div>
                  <p className="theme-text-secondary text-sm leading-relaxed">{item.text}</p>
                </div>
              ))}
            </div>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.39, duration: 0.45 }}
          className="w-full max-w-5xl px-0 md:px-0 mb-8"
        >
          <div className="theme-panel-soft rounded-3xl border px-4 py-4 sm:px-5 sm:py-5 md:px-6 md:py-6">
            <div className="grid grid-cols-1 md:grid-cols-[0.9fr_1.1fr] gap-4 items-start">
              <div>
                <div className="theme-text-accent text-sm font-medium mb-2">Avant d utiliser le chat</div>
                <h3 className="theme-text-primary text-xl font-semibold mb-2">L essentiel du profil est deja visible plus haut</h3>
                <p className="theme-text-muted text-sm leading-relaxed">
                  Le chat est surtout utile pour verifier un point cible: contexte metier, responsabilites, niveau sur Spring Boot, microservices ou certifications.
                </p>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {chatPreparationItems.map((item) => (
                  <div key={item} className="theme-surface rounded-2xl border px-4 py-3 text-sm theme-text-secondary">
                    {item}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 40 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.6 }}
          className="w-full max-w-3xl px-0 md:px-0 mb-14 md:mb-16 z-10"
        >
          <div className="theme-panel-soft mb-4 rounded-3xl border backdrop-blur-sm px-5 py-4 text-left">
            <div className="theme-text-accent text-sm font-medium mb-1">Assistant de lecture du CV</div>
            <p className="theme-text-muted text-sm leading-relaxed">
              Utilisez le chat pour approfondir un point precis du profil: experience bancaire, architecture microservices, stack Spring Boot, responsabilites recentes ou certifications.
            </p>
          </div>
          <ErrorBoundary>
            <ChatWindow
              messages={messages}
              isLoading={isLoading}
              onSendMessage={onSendMessage}
              onNewSession={onNewSession}
            />
          </ErrorBoundary>
        </motion.div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 md:gap-4 w-full max-w-4xl px-0 md:px-0 mb-20 md:mb-24">
          {[
            { icon: Briefcase, label: "Annees d'experience", value: '8+', delay: 0.4 },
            { icon: Code, label: 'Certifications', value: '5', delay: 0.5 },
            { icon: Cpu, label: 'Technologies clees', value: '20+', delay: 0.6 },
            { icon: Globe, label: 'Contextes critiques', value: '4+', delay: 0.7 },
          ].map(({ icon: Icon, label, value, delay }) => (
            <motion.div
              key={label}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay, duration: 0.5 }}
              className="theme-panel-soft backdrop-blur-md border p-6 rounded-2xl flex flex-col items-center justify-center text-center group transition-colors"
            >
              <div className="theme-surface mb-3 p-3 rounded-full group-hover:text-cyan-400 transition-colors theme-text-muted border">
                <Icon className="w-6 h-6" />
              </div>
              <h4 className="theme-text-primary text-2xl font-bold mb-1">{value}</h4>
              <p className="theme-text-muted text-sm">{label}</p>
            </motion.div>
          ))}
        </div>
      </div>

      <div
        id="experiences"
        className="w-full border-t flex flex-col items-center scroll-mt-24"
        style={{ background: 'color-mix(in srgb, var(--panel-soft) 92%, transparent)', borderColor: 'var(--border)' }}
      >
        <section className="w-full max-w-6xl mx-auto px-4 pt-16 md:pt-20">
          <div className="theme-panel rounded-3xl border backdrop-blur-md p-5 sm:p-6 md:p-8">
            <div className="max-w-3xl mb-8">
              <div className="theme-text-accent text-sm font-medium mb-3">Ce que je peux prendre en charge rapidement</div>
              <h2 className="theme-text-primary text-xl sm:text-2xl md:text-3xl font-bold mb-3">
                Des sujets backend, lead technique et fiabilite de delivery
              </h2>
              <p className="theme-text-muted leading-relaxed">
                Cette section aide a faire le lien entre le parcours et un besoin concret. Elle ne remplace pas l echange, mais permet de voir rapidement les types de sujets sur lesquels Mohamed peut etre utile des les premieres semaines.
              </p>
            </div>
            <SurfaceTextGrid items={contributionTopics} columnsClassName="grid grid-cols-1 md:grid-cols-2 gap-4" />
          </div>
        </section>

        <section className="w-full max-w-6xl mx-auto px-4 pt-6 md:pt-8">
          <div className="theme-panel-soft rounded-3xl border backdrop-blur-md p-5 sm:p-6 md:p-8">
            <div className="max-w-3xl mb-8">
              <div className="theme-text-accent text-sm font-medium mb-3">Postes cibles les plus naturels</div>
              <h2 className="theme-text-primary text-xl sm:text-2xl md:text-3xl font-bold mb-3">
                Les contextes ou le profil est le plus lisible et le plus coherent
              </h2>
              <p className="theme-text-muted leading-relaxed">
                Ce repere n enferme pas le profil, mais il aide a comprendre plus vite dans quels cadres l experience et les responsabilites recentes sont le plus directement transposables.
              </p>
            </div>
            <SurfaceTextGrid items={targetRoleItems} columnsClassName="grid grid-cols-1 md:grid-cols-3 gap-4" />
          </div>
        </section>

        <SkillsSection />
        <ExperienceSection />
      </div>
    </>
  )
}
