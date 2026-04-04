# GitHub Actions CI/CD

Ce depot utilise GitHub Actions pour couvrir 3 niveaux:

1. `CI`
- scan de secrets
- installation du binaire officiel Gitleaks
- tests backend Gradle
- lint + build frontend
- validation Docker Compose
- `helm lint`
- rendu Helm `production-k3s`

2. `Container Image`
- build de l'image Docker depuis `docker/Dockerfile`
- publication via un tag de quarantaine puis promotion vers les tags stables
- scan de vulnerabilites Trivy prevu sur l'image publiee, mais temporairement desactive
- cache Buildx
- SBOM et provenance d'image

### Lancer localement l'image GHCR

Si tu veux lancer localement l'image publiee sans rebuild:

```bash
APP_IMAGE='ghcr.io/daoudmohamed/private-recruiter-portfolio:main' \
docker compose -f docker/docker-compose.ghcr.yml up -d
```

Si tu veux lancer un digest precis, utilise la syntaxe Docker correcte avec `@sha256:`:

```bash
APP_IMAGE='ghcr.io/daoudmohamed/private-recruiter-portfolio@sha256:<digest>' \
docker compose -f docker/docker-compose.ghcr.yml up -d
```

Ne pas utiliser `:sha256-...` sauf si c'est reellement un tag publie comme tel.

3. `Deploy`
- declenchement manuel vers `production`
- environnement GitHub `production`
- lint Helm
- rendu Helm avec remplacement d'image
- `dry_run` par defaut
- `helm upgrade --install` quand le deploy est reel
- `kubectl rollout status` quand le deploy est reel
- verification des prerequis cluster
- smoke minimal post-deploiement

4. `Security Analysis`
- `dependency-review` sur pull request
- CodeQL `java-kotlin` et `javascript-typescript`
- Sonar optionnel si `SONAR_TOKEN` et `SONAR_HOST_URL` sont configures
- DAST baseline OWASP ZAP prevu sur la stack Docker locale, mais temporairement desactive
- couverture backend JaCoCo publiee vers Sonar
- analyse frontend statique incluse dans Sonar
- pas de couverture frontend tant qu'aucun test frontend n'existe dans le repo

## Helm

Helm est maintenant la methode recommandee pour la parametrisation **et** le deploiement de production.

Reference:

- `helm/private-recruiter-portfolio`
- `docs/specs/helm-production-guide.md`

## Secrets GitHub requis

### Repository / organisation
- aucun secret requis pour la CI standard
- `GITHUB_TOKEN` suffit pour pousser l'image vers GHCR

### Environment `production`
- `KUBE_CONFIG_B64`
  contenu: kubeconfig base64 du cluster production

### Optionnels pour l'analyse securite
- `SONAR_TOKEN`
  secret repository ou organisation pour activer l'analyse Sonar

### Variables GitHub optionnelles
- `SONAR_HOST_URL`
  URL du serveur SonarQube ou SonarCloud
- `SONAR_PROJECT_KEY`
  cle de projet Sonar

## Image publiee

Image cible:

`ghcr.io/<owner>/<repo>`

Tags generes:
- `main`
- `latest` sur branche par defaut
- `sha-<commit>`
- tags Git si push de `v*`

## Strategie recommandee

- PR:
  CI uniquement
- merge sur `main`:
  CI + Security Analysis + build/push image
- deploy production:
  workflow manuel avec `environment=production`
  avec approbation GitHub Environment si activee

## Pratiques de securite retenues

- permissions GitHub Actions minimales par workflow
- scan de secrets dans la CI via le binaire officiel Gitleaks
- scan de vulnerabilites de l'image Docker
- scan Trivy temporairement desactive le temps de stabiliser le workflow
- review des dependances en PR
- SAST structurel via CodeQL
- DAST baseline via OWASP ZAP
- analyse qualite/securite Sonar si configuree
- image signee/attestee via provenance
- deploiement manuel, pas d'auto-promotion implicite
- secrets Kubernetes confines aux GitHub Environments
- opt-in Node 24 active pour anticiper la fin du runtime Node 20 des actions JavaScript
