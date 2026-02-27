## WeeChat - raport funkcjonalny i bezpieczeństwa

### 1) Cel i charakter aplikacji
WeeChat to aplikacja czatu z naciskiem na bezpieczeństwo dzieci w sieci. Wsparcie AI realizuje moduł moderacji wiadomości (PL-Guard), który klasyfikuje treści i podpowiada decyzje lub automatycznie akceptuje/odrzuca wiadomości zależnie od poziomu moderacji.

### 2) Architektura (wysoki poziom)
- **Frontend**: React/TypeScript (SPA) z obsługą logowania rodzica i dziecka, czatu w czasie rzeczywistym i paneli moderacji.
- **Backend**: Spring Boot (REST + WebSocket STOMP).
- **Bazy danych**:
  - **PostgreSQL**: dane kont, relacji rodzic-dziecko, znajomości.
  - **MongoDB**: wątki i wiadomości czatu.
- **AI Moderation**: zewnętrzny serwis PL-Guard (HTTP API `/moderate`), używany asynchronicznie do oceny wiadomości.

### 3) Backend - endpointy REST API
**Uwierzytelnianie**
- `POST /api/auth/login` - logowanie rodzica.
- `POST /api/auth/refresh` - odświeżenie tokenu.
- `POST /api/auth/logout` - wylogowanie (czyszczenie cookies).
- `POST /api/children/login` - logowanie dziecka kodem.
- `POST /api/children/refresh` - odświeżenie sesji dziecka.

**Zarządzanie dziećmi**
- `GET /api/children` - lista dzieci przypisanych do rodzica.
- `POST /api/children` - tworzenie profilu dziecka.
- `PUT /api/children/{childId}/moderation-level` - poziom moderacji (NONE/AUTOMATED/MANUAL).
- `DELETE /api/children/{childId}` - usunięcie dziecka.
- `POST /api/children/{childId}/login-qr-token` - token QR (TTL 60s).
- `POST /api/children/{childId}/login-code` - kod tekstowy logowania (TTL 30s).
- Kody logowania są hashowane (bcrypt), a kod tekstowy ma 6 cyfr.

**Znajomi**
- `GET /api/friends` - lista znajomych.
- `GET /api/friends/search?email=...` - wyszukiwanie po emailu.
- `POST /api/friends/requests` - prośba o znajomość.
- `GET /api/friends/requests/pending` - oczekujące prośby (dla rodzica i jego dzieci).
- `POST /api/friends/requests/{requestId}/accept` - akceptacja.
- `POST /api/friends/requests/{requestId}/reject` - odrzucenie.
- Statusy relacji: `PENDING`, `ACCEPTED`, `REJECTED`.

**Wątki i wiadomości**
- `GET /api/threads` - lista wątków.
- `POST /api/threads/direct/messages` - start rozmowy bezpośredniej (tworzy wątek, jeśli nie istnieje).
- `GET /api/threads/{threadId}/messages` - lista wiadomości (paginacja wsteczna, `before`, limit do 50).
- `POST /api/threads/{threadId}/messages` - wysłanie wiadomości.

**Moderacja treści**
- `POST /api/messages/{messageId}/approve` - zatwierdzenie.
- `POST /api/messages/{messageId}/reject` - odrzucenie.
- `GET /api/children/{childId}/moderation-queue` - kolejka moderacji dziecka.

### 4) Backend - WebSocket (STOMP)
- **Endpoint**: `/ws` (handshake).
- **Prefix aplikacji**: `/app`, **broker**: `/topic`, `/queue`, **user**: `/user`.
- **Publikacja**: `/app/threads/list` (żądanie listy wątków).
- **Subskrypcje**:
  - `/user/queue/threads` - lista wątków użytkownika.
  - `/user/queue/messages` - wiadomości w czasie rzeczywistym.
 - **Autoryzacja**: token z nagłówka `Authorization` lub z cookie podczas handshake.

