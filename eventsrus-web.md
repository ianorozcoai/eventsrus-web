# EventsRUs Web
**Project:** `eventsrus-web` | **Stack:** Spring Boot + Thymeleaf | **Repo:** `github.com/ianorozcoai/eventsrus-web`
**Parent doc:** `claude.md` (project root) | **Status of this file:** written from a code scan, not from the original author; correct anything wrong.

## Role
The browser-facing site. It has no database. Every data call goes to `eventsrus-backend` through `BackendClient` (package `com.web.eventsrus.backend`). Runs on port 8081 locally (backend is 8080).

## What it contains
- Public pages: landing, `privacy`, `terms`, storefront pages for suppliers.
- Planner and supplier screens mirroring the mobile app, plus the **admin** area (suppliers, subscription payment review, SMS sending).
- **Subscriptions:** `VendorSubscriptionWebController` and `PayPalReturnWebController` handle the PayPal checkout return. The paywall templates are in `templates/vendor/subscription.html` and `fragments/common.html`.
- **Google Calendar consent:** `GoogleCalendarOAuthController` runs the browser consent redirect (scope `calendar.events`). The mobile app opens this site for it (`AppConfig.webBaseUrl`).
- Support contact: `support@eventsrus.app`.

## Deployment
Web and backend are both hosted on Railway, project `eventsrus`, single `production` environment (confirmed from Ian's Railway screenshot, 2026-10-16).
- Web: https://eventsrus.app
- Backend API: https://api.eventsrus.app
- DNS chain: the domain `eventsrus.app` is registered at GoDaddy, its nameservers point to Cloudflare (DNS and the R2 CDN at `cdn.eventsrus.app`), and Cloudflare records point to Railway. A DNS or domain change must be done in Cloudflare, not GoDaddy (reported by Ian, 2026-10-16).
- Production database: a Postgres service with a volume inside the same Railway project (not Supabase).

## Related
- Backend API: `../eventsrus-backend/eventsrus-backend.md`
- Mobile: `../eventsrus-ui/eventsrus-ui.md`
