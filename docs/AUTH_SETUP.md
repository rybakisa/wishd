# Authentication Setup Guide

This guide walks you through wiring up Google and Apple OAuth via Supabase for both the Android and iOS apps, plus the backend JWT verification.

> **Note on API keys:** Supabase has moved from JWT-based `anon`/`service_role` keys to new **publishable** (`sb_publishable_...`) and **secret** (`sb_secret_...`) keys. This project uses the **publishable key** in the mobile apps (safe to embed in client code). The legacy `anon` key still works during the transition period (until late 2026), but new projects will only have the new key format. See [Supabase API Keys docs](https://supabase.com/docs/guides/api/api-keys) for details.
>
> **Note on JWT signing:** Supabase has moved from shared HS256 secrets to asymmetric signing keys (RS256/ES256) with a JWKS endpoint. The backend verifies tokens using the public key from `https://<project>.supabase.co/auth/v1/.well-known/jwks.json` — no shared secret needed. See [Supabase Signing Keys docs](https://supabase.com/docs/guides/auth/signing-keys) for details.

---

## 1. Create a Supabase Project

1. Go to [supabase.com/dashboard](https://supabase.com/dashboard) and create a new project (or use an existing one).
2. Note these values from **Project Settings > API**:
   - **Project URL** — e.g. `https://abcdefg.supabase.co`
   - **Publishable key** — starts with `sb_publishable_...` (or legacy `anon` key starting with `eyJ...`)

---

## 2. Configure Google OAuth

### 2a. Google Cloud Console

1. Go to [console.cloud.google.com](https://console.cloud.google.com/).
2. Create a project (or select an existing one).
3. Navigate to **APIs & Services > OAuth consent screen**.
   - Choose **External** user type.
   - Fill in app name, support email, and developer contact.
   - Add scopes: `email`, `profile`, `openid`.
   - Publish the app (or add test users while in testing mode).
4. Navigate to **APIs & Services > Credentials > Create Credentials > OAuth client ID**.
   - Choose **Web application** (Supabase uses server-side exchange).
   - Under **Authorized redirect URIs**, add:
     ```
     https://<your-project-ref>.supabase.co/auth/v1/callback
     ```
     Replace `<your-project-ref>` with your Supabase project ref (the subdomain from your project URL).
   - Save and copy the **Client ID** and **Client Secret**.

### 2b. Supabase Dashboard

1. Go to **Authentication > Providers > Google**.
2. Toggle **Enable Google provider** on.
3. Paste the **Client ID** and **Client Secret** from Google Cloud Console.
4. Save.

---

## 3. Configure Apple OAuth

### 3a. Apple Developer Console

1. Go to [developer.apple.com/account](https://developer.apple.com/account).
2. **Register an App ID** (if not already done):
   - Navigate to **Certificates, Identifiers & Profiles > Identifiers**.
   - Create a new **App ID** with "Sign in with Apple" capability enabled.
   - Bundle ID should match your iOS app: `com.wishlist.ios`.
3. **Create a Services ID**:
   - Navigate to **Identifiers > Services IDs > Register**.
   - Description: e.g. "Wishlist App Sign In".
   - Identifier: e.g. `com.wishlist.service`.
   - Enable **Sign in with Apple** and click **Configure**:
     - Primary App ID: select your app.
     - Domains: add `<your-project-ref>.supabase.co`.
     - Return URLs: add `https://<your-project-ref>.supabase.co/auth/v1/callback`.
   - Save.
4. **Create a Key for Sign in with Apple**:
   - Navigate to **Keys > Create a Key**.
   - Name it (e.g. "Wishlist Supabase Key").
   - Enable **Sign in with Apple** and configure it with your Primary App ID.
   - Register the key. Download the `.p8` file and note the **Key ID**.
5. Note your **Team ID** (visible in the top-right of the developer portal, or under Membership).

### 3b. Supabase Dashboard

1. Go to **Authentication > Providers > Apple**.
2. Toggle **Enable Apple provider** on.
3. Fill in:
   - **Service ID (Client ID)**: the Services ID identifier from step 3a (e.g. `com.wishlist.service`).
   - **Secret Key**: paste the full contents of the `.p8` file you downloaded.
   - **Key ID**: from step 3a.4.
   - **Team ID**: from step 3a.5.
4. Save.

---

## 4. Add Redirect URLs in Supabase

1. Go to **Authentication > URL Configuration**.
2. Under **Redirect URLs**, add both:
   ```
   com.wishlist.android://auth
   com.wishlist.ios://auth
   ```
   These match the deep link schemes configured in the Android manifest and iOS app.

---

## 5. Configure the Backend

The backend uses Pydantic Settings — it reads from a `.env` file for local dev, or from environment variables for deployments.

1. Copy the template:
   ```bash
   cp backend/.env.template backend/.env
   ```
2. Edit `backend/.env` with your real values:
   ```env
   DATABASE_URL=postgresql://wishlist:wishlist@localhost:5432/wishlist
   SUPABASE_URL=https://abcdefg.supabase.co
   CORS_ORIGINS=http://localhost:3000,http://10.0.2.2:4000
   ```
   The backend derives the JWKS endpoint from `SUPABASE_URL` and verifies tokens using the published public key (RS256/ES256).

   **Legacy fallback:** If your Supabase project still uses HS256 signing, add `SUPABASE_JWT_SECRET` (found in Project Settings > API > JWT Settings). The backend prefers JWKS when `SUPABASE_URL` is set.

3. Start the backend:
   ```bash
   cd backend
   uvicorn main:app --host 0.0.0.0 --port 4000
   ```

For deployments (Docker, systemd, etc.), set the same variables as environment variables — the `.env` file is only for local convenience.

---

## 6. Configure the Android App

1. Copy the secrets template:
   ```bash
   cp clients/secrets.properties.template clients/secrets.properties
   ```
2. Edit `clients/secrets.properties` with your real values:
   ```properties
   SUPABASE_URL=https://abcdefg.supabase.co
   SUPABASE_PUBLISHABLE_KEY=sb_publishable_abcdef123456...
   API_BASE_URL=http://10.0.2.2:4000
   ```
   - `API_BASE_URL` uses `10.0.2.2` because the Android emulator maps that to the host's `localhost`.
   - For a physical device, use your machine's local IP (e.g. `http://192.168.1.100:4000`).
   - If your Supabase project still uses the legacy key format, the `eyJ...` anon key works here too.
3. Build and run:
   ```bash
   cd clients
   ./gradlew :android:installDebug
   ```

The deep link `com.wishlist.android://auth` is already configured in `AndroidManifest.xml`.

---

## 7. Configure the iOS App

1. Copy the secrets template:
   ```bash
   cp clients/ios/iosApp/Secrets.swift.template clients/ios/iosApp/Secrets.swift
   ```
2. Edit `clients/ios/iosApp/Secrets.swift` with your real values:
   ```swift
   enum Secrets {
       static let supabaseUrl = "https://abcdefg.supabase.co"
       static let supabasePublishableKey = "sb_publishable_abcdef123456..."
       static let apiBaseUrl = "http://localhost:4000"
   }
   ```
   - For a physical device, replace `localhost` with your machine's local IP.
3. **Add URL Scheme in Xcode**:
   - Open the Xcode project.
   - Select your app target > **Info** tab > **URL Types**.
   - Click **+** and add:
     - **Identifier**: `com.wishlist.ios`
     - **URL Schemes**: `com.wishlist.ios`
     - **Role**: Editor
   - This tells iOS to open your app when a `com.wishlist.ios://` URL is triggered.
4. Build and run from Xcode.

---

## 8. Verify the Full Flow

1. **Start the backend** with `SUPABASE_JWT_SECRET` set.
2. **Launch the app** (Android emulator or iOS simulator).
3. Tap **"Continue with Google"** or **"Continue with Apple"**.
4. The browser/sheet opens for OAuth consent.
5. After granting access, the browser redirects to `com.wishlist.android://auth` (or `com.wishlist.ios://auth`).
6. The app intercepts the redirect, Supabase SDK parses the tokens, and the session is established.
7. The app calls `POST /auth/session` on the backend to sync the user profile.
8. You are now authenticated — API calls include the Supabase JWT automatically, and token refresh happens transparently.

---

## Troubleshooting

### "Invalid token" errors on the backend
- Verify `SUPABASE_URL` is set correctly (e.g. `https://abcdefg.supabase.co`). The backend fetches signing keys from `<SUPABASE_URL>/auth/v1/.well-known/jwks.json`.
- If using the legacy HS256 path, verify `SUPABASE_JWT_SECRET` matches the value in Supabase dashboard (Project Settings > API > JWT Settings).

### OAuth redirect doesn't open the app
- **Android**: Confirm `com.wishlist.android://auth` is in the Supabase redirect URLs AND in `AndroidManifest.xml`.
- **iOS**: Confirm `com.wishlist.ios://auth` is in the Supabase redirect URLs AND the URL scheme is registered in Xcode (Info > URL Types).

### Google sign-in shows "redirect_uri_mismatch"
- Verify the authorized redirect URI in Google Cloud Console matches exactly: `https://<your-ref>.supabase.co/auth/v1/callback`.

### Apple sign-in fails with "invalid_client"
- Verify the Services ID, Key ID, Team ID, and `.p8` private key are all correct in Supabase dashboard.
- Ensure the return URL in the Apple Services ID configuration matches: `https://<your-ref>.supabase.co/auth/v1/callback`.

### Token expires and user gets logged out
- The Supabase SDK auto-refreshes tokens. If you still get 401s, check that `SUPABASE_URL` is set correctly on the backend (it fetches signing keys from the JWKS endpoint).

### Legacy anon key vs publishable key
- Both work with the Supabase Kotlin SDK. New projects issue `sb_publishable_...` keys. Existing projects may still show the `eyJ...` anon key — it remains valid until late 2026.

---

## File Reference

| File | Purpose |
|------|---------|
| `clients/secrets.properties` | Android: Supabase URL, publishable key, API URL (gitignored) |
| `clients/secrets.properties.template` | Template for the above (committed) |
| `clients/ios/iosApp/Secrets.swift` | iOS: Supabase URL, publishable key, API URL (gitignored) |
| `clients/ios/iosApp/Secrets.swift.template` | Template for the above (committed) |
| `backend/.env` | Backend secrets: `SUPABASE_URL`, `DATABASE_URL`, `CORS_ORIGINS` (gitignored) |
| `backend/.env.template` | Template for the above (committed) |
| `backend/config.py` | Pydantic Settings — loads from `.env` or env vars |
| `clients/android/src/main/AndroidManifest.xml` | Android deep link intent filter |
| `clients/ios/iosApp/WishlistApp.swift` | iOS `.onOpenURL` handler |