### 5) Uwierzytelnianie i zabezpieczenia (backend)
- **JWT (HS256)** z krótkim access tokenem i refresh tokenem:
  - Implementacja oparta o bibliotekę JJWT (`io.jsonwebtoken`).
  - Domyślne TTL: access 300s, refresh 2592000s (30 dni).
- **Cookies**:
  - Nazwy: `weechat_access`, `weechat_refresh` (konfigurowalne).
  - Flagi: HttpOnly, SameSite=Lax, Secure konfigurowalne.
  - `POST /api/auth/logout` czyści oba cookies (maxAge=0).
- **Obsługa tokenów**:
  - Access token odczytywany z nagłówka `Authorization: Bearer` lub z cookie.
  - Refresh token odczytywany z body lub z cookie.
- **Publiczne endpointy**:
  - `/api/auth/**`, `POST /api/children/login`, `/ws/**`, dokumentacja swagger.
- **CORS**: konfiguracja oparta o `security.cors.allowed-origins`.
- **WebSocket**: autoryzacja JWT w interceptorze STOMP (CONNECT).
- **Spring Security**: tryb stateless, CSRF wyłączony, filtr JWT w łańcuchu.
- **Autoryzacja domenowa**:
  - Dostęp do wątków kontrolowany przez członkostwo w wątku.
  - Moderacja wiadomości wymaga bycia rodzicem powiązanym z dzieckiem z wątku.

### 6) Moderacja i AI (PL-Guard)
- Wiadomości otrzymują status moderacji przy zapisie (`PENDING`, `APPROVED`, `REJECTED`).
- Poziomy moderacji dziecka:
  - `NONE` - auto-akceptacja.
  - `AUTOMATED` - decyzja automatyczna na podstawie PL-Guard.
  - `MANUAL` - decyzja rodzica w panelu.
- Integracja PL-Guard:
  - Zapytanie `POST /moderate` z treścią wiadomości.
  - Etykieta `safe` -> `APPROVED`, inne -> `REJECTED`.
  - Błąd lub timeout -> pozostaje `PENDING`.
- Moderacja wykonywana asynchronicznie (`@Async`, dedykowany executor).
- Kolejka moderacji: do 25 wątków i do 40 wiadomości na wątek.
- Po zatwierdzeniu wiadomość jest wysyłana do odbiorców w czasie rzeczywistym.

### 7) Backend - logika domenowa i reguły dostępu
- Warstwy: `api` (kontrolery), `application` (serwisy), `domain` (encje), `infrastructure` (repozytoria/integracje).
- Relacje danych: rodzic-dziecko wiele-do-wielu (tabela `parent_child`).
- Dziecko domyślnie otrzymuje poziom moderacji `MANUAL`.
- Dzieci widzą tylko wiadomości zatwierdzone (`APPROVED`), rodzice widzą wszystkie.
- Dostęp do moderacji i kolejki wymaga relacji rodzic-dziecko w danym wątku.

### 8) Frontend - funkcjonalność
- **Logowanie**:
  - Rodzic: login/hasło.
  - Dziecko: kod logowania wygenerowany przez rodzica.
- **Zarządzanie dziećmi**:
  - Tworzenie profili, usuwanie, generowanie kodów logowania.
  - Ustawianie poziomu moderacji (NONE/AUTOMATED/MANUAL).
- **Moderacja**:
  - Kolejka moderacji wiadomości dziecka.
  - Akceptacja/odrzucenie wiadomości.
  - Akceptacja/odrzucenie próśb o znajomość.
- **Czat**:
  - Lista wątków, podgląd ostatnich wiadomości, wskaźniki nieprzeczytanych.
  - Podgląd ostatniej wiadomości ucinany do 20 znaków; data w timestampie pod godziną.
  - Widok wątku z paginacją wsteczną.
  - Wysyłanie wiadomości w istniejących wątkach i start rozmów bezpośrednich.
- **Znajomi**:
  - Zaproszenia do znajomych wyświetlane na górze listy i możliwe do zaakceptowania/odrzucenia.
- **Realtime**:
  - WebSocket STOMP do aktualizacji listy wątków i napływających wiadomości.
