import { motion } from 'motion/react';
import { Calendar, Building, CheckCircle2 } from 'lucide-react';

interface Experience {
  role: string;
  company: string;
  period: string;
  description: string;
  tags: string[];
  recruiterSignals: string[];
  focus: string;
  scope: string;
  recruiterValue: string;
}

const ExperienceCard = ({ role, company, period, description, tags, recruiterSignals, focus, scope, recruiterValue, index }: Experience & { index: number }) => {
  return (
    <motion.div
      initial={{ opacity: 0, x: index % 2 === 0 ? -50 : 50 }}
      whileInView={{ opacity: 1, x: 0 }}
      viewport={{ once: true }}
      transition={{ duration: 0.6, delay: index * 0.1 }}
      className="relative pl-7 md:pl-0"
    >
      <div className={`md:flex items-center justify-between gap-8 ${index % 2 === 0 ? 'flex-row-reverse' : ''}`}>
        <div className="hidden md:block w-1/2" />

        <div className="absolute left-0 md:left-1/2 w-4 h-4 bg-cyan-500 rounded-full border-4 transform -translate-x-[5px] md:-translate-x-1/2 mt-1.5 z-10" style={{ borderColor: 'var(--app-bg)' }} />

        <div className="md:w-1/2 mb-8 md:mb-0">
          <div className={`theme-panel-soft backdrop-blur-md border p-4 sm:p-5 md:p-6 rounded-2xl hover:border-cyan-500/30 transition-all ${index % 2 === 0 ? 'md:mr-8' : 'md:ml-8'}`}>
            <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3 mb-4">
              <div>
                <h3 className="theme-text-primary text-lg sm:text-xl font-bold mb-1">{role}</h3>
                <div className="flex items-center text-cyan-400 text-sm gap-2">
                  <Building className="w-4 h-4" />
                  <span>{company}</span>
                </div>
              </div>
              <div className="theme-surface flex items-center theme-text-subtle text-xs sm:text-sm gap-1 px-3 py-1 rounded-full whitespace-nowrap self-start sm:ml-2 border">
                <Calendar className="w-3 h-3" />
                <span>{period}</span>
              </div>
            </div>

            <p className="theme-text-muted mb-4 leading-relaxed text-sm">{description}</p>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">
              <div className="theme-surface rounded-xl border px-3 py-3">
                <div className="theme-text-subtle text-[11px] uppercase tracking-[0.14em] mb-1">Focus</div>
                <div className="theme-text-secondary text-sm leading-relaxed">{focus}</div>
              </div>
              <div className="theme-surface rounded-xl border px-3 py-3">
                <div className="theme-text-subtle text-[11px] uppercase tracking-[0.14em] mb-1">Perimetre</div>
                <div className="theme-text-secondary text-sm leading-relaxed">{scope}</div>
              </div>
              <div className="theme-surface rounded-xl border px-3 py-3">
                <div className="theme-text-subtle text-[11px] uppercase tracking-[0.14em] mb-1">Interet recruteur</div>
                <div className="theme-text-secondary text-sm leading-relaxed">{recruiterValue}</div>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 mb-4">
              {recruiterSignals.map((signal) => (
                <div
                  key={signal}
                  className="theme-surface flex items-start gap-2 rounded-xl border px-3 py-2 text-xs theme-text-secondary"
                >
                  <CheckCircle2 className="w-3.5 h-3.5 mt-0.5 shrink-0 text-cyan-400" />
                  <span>{signal}</span>
                </div>
              ))}
            </div>

            <div className="flex flex-wrap gap-2">
              {tags.map((tag, i) => (
                <span key={i} className="theme-surface text-xs font-medium theme-text-secondary px-2 py-1 rounded border">
                  {tag}
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>
    </motion.div>
  );
};

export const ExperienceSection = () => {
  const experiences: Experience[] = [
    {
      role: 'Tech Lead Backend & Mobile',
      company: 'Malakoff Humanis',
      period: 'Jan. 2024 – Présent',
      description: "Pilotage technique d'une equipe de 5 devs (Android, iOS, Backend). Conception d'une architecture microservices Spring Boot 3 avec messaging RabbitMQ, securite OIDC/OAuth2/JWT, production Kubernetes et supervision Dynatrace.",
      recruiterSignals: ['Lead technique', 'Equipe de 5 devs', 'Production Kubernetes'],
      focus: 'Architecture microservices, securite et pilotage technique.',
      scope: 'Equipe transverse mobile + backend, delivery et arbitrages.',
      recruiterValue: 'Pertinent pour un poste de Tech Lead ou Lead Backend en contexte critique.',
      tags: ['Java', 'Spring Boot 3', 'WebFlux', 'RabbitMQ', 'Kubernetes', 'OAuth2', 'Dynatrace', 'Jenkins'],
    },
    {
      role: 'Senior Software Engineer',
      company: 'AXA France',
      period: 'Juil. 2022 – Déc. 2023',
      description: "Developpement et evolution d'une application mobile bancaire a forte audience. Backend BFF Spring Boot sur Azure, reduction de dette technique, TDD / BDD et support d incidents production.",
      recruiterSignals: ['Contexte bancaire', 'Backend BFF', 'Support production'],
      focus: 'Backend BFF, qualite logicielle et stabilisation d un produit existant.',
      scope: 'Application bancaire exposee, support production et dette technique.',
      recruiterValue: 'Montre une capacite a intervenir sur des produits sensibles avec forte exigence de qualite.',
      tags: ['Kotlin', 'Spring Boot', 'Azure', 'Clean Architecture', 'TDD', 'BDD'],
    },
    {
      role: 'Ingénieur Sénior Backend & Mobile',
      company: 'Malakoff Humanis',
      period: 'Juil. 2021 – Juil. 2022',
      description: "Evolution et maintenance de l'application mobile MH en production. Nouvelles fonctionnalites sur le back office BFF, reduction de dette technique, CI/CD, tests unitaires et bonnes pratiques SOLID.",
      recruiterSignals: ['Application en production', 'Dette technique', 'CI/CD et qualite'],
      focus: 'Evolution produit, robustesse et maintenabilite.',
      scope: 'Back office BFF, chaine CI/CD et qualite du code.',
      recruiterValue: 'Utile pour des equipes qui cherchent un profil senior fiable sur maintenance evolutive et delivery.',
      tags: ['Kotlin', 'Spring', 'Jetpack Compose', 'MVVM', 'Jenkins', 'Firebase'],
    },
    {
      role: 'Ingénieur développement Android Sénior',
      company: 'EquensWorldline',
      period: 'Août 2017 – Mars 2021',
      description: "Maintenance et evolution d'applications bancaires de paiement sans contact. SDK NFC (EMV, VISA, MasterCard), cryptographie Android Keystore, authentification biometrique, referent technique et code review.",
      recruiterSignals: ['Paiement & NFC', 'Cryptographie', 'Referent technique'],
      focus: 'Paiement, securite mobile et expertise Android.',
      scope: 'Produits bancaires, NFC/EMV, code review et expertise technique.',
      recruiterValue: 'Signale une base solide sur les sujets de securite, paiement et referent technique.',
      tags: ['Kotlin', 'Java', 'NFC/EMV', 'Cryptographie', 'MVVM', 'Gitlab CI'],
    },
  ];

  return (
    <section className="py-16 md:py-20 px-4 w-full max-w-6xl mx-auto relative">
      <div className="text-center mb-12 md:mb-16">
        <motion.h2
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="theme-text-primary text-3xl md:text-4xl font-bold mb-4"
        >
          Experiences cles
        </motion.h2>
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: 0.1 }}
          className="theme-text-muted max-w-2xl mx-auto"
        >
          Un parcours backend et mobile dans des contextes regulés, avec une progression visible vers le lead technique, la fiabilite en production et la responsabilite d equipe.
        </motion.p>
      </div>

      <div className="relative">
        <div className="absolute left-0 md:left-1/2 top-0 bottom-0 w-0.5 bg-gradient-to-b from-cyan-500/50 via-blue-500/20 to-transparent md:-translate-x-1/2" />

        <div className="space-y-12">
          {experiences.map((exp, index) => (
            <ExperienceCard key={index} {...exp} index={index} />
          ))}
        </div>
      </div>
    </section>
  );
};
