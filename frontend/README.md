# Knowledge Base Frontend

Interface React/Vite pour consommer l'API RAG du projet.

## Démarrage

```bash
npm install
npm run dev
npm run build
```

Par défaut, l'application tourne sur `http://localhost:5173`.

## Configuration

Le frontend cible `VITE_API_BASE`, avec `/api/v1` comme valeur par défaut.

Exemple:

```bash
VITE_API_BASE=http://localhost:8080/api/v1
```

## Fonctionnalités actuelles

- Chat en streaming via SSE
- Création et persistance locale d'une session de conversation
- Saisie optionnelle d'une API key stockée en `sessionStorage`
- Sections de présentation statiques autour du chat

## Structure

```text
src/
  App.tsx
  components/
    ChatWindow.tsx
    ErrorBoundary.tsx
    chat/
    sections/
  utils/
    api.ts
    security.ts
```

## Limites actuelles

- L'upload de documents existe dans l'API frontend, mais aucun composant d'upload n'est encore exposé dans l'interface.
- La clé API utilisateur est conservée côté navigateur pour la durée de l'onglet.