- **Routing (slugi)**:
  - `/chats`, `/friends`, `/children`, `/moderation/settings`, `/moderation/queue`.
  - `/threads/*` traktowane jako alias widoku czatów (bez selekcji wątku).

### 9) Bezpieczeństwo po stronie frontendu
- Tokeny przechowywane w **cookie** (persistencja sesji).
- **Access token** jest krótko żyjący, a **refresh token** długo żyjący.
- Automatyczne odświeżanie access tokenu co 4 minuty i retry na 401 dla user/child.
- Autoryzacja żądań przez cookie (fetch z `credentials: include`).
- UI różnicuje funkcje rodzica i dziecka (np. panele moderacji dostępne tylko dla rodzica).

### 10) Konfiguracja i parametry runtime
**Najważniejsze parametry**

| Obszar | Parametr | Znaczenie |
| --- | --- | --- |
| JWT | `security.jwt.secret` | Sekret do podpisu JWT (HS256). |
| JWT | `security.jwt.access-expiration-seconds` | TTL access tokenu (sekundy). |
| JWT | `security.jwt.refresh-expiration-seconds` | TTL refresh tokenu (sekundy). |
| Cookies | `security.jwt.cookie.access-name` | Nazwa cookie access tokenu. |
| Cookies | `security.jwt.cookie.refresh-name` | Nazwa cookie refresh tokenu. |
| Cookies | `security.jwt.cookie.secure` | Flaga Secure dla cookies. |
| Cookies | `security.jwt.cookie.same-site` | Atrybut SameSite (np. Lax). |
| Cookies | `security.jwt.cookie.domain` | Opcjonalna domena cookie. |
| CORS | `security.cors.allowed-origins` | Lista dozwolonych originów. |
| MongoDB | `spring.mongodb.*` | Host, port, user, haslo, baza. |
| PostgreSQL | `spring.datasource.*` | URL, user, haslo. |
| Flyway | `spring.flyway.baseline-on-migrate` | Inicjalizacja migracji. |
| Media | `media.base-url.*` | Bazowe URL-e dla mediów. |
| PL-Guard | `moderation.pl-guard.base-url` | URL serwisu moderacji. |
| PL-Guard | `moderation.pl-guard.timeout-ms` | Timeout zapytań (ms). |

**Kluczowe klasy/komponenty backendu**
- **Uwierzytelnianie**: `AuthController`, `AuthService`, `JwtService`, `JwtAuthenticationFilter`, `AuthCookieService`.
- **Dzieci/rodzic**: `ChildController`, `ChildAuthService`, `ChildManagementService`.
- **Znajomi**: `FriendshipController`, `FriendshipService`.
- **Wątki i wiadomości**: `ThreadController`, `MessageController`, `DirectMessageController`, `ThreadMessageService`, `ThreadListService`.
- **Moderacja**: `MessageModerationController`, `ModerationQueueController`, `ModerationQueueService`, `ModerationLlmService`.
- **WebSocket**: `WebSocketConfig`, `WebSocketAuthChannelInterceptor`, `WebSocketCookieHandshakeInterceptor`, `ThreadWsController`, `MessageNotifier`.

### 11) Migracje i dane startowe
- **PostgreSQL**:
  - Flyway uruchamia migracje przy starcie backendu.
  - `V1__add_child_moderation_level.sql` - schema.
  - `V2__seed_data.sql` - dane przykładowe (kontrole idempotentne).
- **MongoDB**:
  - `data/seed/mongo_seed.js` wykonywany przy pierwszym uruchomieniu kontenera Mongo (jeśli baza pusta).
  - Seeduje wątki i wiadomości.

### 12) Wnioski funkcjonalne
WeeChat to kompletna aplikacja czatu, w sugerowanym modelu rodzic-dziecko, z wbudowaną moderacją treści wspieraną przez AI. Bezpieczeństwo realizowane jest przez JWT w cookie, ograniczanie endpointów oraz logikę autoryzacji domenowej (dostęp do wątków i moderacji). Realtime i UX wspiera WebSocket STOMP oraz panel moderacji z widokami dedykowanymi rodzicom.
