# 🚀 Free Cloud Deployment Guide: Railway + Vercel

This guide walks you through deploying the **Distributed Job Scheduler** for free using:
1. **Railway** (`railway.app`) $\rightarrow$ **PostgreSQL Database + Keycloak Auth + Spring Boot Backend**
2. **Vercel** (`vercel.com`) $\rightarrow$ **Next.js Frontend Dashboard**

---

## 📋 Architecture Overview
```
┌──────────────────────────────────────┐       ┌────────────────────────────────────────────────────────┐
│           VERCEL (Free)              │       │                    RAILWAY (Free)                      │
│                                      │       │                                                        │
│  [ Next.js Frontend Dashboard ]      │──────>│  [ Spring Boot Backend Engine ] (Port 8080)            │
│  (https://your-app.vercel.app)       │       │                      │                                 │
│                                      │──────>│  [ Keycloak OIDC ] ──┤                                 │
│                                      │       │  (Port 8080/8180)    │                                 │
│                                      │       │                      ▼                                 │
│                                      │       │              [ PostgreSQL DB ]                         │
└──────────────────────────────────────┘       └────────────────────────────────────────────────────────┘
```

---

## 🛠️ Step 1: Push Code to GitHub

If you haven't pushed your code to GitHub yet:
1. Create a new repository on [GitHub](https://github.com/new) (e.g. `distributed-job-scheduler`).
2. In your local terminal, initialize and push:
```powershell
cd C:\Users\KAVIYA\.gemini\antigravity-ide\scratch\distributed-job-scheduler
git init
git add .
git commit -m "feat: complete distributed job scheduler platform"
git branch -M main
git remote add origin https://github.com/<your-username>/distributed-job-scheduler.git
git push -u origin main
```

---

## 🚂 Step 2: Deploy Backend, Database & Keycloak on Railway

1. **Create Account**: Go to **[Railway.app](https://railway.app)** and log in with your GitHub account.
2. **Create New Project**: Click **"New Project"** $\rightarrow$ select **"Empty Project"**.

### 2.1 Provision Free PostgreSQL Database
1. Inside your Railway project canvas, click **"+ Create"** (or press `Ctrl + K`) $\rightarrow$ select **Database** $\rightarrow$ **PostgreSQL**.
2. Railway will spin up your PostgreSQL instance instantly.
3. Click on the PostgreSQL card $\rightarrow$ Go to the **Variables** tab $\rightarrow$ Note `DATABASE_URL`, `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE`.

---

### 2.2 Deploy Keycloak on Railway
1. Click **"+ Create"** $\rightarrow$ **Docker Image**.
2. Enter image name: `quay.io/keycloak/keycloak:24.0.5` $\rightarrow$ Press Enter.
3. Click on the Keycloak service card $\rightarrow$ Go to **Settings**:
   - Under **Networking**, click **"Generate Domain"** (e.g. `https://keycloak-production-xxxx.up.railway.app`).
4. Go to **Variables** tab $\rightarrow$ Add:
   - `KEYCLOAK_ADMIN` = `admin`
   - `KEYCLOAK_ADMIN_PASSWORD` = `admin123`
   - `KC_PROXY` = `edge`
   - `KC_HOSTNAME_STRICT` = `false`
   - `KC_HTTP_ENABLED` = `true`
5. Go to **Deploy** $\rightarrow$ Under **Custom Start Command**, enter:
   ```bash
   start-dev --import-realm
   ```
6. Open your Keycloak domain $\rightarrow$ Log in with `admin` / `admin123` $\rightarrow$ Click **Create Realm** $\rightarrow$ Browse and import `keycloak/realm-export.json`.

---

### 2.3 Deploy Spring Boot Backend on Railway
1. Click **"+ Create"** $\rightarrow$ **GitHub Repo** $\rightarrow$ Select your `distributed-job-scheduler` repository.
2. Click on the Backend service card $\rightarrow$ Go to **Settings**:
   - **Root Directory**: `backend`
   - **Dockerfile Path**: `backend/Dockerfile`
   - Under **Networking**, click **"Generate Domain"** (e.g. `https://backend-production-xxxx.up.railway.app`).
3. Go to **Variables** tab $\rightarrow$ Add:
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}`
   - `SPRING_DATASOURCE_USERNAME` = `${{Postgres.PGUSER}}`
   - `SPRING_DATASOURCE_PASSWORD` = `${{Postgres.PGPASSWORD}}`
   - `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` = `https://<your-keycloak-domain>/realms/distributed-scheduler-realm`
   - `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` = `https://<your-keycloak-domain>/realms/distributed-scheduler-realm/protocol/openid-connect/certs`
4. Click **Deploy**. Railway will build your Java 21 Spring Boot container and start the background worker engine!

---

## ⚡ Step 3: Deploy Frontend on Vercel

1. **Create Account**: Go to **[Vercel.com](https://vercel.com)** and log in with your GitHub account.
2. **Add New Project**: Click **"Add New..."** $\rightarrow$ **Project**.
3. **Import Repository**: Select your `distributed-job-scheduler` GitHub repository.
4. **Configure Project**:
   - **Framework Preset**: Next.js
   - **Root Directory**: Click **Edit** $\rightarrow$ Select **`frontend`**.
5. **Environment Variables**:
   Add the following two environment variables:
   - `NEXT_PUBLIC_API_URL` = `https://<your-railway-backend-domain>/api/v1`
   - `NEXT_PUBLIC_KEYCLOAK_URL` = `https://<your-railway-keycloak-domain>`
6. Click **"Deploy"**.
7. In ~1 minute, your production frontend dashboard will be live at:
   👉 `https://<your-project-name>.vercel.app`!

---

## 🔒 Step 4: Add Vercel Domain to CORS in Backend

In your Railway backend variables or `backend/src/main/resources/application.yml`, ensure the Vercel domain is allowed in CORS:
- `app.cors.allowed-origins` = `https://<your-project-name>.vercel.app`

---

## 🎉 Done!
Your **Distributed Job Scheduler** is now fully deployed and running in the cloud with:
- Live PostgreSQL concurrency locking
- Cloud Keycloak JWT authentication
- Automated worker polling and execution
- Global Next.js dashboard accessible from anywhere!
