# Politique interne - Acces recruteur par email

## Finalite

Les adresses email collectees via le formulaire d'acces recruteur sont utilisees uniquement pour:

- envoyer un lien d'acces temporaire au site
- gerer l'expiration ou la reemission d'une invitation
- journaliser les evenements de securite strictement necessaires

Toute reutilisation a des fins de prospection, nurturing, CRM ou communication marketing est exclue de ce flux.

## Donnees traitees

- adresse email du recruteur
- metadonnees d'invitation: identifiant, statut, dates de creation/expiration/consommation
- metadonnees de session temporaire
- donnees techniques de protection: IP, evenements de rate limiting, verification captcha si activee

## Base legale

Le traitement est opere sur la base de l'interet legitime pour proteger un espace prive et envoyer un lien d'acces temporaire explicitement demande.

## Conservation

- invitation active: jusqu'a expiration, puis suppression automatique
- session recruteur: jusqu'a expiration, puis suppression automatique
- journaux de securite: conservation limitee et proportionnee, cible recommandee 90 jours maximum

## Sous-traitants

- envoi d'email transactionnel: Brevo si active
- protection anti-bot: Google reCAPTCHA si active

Avant mise en production, verifier et archiver:

- le DPA du prestataire email
- le cadre de transfert applicable si le prestataire est hors UE
- le DPA et la documentation de transfert du service anti-bot si utilise

## Obligations produit

- afficher une mention d'information concise au niveau du formulaire
- ne jamais exposer l'email dans le lien magique
- ne pas journaliser le lien magique en production
- masquer les emails dans les logs applicatifs
- fournir un point de contact pour les demandes relatives aux donnees personnelles
