# Knowledge Base Frontend

Interface React pour tester l'application Knowledge Base RAG.

## Démarrage rapide

```bash
# Installer les dépendances
npm install

# Lancer en développement
npm run dev

# Build pour la production
npm run build
```

L'app démarre sur `http://localhost:5173`

## Features

- 💬 Chat streaming (SSE) en temps réel
- 📄 Upload de documents (PDF, TXT, MD)
- 📁 Scanner automatique du dossier
- 🔑 Support API Key optionnel
- 💾 Historique des sessions

## Architecture

```
src/
├── App.jsx              # Main app
├── components/
│   ├── ChatWindow.jsx   # Chat UI
│   ├── DocumentUpload.jsx # Upload
│   ├── ChatWindow.css
│   └── DocumentUpload.css
└── App.css
```

## Configuration

L'app se connecte par défaut à `http://localhost:8080/api/v1`

Pour une autre URL, modifier dans App.jsx:
```javascript
const API_BASE = 'http://your-server:8080/api/v1'
```

## API Key

Pour utiliser une API Key:
1. Cliquer sur ⚙️ Paramètres
2. Entrer votre clé
3. Elle est sauvegardée localement

## Développement

- Vite pour hot reload
- React Hooks pour l'état
- Fetch API pour les requêtes
- CSS Grid/Flexbox pour le layout
