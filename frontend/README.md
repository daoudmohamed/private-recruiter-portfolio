# Frontend

Frontend React/Vite du portfolio privé recruteur.

## Démarrage

```bash
npm install
npm run dev
npm run build
npm test
```

Par défaut, l'application tourne sur `http://localhost:5173`.

## Configuration

Le frontend cible `VITE_API_BASE`, avec `/api/v1` comme valeur par défaut.

Exemple:

```bash
VITE_API_BASE=http://localhost:8080/api/v1
```

## Responsabilités

- shell d'application et thème
- portail recruteur privé
- demande d'invitation et consommation de lien d'accès
- chat en streaming via SSE
- rendu du portfolio et points de contact

## Structure

```text
src/
  app/        app shell, bootstrapping, app-wide hooks
  features/   chat, recruiter access, portfolio
  shared/     UI réutilisable et assets stables
  utils/      API client centralisé et helpers bas niveau
```

## Règles

- garder l'accès API centralisé dans `src/utils/api.ts`
- éviter de remettre l'orchestration produit dans `App.tsx`
- ajouter la logique métier frontend dans `features/*`
- ne promouvoir dans `shared/*` que ce qui a une vraie réutilisation

Pour les conventions du dépôt :

- `../AGENTS.md`
