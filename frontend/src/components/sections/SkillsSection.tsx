import { motion } from 'motion/react';
import { Database, Server, Shield, Terminal } from 'lucide-react';

const SkillCard = ({ title, items, icon: Icon, delay }: { title: string; items: string[]; icon: React.ElementType; delay: number }) => (
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    whileInView={{ opacity: 1, y: 0 }}
    viewport={{ once: true }}
    transition={{ delay, duration: 0.5 }}
    className="theme-panel-soft backdrop-blur-sm border p-5 sm:p-6 rounded-2xl hover:border-cyan-500/30 transition-colors group"
  >
    <div className="flex items-center gap-3 mb-4">
      <div className="theme-surface p-2 rounded-lg group-hover:bg-cyan-500/20 group-hover:text-cyan-400 transition-colors theme-text-muted border">
        <Icon className="w-5 h-5" />
      </div>
      <h3 className="theme-text-secondary text-lg sm:text-xl font-semibold">{title}</h3>
    </div>
    <div className="flex flex-wrap gap-2">
      {items.map((item, idx) => (
        <span
          key={idx}
          className="theme-surface px-3 py-1 border rounded-full text-sm theme-text-secondary hover:border-cyan-500/50 transition-colors cursor-default"
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
    <section className="py-16 md:py-20 px-4 w-full max-w-6xl mx-auto">
      <div className="text-center mb-12 md:mb-16">
        <motion.h2
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="theme-text-primary text-3xl md:text-4xl font-bold mb-4"
        >
          Stack et points forts
        </motion.h2>
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: 0.1 }}
          className="theme-text-muted max-w-2xl mx-auto"
        >
          Une lecture rapide des technologies, pratiques d architecture et sujets sur lesquels Mohamed peut etre rapidement operationnel ou credible en entretien.
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
