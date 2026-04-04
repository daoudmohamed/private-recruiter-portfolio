import { motion } from 'motion/react';
import { Calendar, Building } from 'lucide-react';

interface Experience {
  role: string;
  company: string;
  period: string;
  description: string;
  tags: string[];
}

const ExperienceCard = ({ role, company, period, description, tags, index }: Experience & { index: number }) => {
  return (
    <motion.div
      initial={{ opacity: 0, x: index % 2 === 0 ? -50 : 50 }}
      whileInView={{ opacity: 1, x: 0 }}
      viewport={{ once: true }}
      transition={{ duration: 0.6, delay: index * 0.1 }}
      className="relative pl-8 md:pl-0"
    >
      <div className={`md:flex items-center justify-between gap-8 ${index % 2 === 0 ? 'flex-row-reverse' : ''}`}>
        <div className="hidden md:block w-1/2" />

        <div className="absolute left-0 md:left-1/2 w-4 h-4 bg-cyan-500 rounded-full border-4 border-slate-900 transform -translate-x-[5px] md:-translate-x-1/2 mt-1.5 z-10" />

        <div className="md:w-1/2 mb-8 md:mb-0">
          <div className={`bg-slate-900/40 backdrop-blur-md border border-slate-800 p-6 rounded-2xl hover:border-cyan-500/30 transition-all hover:bg-slate-800/60 ${index % 2 === 0 ? 'md:mr-8' : 'md:ml-8'}`}>
            <div className="flex items-start justify-between mb-4">
              <div>
                <h3 className="text-xl font-bold text-white mb-1">{role}</h3>
                <div className="flex items-center text-cyan-400 text-sm gap-2">
                  <Building className="w-4 h-4" />
                  <span>{company}</span>
                </div>
              </div>
              <div className="flex items-center text-slate-500 text-sm gap-1 bg-slate-800/50 px-3 py-1 rounded-full whitespace-nowrap ml-2">
                <Calendar className="w-3 h-3" />
                <span>{period}</span>
              </div>
            </div>

            <p className="text-slate-400 mb-4 leading-relaxed text-sm">{description}</p>

            <div className="flex flex-wrap gap-2">
              {tags.map((tag, i) => (
                <span key={i} className="text-xs font-medium text-slate-300 bg-slate-800 px-2 py-1 rounded border border-slate-700/50">
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
      description: "Pilotage technique d'une équipe de 5 devs (Android, iOS, Backend). Conception d'une architecture microservices Spring Boot 3 avec messaging RabbitMQ, sécurité OIDC/OAuth2/JWT, production Kubernetes (TKGs) et monitoring Dynatrace.",
      tags: ['Java', 'Spring Boot 3', 'WebFlux', 'RabbitMQ', 'Kubernetes', 'OAuth2', 'Dynatrace', 'Jenkins'],
    },
    {
      role: 'Senior Software Engineer',
      company: 'AXA France',
      period: 'Juil. 2022 – Déc. 2023',
      description: "Développement et évolution d'une application mobile bancaire à forte audience. Backend BFF Spring Boot sur Azure, réduction dette technique, TDD & BDD (Gherkin), support incidents production.",
      tags: ['Kotlin', 'Spring Boot', 'Azure', 'Clean Architecture', 'TDD', 'BDD'],
    },
    {
      role: 'Ingénieur Sénior Backend & Mobile',
      company: 'Malakoff Humanis',
      period: 'Juil. 2021 – Juil. 2022',
      description: "Évolution et maintenance de l'application mobile MH en production. Nouvelles fonctionnalités sur le back office BFF, réduction dette technique, CI/CD, tests unitaires et bonnes pratiques SOLID.",
      tags: ['Kotlin', 'Spring', 'Jetpack Compose', 'MVVM', 'Jenkins', 'Firebase'],
    },
    {
      role: 'Ingénieur développement Android Sénior',
      company: 'EquensWorldline',
      period: 'Août 2017 – Mars 2021',
      description: "Maintenance et évolution d'applications bancaires de paiement sans contact. SDK NFC (EMV, VISA, MasterCard), cryptographie Android Keystore, authentification biométrique, référent technique et code review.",
      tags: ['Kotlin', 'Java', 'NFC/EMV', 'Cryptographie', 'MVVM', 'Gitlab CI'],
    },
  ];

  return (
    <section className="py-20 px-4 w-full max-w-6xl mx-auto relative">
      <div className="text-center mb-16">
        <motion.h2
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-3xl md:text-4xl font-bold text-white mb-4"
        >
          Parcours Professionnel
        </motion.h2>
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: 0.1 }}
          className="text-slate-400 max-w-2xl mx-auto"
        >
          8 ans d'expérience en banque & assurance, de développeur à Tech Lead d'équipe.
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
