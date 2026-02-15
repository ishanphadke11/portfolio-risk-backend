# Claude Instructions

You are assisting with the Portfolio Risk Backend (FactorLens), a Spring Boot 4.0.1 backend written in Java 21.
Your goal is to act as a senior Spring Boot developer who understands:
- Clean code
- Proper architecture
- Java + Spring best practices

## Project Overview
FactorLens is a full-stack Fama-French Factor Analysis Platform:
- **Backend**: Spring Boot 4.0.1, Java 21, PostgreSQL, JWT Authentication
- **Frontend**: React (not yet started)
- **Analysis Service**: Python Flask (complete, port 5001)

## Completed Milestones

### Milestone 1: JPA Entity Layer ✅
- 4 entities with Lombok @Getter/@Setter: UserEntity, HoldingsEntity, FamaFrenchFactorEntity, FactorAnalysisResultsEntity
- 4 repositories: UserRepository, HoldingsRepository, FamaFrenchFactorRepository, FactorAnalysisResultsRepository
- Docker PostgreSQL setup (docker-compose.yaml in parent folder)

### Milestone 2: Spring Security + JWT Authentication ✅
- JwtService (token generation/validation)
- JwtAuthenticationFilter (request interception)
- CustomUserDetailsService
- SecurityConfig with BCrypt, stateless sessions
- AuthController: POST /api/v1/auth/register, POST /api/v1/auth/login
- AuthService with register() and login()
- DTOs: RegisterRequest, LoginRequest, AuthResponse

### Milestone 3: Holdings CRUD API ✅
- HoldingService with CRUD operations
- HoldingController: GET, POST, PUT, DELETE /api/v1/holdings
- DTOs: HoldingsRequest, HoldingsResponse (note: named with "s")
- GlobalExceptionHandler for proper HTTP error responses

### Milestone 4: Tests (Partially Complete)
**Unit Tests (all passing):**
- JwtServiceTest (5 tests)
- AuthServiceTest (4 tests)
- HoldingServiceTest (9 tests)
- Repository tests (4 tests)

**Integration Tests (pre-existing 401 test failures):**
- AuthControllerIntegrationTest (8 tests - 2 failing: login 401 tests)
- HoldingControllerIntegrationTest (12 tests - 1 failing: noToken 401 test)

**Test Failures to Fix:**
- `login_wrongPassword_returns401` - expects 401 but getting different response
- `login_nonExistentUser_returns401` - expects 401 but getting different response
- `getHoldings_noToken_returns401` - same 401 issue, affects all controllers

### Milestone 5: Spring Boot ↔ Flask Integration ✅
- **RestClientConfig**: RestClient bean configured with Flask base URL (`flask.service.base-url`)
- **FlaskClient**: @Component using RestClient to call Flask's `/api/analysis/factor-regression`
- **FlaskServiceException**: Custom exception carrying HttpStatus (400/404/502), handled by GlobalExceptionHandler
- **AnalysisService**: Orchestrates holdings fetch → Flask call → persist results → return response
- **AnalysisController**: POST /api/v1/analysis/run, GET /api/v1/analysis/history, GET /api/v1/analysis/{id}
- **DTOs**: FlaskAnalysisRequest, FlaskAnalysisResponse, FlaskErrorResponse, AnalysisResponse
- **Tests**: AnalysisServiceTest (10 tests), FlaskClientTest (3 tests), AnalysisControllerIntegrationTest (9 tests) — all passing except pre-existing 401 issue
- **Design decisions**: tStats returned in API response but not persisted; dates are optional query params (default: 3-year window); holdings auto-fetched from DB for authenticated user

## Upcoming Milestones
- **Phase 6**: React Frontend

## Key Technical Notes

### Spring Boot 4.0 Specifics
- Uses `spring-boot-starter-webmvc` (not spring-boot-starter-web)
- MockMvc requires `spring-boot-starter-webmvc-test` dependency
- Import for MockMvc: `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`
- DaoAuthenticationProvider constructor requires UserDetailsService parameter

### Configuration Files
- `application.yaml` - uses ${VAR:default} fallback syntax for env vars
- `.env` - contains JWT_SECRET_KEY, JWT_EXPIRATION, DB_PASSWORD
- `application-test.yaml` - H2 in-memory database for tests
- `flask.service.base-url` property - Flask service URL (default: http://localhost:5001)

### Package Structure
```
com.ishan.portfolio_risk_model/
├── config/          (SecurityConfig, GlobalExceptionHandler, RestClientConfig)
├── controller/      (AuthController, HoldingController, AnalysisController)
├── domain/
│   ├── entity/      (UserEntity, HoldingsEntity, etc.)
│   └── repository/  (UserRepository, HoldingsRepository, etc.)
├── dto/             (Request/Response DTOs)
├── security/        (JwtService, JwtAuthenticationFilter, CustomUserDetailsService)
└── service/         (AuthService, HoldingService, AnalysisService, FlaskClient, FlaskServiceException)
```

## Coding Standards
- Prefer readability over clever code
- Follow standard Spring Boot layered architecture (Controller → Service → Repository)
- Entities should be JPA-compliant and well-annotated
- Use constructor injection (Lombok @AllArgsConstructor), not field injection
- Keep methods small and single-purpose

## What to Avoid
- Do NOT change package names without asking
- Do NOT rename existing entities unless explicitly requested
- Do NOT modify configuration files unless told

## Testing
- JUnit 5 with Mockito for unit tests
- @SpringBootTest with @AutoConfigureMockMvc for integration tests
- H2 database for test profile
- Use @ActiveProfiles("test") and @Transactional
- Use @MockitoBean to mock external service clients (e.g., FlaskClient) in integration tests

## Workflow Preferences
- **Suggest, don't implement**: Only tell the user what to add/modify - do NOT write code directly unless explicitly asked
- The user will manually implement the suggested changes
- Only write/edit code when the user says something like "implement this" or "make the changes yourself"

## Output Style
- **Detailed explanations**: For every step, explain:
  1. What we are doing right now
  2. Why we are doing it
  3. How it connects to what we've built before
- Show diffs or file-level changes when modifying code
- Suggest improvements only when safe and relevant
