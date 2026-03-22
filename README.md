# AlfredJenny

Un'app Android con assistente AI multi-provider (Alfred) e companion personalizzata (Jenny), con integrazione smart home Tuya, memoria a lungo termine e voce ElevenLabs.

---

## Architettura

```
AlfredJenny/
├── backend/          # FastAPI (Python)  →  deploy su Railway
└── app/              # Android (Kotlin + Jetpack Compose)
```

**Backend stack:** FastAPI · Python 3.12 · OpenAI / Anthropic / Gemini · ElevenLabs · Tuya Cloud API
**Android stack:** Kotlin · Jetpack Compose · Hilt · Room · Retrofit · OkHttp SSE · DataStore

---

## 1. Backend — Setup locale

### Prerequisiti
- Python 3.12+
- pip

### Installazione

```bash
cd backend
pip install -r requirements.txt
cp .env.example .env
# Modifica .env con le tue chiavi
uvicorn main:app --reload --port 8000
```

L'API è ora disponibile su `http://localhost:8000`.
Documentazione interattiva: `http://localhost:8000/docs`

### Variabili d'ambiente (`.env`)

| Variabile | Obbligatoria | Descrizione |
|---|---|---|
| `JWT_SECRET` | ✅ | Chiave segreta per JWT (usa una stringa lunga casuale) |
| `JWT_EXPIRE_MINUTES` | — | Durata sessione in minuti (default: 1440) |
| `USER_USERNAME` | ✅ | Username utente normale (default: `utente`) |
| `USER_PASSWORD` | ✅ | Password utente normale (default: `1234`) |
| `ADMIN_USERNAME` | ✅ | Username amministratore (default: `admin`) |
| `ADMIN_PASSWORD` | ✅ | Password amministratore |
| `ACTIVE_PROVIDER` | — | Provider AI attivo: `openai` / `anthropic` / `gemini` (default: `openai`) |
| `OPENAI_API_KEY` | ⚠️ | Chiave API OpenAI |
| `ANTHROPIC_API_KEY` | ⚠️ | Chiave API Anthropic (Claude) |
| `GEMINI_API_KEY` | ⚠️ | Chiave API Google Gemini |
| `ELEVENLABS_API_KEY` | — | Chiave ElevenLabs TTS (lato server, opzionale) |
| `TUYA_CLIENT_ID` | — | Client ID Tuya IoT Platform |
| `TUYA_CLIENT_SECRET` | — | Client Secret Tuya IoT Platform |
| `TUYA_BASE_URL` | — | URL base Tuya per regione (default: EU) |
| `CORS_ORIGINS` | — | Origini CORS ammesse, separate da virgola (default: `*`) |

> ⚠️ Almeno uno tra `OPENAI_API_KEY`, `ANTHROPIC_API_KEY` o `GEMINI_API_KEY` è richiesto.

---

## 2. Backend — Deploy su Railway

### Passo 1 — Crea il progetto

