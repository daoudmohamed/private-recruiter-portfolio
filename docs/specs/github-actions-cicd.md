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
- connexion au tailnet via Tailscale avant tout acces au cluster
- provisionnement de `redis-prod` et `qdrant-prod` depuis le repo avant le deploiement applicatif
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
  contenu: kubeconfig base64 du cluster production, avec `server:` pointant vers l'adresse Tailscale du Raspberry/K3s
- `TS_OAUTH_CLIENT_ID`
  secret Tailscale OAuth client id utilise par GitHub Actions pour rejoindre le tailnet
- `TS_OAUTH_SECRET`
  secret Tailscale OAuth client secret utilise par GitHub Actions pour rejoindre le tailnet
- `TLS_CERT_PEM`
  contenu PEM du certificat origin TLS a installer en secret Kubernetes. Optionnel si le secret TLS existe deja dans le cluster.
- `TLS_KEY_PEM`
  contenu PEM de la cle privee associee a `TLS_CERT_PEM`. Optionnel si le secret TLS existe deja dans le cluster.
- `OPENAI_API_KEY`
  cle OpenAI utilisee par l'application
- `RECRUITER_ACCESS_TOKEN_SECRET`
  secret de signature des tokens d'acces recruteur
- `ADMIN_API_KEY`
  cle API admin pour les endpoints proteges
- `BREVO_API_KEY`
  cle API Brevo pour l'envoi d'emails recruteur
- `RECRUITER_ACCESS_CAPTCHA_RECAPTCHA_SECRET_KEY`
  secret reCAPTCHA origin si le captcha est active. Optionnel si le provider est `NONE`.
- `REDIS_PASSWORD`
  mot de passe Redis si ton service Redis en utilise un. Optionnel sinon.
- `QDRANT_API_KEY`
  cle API Qdrant si ton service Qdrant en utilise une. Optionnel sinon.

### Variables GitHub d'environnement `production`
- `TS_K8S_HOST`
  nom MagicDNS ou IP Tailscale du Raspberry/K3s. Utilise pour verifier que le runner voit bien le cluster a travers le tailnet.
- `PUBLIC_HOST`
  domaine public expose par Traefik, par exemple `portfolio.example.com`
- `PUBLIC_BASE_URL`
  URL publique canonique du site, par exemple `https://portfolio.example.com`
- `TLS_SECRET_NAME`
  nom du secret TLS Kubernetes utilise par l'Ingress, par exemple `portfolio-example-com-tls`

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

## Deploiement K3s via Tailscale

Le workflow `Deploy` part maintenant du principe que le cluster `k3s` sur Raspberry **n'est pas expose publiquement**.

Modele retenu:

- le Raspberry rejoint le tailnet Tailscale
- GitHub Actions rejoint temporairement le meme tailnet via `tailscale/github-action`
- le kubeconfig stocke dans `KUBE_CONFIG_B64` vise l'endpoint Kubernetes sur l'adresse Tailscale du Raspberry
- les tags Tailscale demandes par le runner sont injectes via `TS_TAGS` dans l'environnement GitHub `production`
- les vraies valeurs publiques d'Ingress (`PUBLIC_HOST`, `PUBLIC_BASE_URL`, `TLS_SECRET_NAME`) sont injectees par l'environnement GitHub `production`, pas hardcodees dans le repo
- si `TLS_CERT_PEM` et `TLS_KEY_PEM` sont presents, le workflow cree ou met a jour automatiquement le secret TLS Kubernetes reference par `TLS_SECRET_NAME`
- le workflow cree ou met a jour automatiquement le secret applicatif `mutuelle-secrets` depuis les secrets GitHub `production`
- le workflow applique aussi `k8s/services/production-k3s` pour provisionner `redis-prod` et `qdrant-prod` avec PVC avant le deploiement de l'application

Pre-requis cote infra:

- Tailscale installe et connecte sur le Raspberry
- l'API Kubernetes `k3s` ecoute sur l'interface Tailscale accessible depuis le tailnet
- le `server:` du kubeconfig reference l'IP ou le nom MagicDNS Tailscale du noeud

Exemple de `server:` attendu dans le kubeconfig:

```yaml
server: https://raspberry-k3s.tailnet-name.ts.net:6443
```

Si tu regeneres ton kubeconfig:

1. copie `/etc/rancher/k3s/k3s.yaml`
2. remplace `127.0.0.1` par l'adresse Tailscale/MagicDNS du Raspberry
3. encode le fichier en base64
4. mets le resultat dans le secret GitHub `production/KUBE_CONFIG_B64`

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
