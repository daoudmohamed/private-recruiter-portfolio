import { motion } from 'motion/react'
import { LinkedinIcon, Mail, MessageSquareMore } from 'lucide-react'

export function ContactSection() {
  return (
    <>
      <section id="contact" className="w-full max-w-5xl mx-auto px-4 md:px-0 pt-16 md:pt-20 scroll-mt-24">
        <div className="theme-panel rounded-3xl border backdrop-blur-md p-5 sm:p-6 md:p-8">
          <div className="grid grid-cols-1 lg:grid-cols-[1.15fr_0.85fr] gap-6">
            <div>
              <div className="theme-text-accent inline-flex items-center gap-2 text-sm font-medium mb-3">
                <MessageSquareMore className="w-4 h-4" />
                Passer du portfolio a l echange
              </div>
              <h2 className="theme-text-primary text-2xl md:text-3xl font-bold mb-3">
                Si le profil semble pertinent, un echange direct sera plus utile qu une lecture plus longue
              </h2>
              <p className="theme-text-muted leading-relaxed mb-5">
                Le site sert a accelerer la lecture du CV. Si vous souhaitez valider un contexte, discuter d un poste ou confronter le parcours a un besoin reel, le contact direct reste l etape la plus efficace.
              </p>
              <div className="flex flex-col sm:flex-row gap-3">
                <a
                  href="mailto:daoud.mohamed.tn@gmail.com?subject=Prise%20de%20contact%20suite%20a%20votre%20portfolio"
                  className="btn-theme-primary inline-flex items-center justify-center gap-2 rounded-2xl font-semibold px-5 py-3 transition-colors"
                >
                  <Mail className="w-4 h-4" />
                  Contacter par email
                </a>
                <a
                  href="https://www.linkedin.com/in/daoudmohamed/"
                  target="_blank"
                  rel="noreferrer"
                  className="btn-theme-secondary inline-flex items-center justify-center gap-2 rounded-2xl border px-5 py-3 transition-colors"
                >
                  <LinkedinIcon className="w-4 h-4" />
                  Ecrire sur LinkedIn
                </a>
              </div>
            </div>
            <div className="theme-surface rounded-2xl border p-5">
              <div className="theme-text-accent text-sm font-medium mb-4">Quand prendre contact</div>
              <div className="space-y-3 text-sm theme-text-secondary leading-relaxed">
                <div className="theme-surface-soft rounded-xl border px-4 py-3">
                  Vous cherchez un profil backend senior capable de tenir des sujets critiques en production.
                </div>
                <div className="theme-surface-soft rounded-xl border px-4 py-3">
                  Vous voulez valider une experience banque, assurance, paiements ou architecture microservices.
                </div>
                <div className="theme-surface-soft rounded-xl border px-4 py-3">
                  Vous souhaitez confronter rapidement le parcours a un poste de Tech Lead, Senior Backend ou Lead Engineer.
                </div>
              </div>
              <div className="theme-badge mt-5 rounded-xl border px-4 py-3 text-sm theme-text-secondary">
                Reponse attendue cote Mohamed: echange rapide, retour direct et discussion concrete sur le contexte, le poste et les attentes.
              </div>
            </div>
          </div>
        </div>
      </section>

      <motion.div
        initial={{ opacity: 0 }}
        whileInView={{ opacity: 1 }}
        viewport={{ once: true }}
        className="theme-text-subtle text-center text-sm pb-8 pt-20"
      >
        <p>© 2026 Mohamed Daoud · Tech Lead Backend · Paris</p>
      </motion.div>
    </>
  )
}
