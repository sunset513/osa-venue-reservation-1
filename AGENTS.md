# Repository Guidelines

## Project Structure & Module Organization
This repo has a Vue 3 frontend and a Spring Boot backend. Frontend code lives in `frontend/src`, with API clients in `src/api`, route definitions and guards in `src/router`, page-level views in `src/views`, reusable UI in `src/components`, shared pure helpers in `src/utils`, reusable SCSS partials in `src/assets/styles`, and the active global stylesheet in `src/style/style.css`. Frontend tests live beside utilities under `src/utils/__tests__`. Backend code lives in `backend/venue-reservation-service/src/main/java/tw/edu/ncu/osa/venue_reservation_service`, organized by `controller`, `service`, `mapper`, `model`, `config`, `util`, and `exception`. MyBatis XML files live in `src/main/resources/mapper`. Root-level `docker-compose.yml` starts MySQL, backend, and frontend; `venue_seed_v1.sql` seeds initial data.

## Build, Test, and Development Commands
- `docker compose up -d --build`: start the full local stack.
- `docker compose logs -f backend`: follow backend logs for debugging.
- `cd frontend && pnpm install && pnpm dev`: run the Vite frontend locally.
- `cd frontend && pnpm build`: create the production frontend bundle.
- `cd frontend && pnpm test`: run frontend Vitest unit tests once.
- `cd frontend && pnpm test:watch`: run frontend Vitest in watch mode.
- `cd backend/venue-reservation-service && ./mvnw spring-boot:run`: run the API locally.
- `cd backend/venue-reservation-service && ./mvnw test`: run backend tests.
- `cd backend/venue-reservation-service && ./mvnw package`: build the backend JAR.

## Coding Style & Naming Conventions
Frontend uses ES modules, Vue SFCs, and 2-space indentation. Use PascalCase for component files such as `VenueCalendar.vue` and camelCase for utilities such as `dateHelper.js`. Keep API request details in `src/api`, route validation in `src/router/guards.js`, page orchestration in `src/views`, and reusable modal or display blocks in `src/components`. Put pure shared helpers in `src/utils`; keep `dateHelper.js` focused on slot/time conversion and calendar event colors, `bookingMeta.js` focused on booking display metadata, and `calendarDisplay.js` focused on calendar display helpers. Backend uses Java 17, Spring Boot, and MyBatis; keep package names lowercase and class names PascalCase such as `BookingController`. Match the surrounding file style exactly; no dedicated lint or formatter config is committed.

## Testing Guidelines
Backend tests use JUnit via Spring Boot test support under `backend/venue-reservation-service/src/test/java`. Name new tests `*Tests.java` and add service or controller coverage for behavior changes, not just context loading. Frontend tests use Vitest and currently focus on pure utilities under `frontend/src/utils/__tests__`; name new frontend tests `*.test.js` and run `cd frontend && pnpm test` before handing off changes. For UI behavior changes, also verify the affected flow manually against the Vite dev server or Docker stack and document what you checked in the PR.

## Common Troubleshooting
- If PowerShell refuses `pnpm` with an execution-policy error, run the command through `corepack pnpm ...` or invoke the binary directly with `frontend\\node_modules\\.bin\\vitest.cmd`.
- If Vitest fails before tests start with a Rolldown or native binding error such as `@rolldown/binding-win32-x64-msvc`, rebuild the frontend dependencies with `cd frontend && corepack pnpm install --force`, then retry the test command.
- If a reviewer deep link does not prefill the expected filters, check that the route includes the intended query keys, especially `mode=equipment`, `equipmentKeyword`, and `equipmentStatus=all`.

## Commit & Pull Request Guidelines
Recent history mixes plain summaries and Conventional Commit style (`feat: ...`). Prefer short, imperative commit messages with a type prefix such as `feat:`, `fix:`, or `docs:`. Keep each commit scoped to one change. PRs should include a concise summary, affected areas (`frontend`, `backend`, `docker`), setup notes, linked issues if available, and screenshots or API examples for user-facing changes.

## Configuration & Security Tips
Do not commit local secrets. Create the root `.env`, `frontend/.env.development`, and backend `application-dev.yaml` as described in `README.md`. For local mock auth, send `Authorization: mock-token-123` and keep environment-specific ports and database credentials outside tracked files.
