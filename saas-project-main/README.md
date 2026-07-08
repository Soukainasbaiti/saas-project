# SaaS Gestion — Documentation Technique

Application de gestion de projets (suivi budgétaire, risques, issues, opportunités, WIP/MIP) destinée aux Project Managers, Business Unit Managers et Administrateurs. Elle couvre tout le cycle de vie d'un projet : création, validation par un administrateur, suivi mensuel des prévisions, et gestion des registres associés (risques, issues, opportunités).

---

## 1. Vue d'ensemble de l'architecture

```
saas-project-main/
├── saasgestion/          → Backend Spring Boot (Java 21)
├── saasgestion-front/    → Frontend Angular 21
├── init.sql / dump.sql   → Scripts d'initialisation PostgreSQL
├── docker-compose.yml    → Orchestration locale (Postgres + Backend + Frontend)
├── .env.example          → Variables d'environnement requises
```

- **Frontend** : Angular 21, déployé sur **Vercel**
- **Backend** : Spring Boot 3.5.13 (API REST), déployé sur **Railway**
- **Base de données** : PostgreSQL 16, hébergée sur **Railway**
- **Email transactionnel** : Brevo (pour les workflows d'approbation par email)

⚠️ Ces trois composants sont hébergés séparément. Si le backend ou la base de données est arrêté(e) (ex: service Railway suspendu), le frontend reste visuellement accessible mais l'application est **non fonctionnelle** (aucune donnée, erreurs API).

---

## 2. Stack technique — Frontend (`saasgestion-front/`)

| Élément | Version / Choix |
|---|---|
| Framework | Angular 21.2.0 |
| Langage | TypeScript 5.9.2 |
| UI | Angular Material 21.2.2 + Angular CDK |
| Graphiques | Chart.js 4.5.1 + ng2-charts |
| Export PDF | jsPDF 4.2.1, jspdf-autotable, html2canvas |
| Export Excel | xlsx 0.18.5 |
| HTTP / Réactivité | RxJS 7.8.0, HttpClient Angular |
| SSR | @angular/ssr 21.2.2 + Express 5.1.0 |
| Tests | Vitest |
| Package manager | npm 10.8.2 |

### Architecture Angular (`src/app/`)

**Core (`core/`)**
- `guards/` — `authGuard`, `adminGuard`, `loginGuard` : protègent les routes selon l'état de connexion et le rôle
- `interceptors/` — `JwtAuthInterceptor` : injecte automatiquement le token JWT dans chaque requête HTTP
- `services/` — `AuthService` (login/logout/refresh token), `ApiService` (appels HTTP centralisés), `I18nService`
- `models/` — interfaces TypeScript (ex: `Project`)

**Features (`features/`)** — un module par fonctionnalité :

| Module | Rôle |
|---|---|
| `login` | Authentification |
| `dashboard` | Vue d'ensemble des projets (PM) |
| `project-form` | Création d'un nouveau projet |
| `project-detail` | Détail d'un projet et de ses registres |
| `project-edit` | Édition d'un projet rejeté (via token email) |
| `project-management` | Suivi du cycle de vie, prévisions mensuelles |
| `admin-dashboard` | Tableau de bord KPI (vue exécutive), filtres Client/Groupe Client/Industrie |
| `admin-users` | Gestion des utilisateurs et rôles (admin) |
| `admin-approve` | Approbation des projets créés/modifiés via lien email |
| `admin-ref-data` | Gestion des données de référence (BU, Clients, Industries...) |
| `admin-sidebar` | Navigation admin |
| `change-password` | Changement de mot de passe forcé à la première connexion |
| `issue-register` | Registre des issues |
| `risk-register` | Registre des risques |
| `opportunity-register` | Registre des opportunités |
| `mip-register` | Registre MIP (Management Issue Pool) |
| `wip-register` | Registre WIP (Work In Progress) |

### Routing principal (`app.routes.ts`)

- Public : `/login`
- Protégé (auth) : `/`, `/projects/new`, `/projects/:id`, `/projects/:id/management`, `/change-password`
- Admin : `/admin`, `/admin/users`, `/admin/ref-data`
- Basé sur token (sans login) : `/admin/approve/:token`, `/projects/edit/:token`
- Fallback : `**` → accueil

---

## 3. Stack technique — Backend (`saasgestion/`)

| Élément | Version / Choix |
|---|---|
| Framework | Spring Boot 3.5.13 |
| Langage | Java 21 (Eclipse Temurin) |
| Build | Maven 3.9.6 |
| Port / Context-path | 8080 / `/api` |
| ORM | Spring Data JPA + Hibernate 6.3 |
| Sécurité | Spring Security 6.x + JWT (jjwt 0.12.5) |
| Validation | Jakarta Validation |
| Mapping DTO | MapStruct 1.5.5 |
| Génération de code | Lombok |
| Documentation API | Springdoc-OpenAPI 2.6.0 (Swagger UI) |
| PDF | Apache PDFBox 3.0.3 (documents BL signés) |
| Excel/CSV | Apache POI 5.3.0 |
| Email | Intégration API Brevo |

### Architecture (`src/main/java/com/segula/saasgestion/`)

- **controller/** — 12 contrôleurs REST : `AuthController`, `ProjectController`, `ProjectManagementController`, `ProjectEditController`, `AdminController`, `ReferenceDataController`, `AdminRefController`, `RiskController`, `IssueController`, `WipController`, `MipController`, `OpportunityController`
- **service/** — logique métier : `ProjectService`, `ProjectManagementService`, `ProjectPendingService`, `AuthService`, `EmailService`, `RiskService`, `IssueService`, `WipService`, `MipService`, `OpportunityService`
- **domain/** — entités JPA : `Project`, `ProjectResource(Entry)`, `ProjectDeliverable`, `ProjectRisk`, `ProjectIssue`, `ProjectMip`, `ProjectOpportunity`, `ProjectWip`, `ProjectManagementConfig`, `ProjectMonthlyForecast`, `ProjectMonthStatus`, `ProjectValidationHistory`, `ProjectAuditLog`, `ProjectWorkTicket`, `ProjectWorkType`, `AppUser`, `BU`, `Customer`, `Engagement`, `Industry`, `EngineeringDiscipline`, `FrontFinancier`, `ProjectFunction`, `TechnicalOffice`, `RefreshToken`
- **repository/** — couche d'accès aux données (Spring Data)
- **security/** — filtre JWT, utilitaires JWT, configuration de sécurité
- **dto/** — objets de transfert pour les requêtes/réponses API

### Sécurité

- Authentification par JWT : access token (1h), refresh token (7 jours)
- Filtrage des requêtes via `JwtAuthFilter`
- Attribution des rôles lors du workflow d'approbation par email

---

## 4. Base de données (PostgreSQL 16)

**Types personnalisés (ENUM)**
- `project_status` : `On Going`, `Closed`, `Planned`, `On Hold`, `Canceled`
- `technical_office_type` : `Front Office`, `Back Office`

**Tables principales**
- `app_user` (rôles : USER, PM, BUM, ADMIN)
- `bu`, `customer`, `engagement`, `industry`, `engineering_discipline`, `front_financier`, `project_function` — données de référence
- `project` — table centrale (budgets, marges calculées, soft delete)
- `project_audit_log` — historique des modifications
- `refresh_token` — jetons JWT
- `project_deliverable`, `project_resource(_entry)`, `project_risk`, `project_issue`, `project_mip`, `project_opportunity`, `project_wip`
- `project_management_config`, `project_monthly_forecast`, `project_month_status`
- `project_validation_history`, `project_work_ticket`, `project_work_type`, `project_pending` (workflow d'approbation)

**Triggers / fonctions**
- `fn_calc_project_margin()` — calcule automatiquement `margin_budget` et `project_margin` (revenue - cost, et % de marge)
- `fn_guard_soft_deleted_project()` — empêche la modification d'un projet archivé
- `fn_set_updated_at()` — met à jour automatiquement le timestamp de modification

---

## 5. Déploiement & DevOps

### Docker (local)
- **Backend** : build multi-stage `maven:3.9.6` → `eclipse-temurin:21-jre-alpine`, profil `docker`, port 8080
- **Frontend** : build multi-stage `node:20-alpine` → `nginx:alpine`, sert `dist/saasgestion-front/browser`, port 80
- **docker-compose.yml** : orchestre Postgres + Backend + Frontend avec health checks

### Production
- **Backend → Railway** (`railway.json`) : build via Dockerfile, politique de redémarrage `ON_FAILURE` (3 tentatives max)
- **Frontend → Vercel** (`vercel.json`) : sert `dist/saasgestion-front/browser`, proxy API vers `https://saas-gestion-production.up.railway.app/api`, routing SPA
- **Base de données → Railway** (PostgreSQL managé)

### Variables d'environnement (`.env.example`)

```
BREVO_API_KEY=xkeysib-...       # clé API service d'email
JWT_SECRET=...                  # secret JWT (min 32 caractères)
FRONTEND_URL=...                # URL du frontend (dev ou prod)
MAIL_FROM=...                   # adresse email d'expédition
PGHOST, PGPORT, PGUSER, PGPASSWORD, POSTGRES_DB  # connexion PostgreSQL
```

---

## 6. Fonctionnement de l'application de A à Z

### 6.1 Cycle de vie d'un projet

1. **Création** — un Project Manager crée un projet via `/projects/new`
2. **Attente d'approbation** — le projet passe en statut `PENDING`, un email est envoyé (via Brevo) à l'administrateur avec un lien de validation
3. **Décision admin** — l'administrateur accède à `/admin/approve/:token` (sans besoin de login, sécurisé par token unique) et approuve ou rejette
4. **Si rejeté** — le PM reçoit un lien `/projects/edit/:token` pour corriger et resoumettre le projet
5. **Si approuvé** — le projet passe en statut `On Going` et apparaît sur le dashboard principal et le dashboard admin

### 6.2 Suivi en cours de vie

- **Prévisions mensuelles** (`ProjectMonthlyForecast`, `ProjectMonthStatus`) — suivi budget/revenu mois par mois
- **Marge calculée automatiquement** en base via triggers (`margin_budget`, `project_margin`)
- **Registres associés** à chaque projet :
  - Risques (`risk-register`)
  - Issues (`issue-register`)
  - Opportunités (`opportunity-register`)
  - WIP — travail en cours (`wip-register`)
  - MIP — pool de gestion des problèmes (`mip-register`)

### 6.3 Rôles utilisateurs

| Rôle | Accès |
|---|---|
| `USER` | Accès limité (lecture) |
| `PM` (Project Manager) | Création/gestion de ses projets, registres |
| `BUM` (Business Unit Manager) | Vue sur les projets de sa BU |
| `ADMIN` | Dashboard KPI, approbation des projets, gestion des utilisateurs et des données de référence |

### 6.4 Documents et exports

- **Génération de PDF** — Apache PDFBox côté backend (documents BL avec signature manuscrite via pad de signature intégré), jsPDF côté frontend pour d'autres exports
- **Export Excel** — Apache POI (backend) / xlsx (frontend)

### 6.5 Authentification

- Connexion via `/login` → génération d'un access token JWT (1h) + refresh token (7 jours)
- `JwtAuthInterceptor` (frontend) attache le token à chaque requête sortante
- `JwtAuthFilter` (backend) valide le token à chaque requête entrante
- Changement de mot de passe forcé à la première connexion (`change-password`)

---

## 7. Démarrage en local

```bash
# Backend + BD via Docker
docker-compose up

# Frontend (mode dev)
cd saasgestion-front
npm install
ng serve
```

L'application backend expose l'API sur `http://localhost:8080/api` et la documentation Swagger via Springdoc. Le frontend est accessible sur `http://localhost:4200`.
