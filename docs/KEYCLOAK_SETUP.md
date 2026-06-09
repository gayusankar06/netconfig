# Enterprise Keycloak & Google OAuth Federation Setup Guide

This guide details how to spin up the containerized Keycloak server, configure it as your central Identity Provider, and federate it with Google OAuth to enable Enterprise SSO.

## 1. Startup Instructions

First, ensure your main application network exists:
```bash
docker network create netconfig_network || true
```

Then, boot the Keycloak environment:
```bash
docker-compose -f docker-compose.keycloak.yml up -d
```

Keycloak will be accessible at: `http://localhost:8080`
- **Username:** `admin`
- **Password:** `AdminSecure2024!` (or what you configured in your `.env` / compose file)

## 2. Keycloak Realm Configuration

1. Log into the Keycloak Admin Console (`http://localhost:8080/admin`).
2. Hover over the top-left dropdown (currently says "master") and click **Create Realm**.
3. Set **Realm name** to `netconfig-realm` and click Create.

## 3. Client Creation (For React & FastAPI)

1. In the `netconfig-realm`, go to **Clients** -> **Create client**.
2. **Client ID:** `netconfig-frontend`
3. Click **Next**.
4. Enable **Standard flow** (for React) and **Direct access grants**.
5. Set **Valid redirect URIs** to: `http://localhost:3000/*`
6. Set **Web origins** to: `http://localhost:3000`
7. Click **Save**.

## 4. Google Cloud Console Setup

1. Go to the [Google Cloud Console](https://console.cloud.google.com).
2. Navigate to **APIs & Services** > **Credentials**.
3. Click **Create Credentials** > **OAuth client ID**.
4. Application Type: **Web application**.
5. **Authorized redirect URIs:** 
   ```text
   http://localhost:8080/realms/netconfig-realm/broker/google/endpoint
   ```
6. Click **Create** and copy your **Client ID** and **Client Secret**.

## 5. Keycloak Google Identity Provider Setup

1. Back in the Keycloak Admin Console (inside `netconfig-realm`), go to **Identity providers**.
2. Click **Google**.
3. Paste the **Client ID** and **Client Secret** obtained from Google Cloud.
4. Set **Trust Email** to `ON` (to automatically verify Google emails).
5. Set **First Login Flow** to `first broker login` (this syncs the user into Keycloak automatically).
6. Click **Save**.

### 5.1 Identity Provider Mappers (User Synchronization)
To pull the avatar and name properly:
1. In the Google Identity Provider settings, go to the **Mappers** tab.
2. Click **Add mapper**.
3. **Name:** `profile_picture`
   - **Mapper Type:** `Attribute Importer`
   - **Social Profile JSON Field Path:** `picture`
   - **User Attribute Name:** `picture`
4. Click **Save**.

## 6. Enterprise RBAC Roles Setup

1. In Keycloak, go to **Realm roles**.
2. Click **Create role** for each of the following:
   - `NETWORK_ENGINEER`
   - `REVIEWER`
   - `APPROVER`
   - `SECURITY_MANAGER`
   - `AUDITOR`
   - `ADMIN`
3. *(Optional)* Go to **Groups**, create matching groups, and assign the realm roles to the groups for easier management. Then, assign users to groups.

## 7. Connecting FastAPI to Keycloak

The FastAPI backend will now validate JWT tokens issued by Keycloak. 
Make sure your `.env` for the backend contains:
```env
KEYCLOAK_SERVER_URL=http://localhost:8080
KEYCLOAK_REALM=netconfig-realm
KEYCLOAK_CLIENT_ID=netconfig-frontend
```
