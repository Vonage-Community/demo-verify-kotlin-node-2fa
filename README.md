# Silent Authentication + SMS Fallback Demo

This repository contains a full‑stack demo app that implements Silent
Authentication (2FA) with a SMS fallback when silent auth cannot be completed.
It’s intended as a developer reference and demo for integrating Silent
Authentication flows with a mobile client and a Node.js backend using Vonage
services.

## Overview

This demo shows how to:

- Use a Node.js server to orchestrate [Verify API](https://developer.vonage.com/en/verify/overview) calls with the [Vonage Node SDK](https://github.com/vonage/vonage-node-sdk).

- Implement an Android client (Kotlin) that triggers the silent auth flow and falls back to SMS when needed.

- Use the [Vonage Client Library](https://github.com/Vonage/vonage-android-client-library) for Android to ensure the silent auth can work even when the device is connected to Wi‑Fi. 

The purpose of the repository is educational: to provide a complete example of the end‑to‑end flow and the pieces you need to adapt for production.

## Features

* [Silent authentication](https://developer.vonage.com/en/verify/concepts/silent-authentication) as the primary 2FA method (no OTP required when successful).

* SMS fallback: if silent auth fails or cannot be attempted (no mobile network, unsupported operator, etc.), the system falls back to a traditional SMS OTP.

* Simple UI for demo purposes and clear server logs for each step.

## Prerequisites

* Node.js (v14+ recommended)

* npm or yarn

* Android Studio (for building the client) and an Android device (recommended) or emulator with mobile network simulation

* A [Vonage developer](https://developer.vonage.com) account.

* An application set up in the [Vonage Dashboard](https://developer.vonage.com/dashboard) with the `Network Registry` capability enabled.

## Server (NodeJS backend)

Create a .env file (or set environment variables) for the server. The file contains the variables needed to [generate a JWT](https://developer.vonage.com/en/verify/concepts/authentication#jwt) during the API authentication phase: 

```
VONAGE_APP_ID=123456abcd
VONAGE_PRIVATE_KEY=/path/to/my/private.key
```

### Build & Run

From the `server` folder:

```
# install dependencies
npm install


# run in development
npm run start 
```

## Client (Android app)

Open the project in Android Studio and sync Gradle to download dependencies.

Important runtime requirement for silent auth: the device must have mobile data
available (the demo uses the Vonage client library to prioritize/bypass Wi‑Fi
during the silent auth attempt). For reliable testing use a physical device
with a mobile data connection.

### Build & Run

* Open the `client` folder in Android Studio.

* Configure `BASE_URL` with the URL where your backend is running.

* Build and install on a device (or emulator).

* Start the app, enter a phone number and follow the UI to start verification.


## Authentication flow

### Silent Authentication (primary)

1. The client sends a request to the backend to start verification with the user's phone number.

2. The backend calls Vonage Verify API to request silent authentication for that phone number.

3. Vonage coordinates with the mobile operator to attempt a silent network‑based authentication (no OTP required if successful).

4. The mobile operator returns an auth indication to Vonage which triggers a callback to your server (webhook). 

5. The server marks the user verified and the client proceeds.

### SMS fallback

If silent auth cannot be completed (unsupported operator, no mobile data, user
device not compatible, timeout, or explicit failure), the backend will request
a traditional SMS OTP from Vonage and the client will present an input for the
user to enter the OTP.

Flow (brief):

1. Silent auth attempt fails or times out.

2. Backend requests an SMS verification to the same phone number.

3. User receives OTP via SMS and enters it in the client UI.

4. Client sends OTP to backend for validation; backend verifies with Vonage and completes the flow.

## Testing recommendations

* Use a real Android device with a mobile data connection for the silent auth path. Emulators may not faithfully replicate all mobile‑network behaviors.

* Use `ngrok` (or similar) to expose your local server to the internet for Vonage callbacks during development.

## Security considerations

* Protect all secrets (API key/secret, private keys). Do not commit them to your git repo.

* Validate and authenticate incoming webhooks from Vonage (verify signatures if provided).

* Implement rate limiting and abuse protections on the start endpoint to avoid malicious usage.

## License

This project is provided for demonstration purposes and is licensed under the MIT License. See the LICENSE file for details.

## Contact

If you want help adapting this demo to your own product or want improvements, open an issue or contact `devrel@vonage.com`