1. Vai su [railway.app](https://railway.app) e accedi con GitHub.
2. Click **New Project → Deploy from GitHub repo**.
3. Seleziona il repository `alfred-jenny-app`.

### Passo 2 — Configura la root directory

Nella schermata del servizio:
**Settings → Source → Root Directory** → imposta `backend`

Railway userà il `Dockerfile` presente in `backend/` per la build.

### Passo 3 — Variabili d'ambiente su Railway

Vai su **Variables** nel pannello del servizio e aggiungi:

```
JWT_SECRET          = <stringa-casuale-32-char>
USER_USERNAME       = utente
USER_PASSWORD       = <password-sicura>
ADMIN_USERNAME      = admin
ADMIN_PASSWORD      = <password-admin-sicura>
ACTIVE_PROVIDER     = openai
OPENAI_API_KEY      = sk-...
ANTHROPIC_API_KEY   = sk-ant-...
GEMINI_API_KEY      = AIza...
ELEVENLABS_API_KEY  = <opzionale>
TUYA_CLIENT_ID      = <opzionale>
TUYA_CLIENT_SECRET  = <opzionale>
TUYA_BASE_URL       = https://openapi.tuyaeu.com
CORS_ORIGINS        = *
```

### Passo 4 — Deploy e health check

Dopo il deploy, Railway esegue il controllo su `GET /health`.
L'URL pubblico sarà nel formato: `https://<nome-app>.railway.app`

Verifica che funzioni:
```bash
curl https://<nome-app>.railway.app/health
# → {"status":"healthy","active_provider":"openai"}
```

---

## 3. App Android — Primo avvio (Onboarding)

Al primo avvio l'app mostra un onboarding in 3 step:

1. **URL Backend** — inserisci l'URL Railway (es. `https://my-app.railway.app`) e premi **Test** per verificare la connessione.
2. **Login** — inserisci username e password configurati nel `.env`.
3. **Chiave API** *(opzionale)* — inserisci una chiave AI lato client. Puoi saltare questo step se le chiavi sono configurate server-side.

### Build da sorgente

```bash
# Clona il repo
git clone https://github.com/Tulliz31/alfred-jenny-app.git
cd alfred-jenny-app

# Apri il progetto in Android Studio
# oppure compila da riga di comando:
./gradlew assembleDebug

# APK in: app/build/outputs/apk/debug/app-debug.apk
```

**Requisiti:** Android Studio Hedgehog+ · JDK 17 · Android SDK API 26+

---

## 4. Funzionalità principali

### Chat AI multi-provider
- Streaming word-by-word con animazione avatar
- Fallback automatico: OpenAI → Anthropic → Gemini
- Notifica amber quando si attiva il fallback

### Memoria a lungo termine
- Riassunto conversazioni ogni N messaggi (configurabile)
- Contesto iniettato nelle sessioni future
- Storage locale con Room DB

### Voce (ElevenLabs)
- TTS si attiva dopo la prima frase completa (latenza ridotta)
- Voce distinta per Alfred e Jenny
- Modalità "Casa" (sempre in ascolto) e "Outdoor" (press-to-talk)

### Smart Home (Tuya)
- Griglia dispositivi con stato in tempo reale (polling 10s)
- Toggle on/off e slider luminosità
- Alfred controlla i dispositivi via linguaggio naturale
- Configura le credenziali Tuya in **Impostazioni → Smart Home** (sezione admin)

### Companion Jenny *(admin only)*
- Personalità configurabile (livello 1–5)
- Voce dedicata
- Sezione nascosta, sbloccata con triplo tap → password admin

---

## 5. Struttura API principali

| Metodo | Endpoint | Descrizione |
|---|---|---|
| `GET` | `/health` | Health check |
| `POST` | `/auth/login` | Login → JWT token |
| `POST` | `/chat` | Chat non-streaming |
| `POST` | `/chat/stream` | Chat SSE streaming |
| `POST` | `/chat/summarize` | Riassunto conversazione |
| `GET` | `/providers` | Lista provider AI |
| `PUT` | `/providers/active` | Cambia provider attivo |
| `GET` | `/companions` | Lista companion disponibili |
| `GET` | `/devices` | Lista dispositivi smart home |
| `GET` | `/devices/{id}/status` | Stato dispositivo |
| `POST` | `/devices/{id}/command` | Invia comando (`on`/`off`/`brightness`) |
| `POST` | `/devices/discover` | Riscopri dispositivi (admin) |

---

## 6. Tuya Smart Home — Setup

1. Registrati su [iot.tuya.com](https://iot.tuya.com)
2. Crea un progetto **Smart Home**
3. Copia `Client ID` e `Client Secret`
4. Aggiungi i tuoi dispositivi al progetto Tuya
5. Imposta le variabili nel `.env` (o Railway Variables):
   ```
   TUYA_CLIENT_ID     = ...
   TUYA_CLIENT_SECRET = ...
   TUYA_BASE_URL      = https://openapi.tuyaeu.com   # EU region
   ```
6. Nell'app: **Impostazioni → Smart Home → Scopri dispositivi**
7. Abilita il tab "Casa" con il toggle **Abilita Smart Home**

**Region URLs:**
| Regione | URL |
|---|---|
| Europa | `https://openapi.tuyaeu.com` |
| USA | `https://openapi.tuyaus.com` |
| Cina | `https://openapi.tuyacn.com` |
| India | `https://openapi.tuyain.com` |

---

## 7. Licenza

MIT License — vedi [LICENSE](LICENSE)
