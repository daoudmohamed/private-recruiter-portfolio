import { motion } from 'motion/react';
import { Database, Server, Shield, Terminal } from 'lucide-react';

const SkillCard = ({ title, items, icon: Icon, delay }: { title: string; items: string[]; icon: React.ElementType; delay: number }) => (
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    whileInView={{ opacity: 1, y: 0 }}
    viewport={{ once: true }}
    transition={{ delay, duration: 0.5 }}
    className="bg-slate-900/50 backdrop-blur-sm border border-slate-800 p-6 rounded-2xl hover:border-cyan-500/30 transition-colors group"
  >
    <div className="flex items-center gap-3 mb-4">
      <div className="p-2 bg-slate-800 rounded-lg group-hover:bg-cyan-500/20 group-hover:text-cyan-400 transition-colors text-slate-400">
        <Icon className="w-5 h-5" />
      </div>
      <h3 className="text-xl font-semibold text-slate-200">{title}</h3>
    </div>
    <div className="flex flex-wrap gap-2">
      {items.map((item, idx) => (
        <span
          key={idx}
          className="px-3 py-1 bg-slate-800/50 border border-slate-700 rounded-full text-sm text-slate-300 hover:text-white hover:border-cyan-500/50 transition-colors cursor-default"
        >
          {item}
        </span>
      ))}
    </div>
  </motion.div>
);

export const SkillsSection = () => {
  const skills = [
    {
      title: 'Backend & Architecture',
      icon: Server,
      items: ['Java', 'Spring Boot 3', 'WebFlux', 'Microservices', 'Architecture Hexagonale', 'Event-Driven', 'RabbitMQ', 'Kotlin'],
    },
    {
      title: 'Cloud & DevOps',
      icon: Terminal,
      items: ['Kubernetes', 'TKGs', 'Docker', 'CI/CD', 'Jenkins', 'Dynatrace', 'AWS', 'Observabilité'],
    },
    {
      title: 'Sécurité & Data',
      icon: Shield,
      items: ['OIDC', 'OAuth2', 'JWT', 'AES', 'MongoDB', 'MariaDB'],
    },
    {
      title: 'Mobile & Outils',
      icon: Database,
      items: ['Android SDK', 'Jetpack Compose', 'MVVM', 'Clean Architecture', 'Sonar', 'Git', 'Gradle'],
    },
  ];

  return (
    <section className="py-20 px-4 w-full max-w-6xl mx-auto">
      <div className="text-center mb-16">
        <motion.h2
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-3xl md:text-4xl font-bold text-white mb-4"
        >
          Arsenal Technique
        </motion.h2>
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: 0.1 }}
          className="text-slate-400 max-w-2xl mx-auto"
        >
          Une expertise backend & cloud forgée sur 8 ans en environnements bancaires et assurantiels à forte criticité.
        </motion.p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {skills.map((skill, index) => (
          <SkillCard key={skill.title} {...skill} delay={index * 0.1} />
        ))}
      </div>
    </section>
  );
};
