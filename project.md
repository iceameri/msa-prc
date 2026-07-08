OAuth 2.1 기반 인증 서버와
리소스 서버(jwt),
리소스 서버(opaque token)



Spring Boot 4.0
Java 21
Kotlin
MSA
Swagger


Docker-compose (로컬 개발)
- postgres-auth (port 5432) → authorization_db
- postgres-jwt  (port 5433) → jwt_db
- postgres-opaque (port 5434) → opaque_db
- redis, Kafka (KRaft), Kafka-connect, MinIO, Elasticsearch
- LGTM 스택: Loki(3100), Tempo(3200·4317·4318), Mimir(9009), Alloy(12345), Grafana(3000)
  - docker/tempo.yaml, docker/mimir.yaml, docker/alloy.config, docker/grafana/provisioning/ (git 추적)
  - infra/ 디렉터리는 데이터 볼륨 전용 (.gitignore)
- kafka_data 볼륨 추가 (재시작 시 토픽/오프셋 보존)
- kafka-connect healthcheck 추가 (port 8083, start_period 30s)
- kafka-init 제거 → 토픽 생성은 각 서비스 KafkaTopicConfig로 대체

Deprecated 서비스 (K8s 전환으로 역할 대체)
- eureka-server (port 1120): Spring Cloud 의존성 제거 → K8s Service DNS (서비스명.네임스페이스.svc.cluster.local)로 대체
- config-server (port 1110): spring.config.import(configserver) 제거 → K8s ConfigMap/Secret으로 대체
- gateway-server (port 1100): K8s Gateway API + Istio로 대체, Rate Limiting/CORS는 각 서비스로 이관
  - authorization-server/jwt-server/opaque-server build.gradle.kts에서 spring-cloud-*, eureka-client 의존성 제거
  - application.yaml에서 spring.config.import, eureka 블록 제거

배포 프로파일 분리
- application.yaml: 로컬 개발용 (localhost DNS, 하드코딩 포트)
- application-k8s.yaml (신규): K8s 배포용 (K8s Service DNS, env var 참조)
  - 활성화: --spring.profiles.active=k8s
  - DB/Redis/Kafka: {service-name}.{namespace}.svc.cluster.local
  - 민감 값: ${ENV_VAR} 참조 (K8s Secret에서 주입)
  - spring.sql.init.mode=always: 멱등 DDL(IF NOT EXISTS) 기동 시 자동 실행

이미지 빌드
- 각 서비스 build.gradle.kts에 bootBuildImage 태스크 추가 (Buildpacks)
  - imageName: iceameri/{service-name}:{version}

Kafka (KRaft 모드)
- ZooKeeper 미사용 (Kafka 4.0부터 완전 제거됨)
- broker + controller 단일 노드 겸용 (KAFKA_PROCESS_ROLES: broker,controller)
- CLUSTER_ID 고정값 설정 (재기동 시 동일 클러스터 유지, 토픽/오프셋 데이터 보존)
- 리스너 구성: PLAINTEXT(컨테이너 내부 29092), PLAINTEXT_HOST(호스트 9092), CONTROLLER(내부 9093)
- 토픽 생성: KafkaAdmin + NewTopic 빈 (앱 기동 시 자동 생성, 이미 존재하면 무시)
  - jwt-server: notifications, reports, outbox.events (파티션 3)
  - opaque-server: payment.saga, user-management, report-actions, user-active (파티션 3)
  - authorization-server: user-sync, user.username.updated (파티션 3)
  - ~~springCloudBus: Spring Cloud Bus 자동 생성~~

Common
- dto는 Kotlin data class 사용 (Java record 대체)
- Clean Architecture (domain / application / infrastructure / presentation)
- JdbcTemplate만 사용 (JPA/Hibernate 미사용)
- FK 논리적 사용 (물리적 REFERENCES 제약 없음, 컬럼 주석으로 관계 표시)
  - MSA 특성상 서비스 간 DB 분리, 각 DB 내부도 운영 유연성을 위해 논리 FK만 사용
- SQL 테이블명 완전 한정 표기 (database.schema.table 형식)
  - authorization-server: authorization_db.public.{table}
  - jwt-server: jwt_db.public.{table}
  - opaque-server: opaque_db.public.{table}

테이블 설계 기본 원칙
- user_id + username_snapshot 이중 저장 패턴 (감사/이력 테이블에 적용)
  - user_id NOT NULL: 신뢰 식별자 (변경 불가, authorization_db users.id 기준)
  - {actor/recipient/reporter}_username: 이력 스냅샷 (행위/수신 시점의 username 영구 보존)
  - 이유: username은 변경 가능하지만 감사 목적의 행위 시점 username은 불변이어야 함
  - 적용 대상: audit_logs(actor_id + actor_username), notifications(recipient_id + recipient_username)
- authorization_users JOIN 패턴 (표시용 username이 필요한 도메인 테이블에 적용)
  - posts/comments: author_id만 저장, 조회 시 authorization_users LEFT JOIN으로 현재 username 획득
  - username 변경 시 authorization_users 단 1행 UPDATE → 모든 JOIN이 자동 반영
  - snapshot 패턴과 구분: 이쪽은 항상 현재 username 표시, 저쪽은 행위 시점 username 고정
- Swagger 문서화 (springdoc-openapi, auth/jwt/opaque: webmvc, ~~gateway: webflux~~)
- Resilience4j 기반 timeout, retry, circuit breaker
- CORS: 각 서비스(jwt-server, opaque-server, authorization-server) SecurityConfig에서 자체 처리
  - allowedOriginPatterns: * / OPTIONS /** permitAll (preflight 차단 방지)
  - gateway-server CORS 의존 제거 → Zero Trust 아키텍처 일관성 유지
- Outbox Pattern (Kafka 메시지 유실 방지, Kafka-connect 연동)
- Saga Pattern (분산 트랜잭션)
- CQRS (읽기/쓰기 분리)
- 배포 후 재기동 시 알림 제외
  - ~~eureka initial-status: STARTING (헬스체크 통과 전 트래픽 차단)~~
  - Kafka consumer auto-offset-reset: latest (재기동 후 이전 메시지 재처리 방지)
- ~~Eureka 클라이언트 공통 설정 (config/gateway/authorization/jwt/opaque)~~
  - ~~instance-id: ${spring.application.name}:${server.port} (멀티 인스턴스 구분)~~
  - ~~lease-renewal-interval-in-seconds: 10 (기본 30 → 하트비트 주기)~~
  - ~~lease-expiration-duration-in-seconds: 30 (기본 90 → DOWN 판정 시간)~~
  - ~~registry-fetch-interval-seconds: 5 (기본 30 → 레지스트리 갱신 주기)~~
  - ~~health-check-url-path: /actuator/health~~
  - ~~status-page-url-path: /actuator/info~~
- Prometheus 메트릭 수집 (micrometer-registry-prometheus, /actuator/prometheus)
- LGTM 스택 (Loki + Grafana + Tempo + Mimir + Alloy) — ELK·Zipkin 대체
  - Tracing: micrometer-tracing-bridge-otel + opentelemetry-exporter-otlp
    - 로컬: management.otlp.tracing.endpoint=http://localhost:4318/v1/traces (Tempo 직접 전송)
    - K8s: management.otlp.tracing.endpoint=http://tempo:4318/v1/traces
  - Logging: com.github.loki4j:loki-logback-appender:1.5.2 (Spring Boot BOM 미관리 → 버전 명시)
    - k8s 프로파일에서만 Loki4jAppender 활성화 (로컬은 CONSOLE only — 연결 오류 방지)
    - 레이블: app=${appName},level=%level / 메시지 패턴에 traceId·spanId 포함 (Tempo 연동)
  - Metrics: Alloy가 /actuator/prometheus 스크레이프 → Mimir remote_write (로컬: host.docker.internal 경유)
  - Grafana 데이터소스 자동 프로비저닝: Mimir(기본)/Loki/Tempo, TraceID↔Loki 연동 설정


authorization-server
- port 1010
- OAuth 2.1 기반 토큰 발급 (Spring Authorization Server)
  - JdbcRegisteredClientRepository / JdbcOAuth2AuthorizationService / JdbcOAuth2AuthorizationConsentService
  - RSA 키페어로 JWT 서명
  - dev (로컬): 기동 시 인메모리 생성
  - k8s 프로파일: classpath에서 private.pem / public.pem 파일 로드 (AuthorizationServerConfig에서 PemUtils 사용)
  - ⚠️ private.pem, public.pem Git 커밋 금지 → K8s Secret으로 마운트
  - 등록 클라이언트: jwt-service-client (JWT), opaque-server-client (Opaque token)
  - PKCE 필수 (require-proof-key: true) — authorization_code 흐름 시 code_challenge/code_verifier 필수
  - refresh token 재사용 불가 (reuse-refresh-tokens: false) — 갱신 시 새 refresh token 발급
- OIDC 지원
- 기본 회원 로그인 (아이디/비밀번호, BCrypt)
- Social login (Google, GitHub) — 미구현, application.yaml 주석처리 상태 (client-id/secret 발급 후 활성화)
- MFA / TOTP (Google Authenticator)
- QR 로그인
- 로그인 실패 횟수 제한 (5회 초과 시 30분 잠금)
- 동시 로그인 세션 제한 (jwt: 무제한, opaque: 5개)
- Token revocation endpoint
- 비밀번호 재설정 플로우
- 로그아웃 (POST /api/logout) — SLO (Single Logout)
  - RedisLogoutHandler: jwt:authorities:{username} + auth:user:{username} Redis 삭제
  - TokenRevocationService.revokeAllForPrincipal(): oauth2_authorization 테이블 Refresh Token 폐기 (재발급 차단)
  - HTTP 세션 무효화 + SecurityContext 클리어
  - 성공 시 /login?logout 리다이렉트

권한(Authorities) 설계
- JWT payload에 authorities 미포함 (노출 방지)
- 로그인 성공 시 authorities → Redis 저장 (key: jwt:authorities:{username}, TTL: 24h)
- ~~gateway에서 요청마다 Redis 조회 → X-User-Authorities 헤더로 하위 서비스에 전달~~
- ~~하위 서비스(jwt-server)는 헤더를 신뢰하여 SecurityContext 구성 (Redis 직접 접근 없음)~~
- ~~Redis miss 시 빈 권한으로 헤더 전달 → 하위 서비스에서 403 → 클라이언트 재로그인 유도~~
- 현재: jwt-server/opaque-server가 Redis jwt:authorities:{username} 직접 조회 (Zero Trust)

MFA / TOTP (Google Authenticator)
- setup: secret 생성 → Redis 임시 저장(auth:mfa:pending:{username}, TTL 10분) → QR base64 반환
- enable: Redis pending secret 조회 → TOTP 코드 검증 → DB 저장 → Redis 캐시 무효화
- disable: DB mfa_enabled=false, mfa_secret=null → 캐시 무효화gg
- 로그인 MFA 분기: LoginSuccessHandler에서 mfaEnabled 확인 → SecurityContext clear → session에 MFA_PENDING_USER 저장 → /mfa/verify 리다이렉트
- /mfa/verify: TOTP 코드 검증 → 수동 재인증(UsernamePasswordAuthenticationToken) → Redis authorities 저장 → saved request로 복귀

QR 로그인
- token: UUID, 상태: PENDING → CONFIRMED → 소비(삭제)
- TTL: 5분, Redis 키: auth:qr:{token}
- /qr-login/generate: QR 이미지(ZXing11 PNG) + token 반환
- /qr-login/status/{token}: 브라우저 폴링용
- /qr-login/confirm/{token}: 모바일 앱(인증된 사용자)이 호출 → CONFIRMED 상태로 전환
- /qr-login/consume/{token}: 브라우저가 CONFIRMED 감지 후 호출 → username 반환 후 토큰 삭제

비밀번호 재설정
- /password/reset-request: 이메일로 재설정 링크 발송 (이메일 존재 여부 무관 200 반환 — 계정 노출 방지)
- /password/reset: token + 새 비밀번호 → BCrypt 인코딩 후 DB 업데이트
- token: Redis 저장 (auth:reset:{token} → username, TTL 30분)

동시 세션 제한
- SessionLimitingAuthorizationService가 JdbcOAuth2AuthorizationService를 래핑
- opaque-server-client 토큰 발급 시 oauth2_authorization 테이블에서 해당 사용자 세션 수 조회
- 5개 초과 시 가장 오래된 세션부터 revoke

Redis 키 네임스페이스 (단일 Redis 인스턴스, prefix로 논리 분리)
- auth:user:{username}        → 유저 객체 캐시 (TTL 30분)
- auth:attempts:{username}    → 로그인 시도 횟수 (TTL 30분)
- auth:mfa:pending:{username} → MFA setup 임시 secret (TTL 10분)
- auth:qr:{token}             → QR 로그인 세션 (TTL 5분)
- auth:reset:{token}          → 비밀번호 재설정 토큰 (TTL 30분)
- jwt:authorities:{username}  → 유저 권한 목록 (TTL 24h) ← authorization-server write / jwt-server·opaque-server read

CORS
- SecurityConfig에 CorsConfigurationSource 빈 등록 (두 필터 체인 모두 적용)
- allowedOriginPatterns: * / allowedMethods: GET·POST·PUT·DELETE·PATCH·OPTIONS / allowedHeaders: *
- allowCredentials: true (login form 세션 쿠키 지원)
- defaultSecurityFilterChain: OPTIONS /** permitAll 추가 (Spring Security preflight 차단 방지)

JWT 클레임 커스터마이징
- OAuth2TokenCustomizerConfig: OAuth2TokenCustomizer<JwtEncodingContext> 빈 등록 (토큰 타입별 분기)
  - access_token (유저): user_id=userId.toString(), username 클레임 추가 (jwt-server JwtClaimsFilter용)
  - id_token (OIDC): sub=userId.toString() (안정적 식별자), preferred_username=username (OIDC 표준 클레임)
  - client_credentials: client_id 클레임 추가, user_id/username 없음
- OAuth2TokenCustomizerConfig: OAuth2TokenCustomizer<OAuth2TokenClaimsContext> 빈 등록 (opaque token 전용)
  - 유저 토큰: sub = userId.toString() (opaque introspector가 toLongOrNull()로 유저 감지), username 클레임 추가
  - client_credentials: 아무것도 추가하지 않음 → sub = client_id (Spring 기본값, 비숫자)

Clean Architecture 구조
- domain/user: User (status, lastActiveAt 추가; mfaEnabled, mfaSecret 포함), UserRole, UserRepository (findByUsername/findByEmail/findById/save/lockUser/resetLoginAttempts/updateMfaSettings/setEnabled/setEnabledById/updateStatusById/setStatusAndEnabled/updateUsername), UserActivityRepository (upsert)
- domain/qr: QrLoginSession, QrLoginStatus
- application/port/out: UserCachePort (deleteAuthorities 포함), QrLoginCachePort, PasswordResetCachePort
- application/service: UserDetailsServiceImpl, LoginAttemptService, MfaService, QrLoginService, PasswordResetService
- infrastructure/persistence: UserJdbcRepository (ResultSetExtractor + LEFT JOIN, mfa/status/last_active_at 컬럼, findByEmail; save() 후 user-sync 이벤트 발행; updateUsername() 후 user.username.updated 이벤트 발행), UserActivityJdbcRepository (user_activity UPSERT)
- infrastructure/cache: RedisUserCache, RedisQrLoginCache, RedisPasswordResetCache
- infrastructure/oauth2: SessionLimitingAuthorizationService, TokenRevocationService (principal_name 기준 oauth2_authorization 전체 삭제 — SUSPEND/BAN/DELETE 시 기존 토큰 즉시 폐기)
- infrastructure/security: LoginSuccessHandler (MFA 분기), LoginFailureHandler, RedisLogoutHandler (LogoutHandler 구현)
- infrastructure/kafka: UserManagementEventConsumer (user-management 토픽 구독 → userId 기반 setStatusAndEnabled 단일 쿼리 + Redis 무효화 + TokenRevocationService 토큰 폐기), UserActivityEventConsumer (user-active 토픽 구독 → user_activity UPSERT), UserSyncEventPublisher (user-sync 토픽 발행 + user.username.updated 토픽 발행)
  - UserSyncEventPublisher.publish(): enabled, status 파라미터 포함 (jwt-server 계정 상태 동기화)
  - setStatusAndEnabled(): UPDATE...RETURNING으로 변경 → Kafka payload에 version 포함 가능
  - setEnabled / setEnabledById / updateStatusById 구현체 제거 (setStatusAndEnabled으로 통합)
- infrastructure/batch: InactiveUserCleanupService (@Scheduled 매일 03:00 + 수동 트리거, JdbcTemplate으로 user_activity 조회 → user-management 토픽 발행)
- infrastructure/config: SecurityConfig (logout 블록 추가), AuthorizationServerConfig, OAuth2TokenCustomizerConfig, RedisConfig, SwaggerConfig, KafkaProducerConfig (KafkaTemplate<String,String> 명시적 빈 + user-sync/user.username.updated NewTopic)
- presentation: MfaController, QrLoginController, PasswordResetController, BatchController (수동 실행: POST /admin/batch/inactive-user-cleanup → 처리된 유저 수 반환)

주요 설계 결정
- http.userDetailsService() Spring Security 7에서 제거 → @Service bean 자동 감지로 대체
- AuthorizationServerConfig issuer: 필드 주입(lateinit var) → 생성자 주입으로 변경
- saveAuthorities 시그니처: Set<String?> (linter 수정 반영)
- SessionLimitingAuthorizationService: queryForList<String> Kotlin 확장 함수 사용 (linter 수정 반영)
- UserManagementEventConsumer: Kafka payload에서 userId(Long) 수신 → findById로 username 조회 → Redis 무효화
  (opaque-server가 userId 기반으로 이벤트 발행하기 때문)

비활성 유저 정리 (InactiveUserCleanupService)
- Spring Batch 미사용 (Spring Boot 4.0 미지원) → JdbcTemplate + @Scheduled 직접 구현
- 매일 03:00 자동 실행 (@Scheduled cron="0 0 3 * * *"), @EnableScheduling 활성화
- user_activity 테이블에서 last_active_at < NOW() - 90days 조회 → userId 목록
- user-management 토픽에 {"action":"SUSPEND","userId":$userId} 발행
  → UserManagementEventConsumer 소비 → DB status 업데이트 + Redis 무효화
- 수동 실행: POST /admin/batch/inactive-user-cleanup → {"queued": N, "status": "COMPLETED"}

DDL (authorization_db) — FK 논리적 사용 (REFERENCES 제약 없음)
- users: id(BIGSERIAL PK), username, password, email, enabled, status(ACTIVE/SUSPENDED/BANNED/DELETED), login_attempts, locked_until, last_active_at, created_at, mfa_enabled, mfa_secret, version BIGINT DEFAULT 1 (이벤트 발행 횟수 — jwt-server out-of-order 판정 기준)
- user_authorities: user_id (logical FK → users.id), authority — 복수 권한 지원
- user_activity: user_id(BIGINT PK, logical FK → users.id), last_active_at
- authorization_system_clients: client_id(VARCHAR PK), display_name — 시스템 클라이언트 원본 (jwt_db.authorization_system_clients의 source of truth)
- oauth2_registered_client, oauth2_authorization, oauth2_authorization_consent (timestamp 전부 TIMESTAMPTZ)


jwt-server
- port 1020
- 일반 유저, 어드민 유저, 시스템 클라이언트 (ROLE_USER, ROLE_ADMIN, ROLE_SYSTEM)
- @EnableMethodSecurity 활성화 (@PreAuthorize 메서드 레벨 권한 체크)
- STATELESS 세션
- 게시글 CRUD (삭제: 작성자 본인 또는 ADMIN 가능)
- 댓글 기능
- 좋아요 기능
- 팔로우 / 팔로워
- 피드 (팔로우한 유저 게시글 모아보기)
- 해시태그
- 검색 (게시글, 유저, Elasticsearch 연동)
  - ES 초기화/재기동 시: POST /api/search/reindex (ADMIN 전용) → PostgreSQL 전체 재인덱싱
- 이미지 업로드 (MinIO 연동)
- 알림 (Kafka 이벤트 발행)
- 신고 기능

Rate Limiting (Redis 토큰 버킷)
- RedisRateLimiter: StringRedisTemplate + RedisScript<Long>으로 Lua 스크립트 원자적 실행
  - bucket4j Maven Central 미제공 → 직접 구현 (HMGET·HMSET·EXPIRE)
  - 키: rl:api:{principal} (인증된 경우 username, 미인증 시 IP)
  - /api/**: burst=40, refill=20req/s
- RateLimitInterceptor: preHandle에서 tryConsume() → false면 429 응답
- WebMvcConfig: addInterceptors("/api/**")
- 429 응답 형식: {"status":429,"error":"Too Many Requests","path":"..."}

CORS
- SecurityConfig: CorsConfigurationSource 빈 + OPTIONS /** permitAll
- allowedOriginPatterns: * / allowedMethods: GET·POST·PUT·DELETE·PATCH·OPTIONS / allowedHeaders: *

인증 방식 (Zero Trust — Istio 서비스 메시 위임)
- JWT 서명 검증: Istio sidecar의 RequestAuthentication이 처리 (authorization-server JWKs 조회)
  - 앱 레벨 서명 검증 제거 → spring-boot-starter-oauth2-resource-server 의존성 제거
  - Istio가 거부한 요청은 앱까지 도달하지 않음 (K8s 환경)
  - 로컬(Istio 없음): 서명 검증 없이 JWT payload Base64 파싱만 수행 (개발 편의)
- JwtClaimsFilter (OncePerRequestFilter): Istio 검증 통과 후 Authorization 헤더에서 클레임 파싱
  - user_id 클레임 있음 → 유저 토큰: sub(username)으로 Redis jwt:authorities:{username} 조회 → AuthenticatedUser(id, username, roles) → SecurityContext
  - user_id 없음 → 시스템 토큰: AuthenticatedClient(clientId) → SecurityContext (ROLE_SYSTEM)
  - Redis miss → 빈 roles → @PreAuthorize에서 403 (fail-open, 재로그인 유도)
  - 파싱 실패 → SecurityContext 미설정 → anyRequest().authenticated() → 401
- SecurityConfig: authenticationEntryPoint 추가 (미인증 시 401, 기존 403 방지)
- CallerPrincipal 인터페이스: AuthenticatedUser / AuthenticatedClient 공통 타입
  - PostController/CommentController: @AuthenticationPrincipal caller: CallerPrincipal 사용 (USER/ADMIN/SYSTEM 모두 허용)
  - LikeController/FollowController/FeedController/ReportController: @AuthenticationPrincipal user: AuthenticatedUser? + null 가드 (SYSTEM 인증 통과 후 사용자 컨텍스트 필요 시 403)
- 역할(Role) 기반 인가: 앱 레벨 @PreAuthorize 유지 (roles는 Redis에 있어 Istio에서 처리 불가)

authorization_system_clients (Local Read-Model for machine clients)
- Source of truth: authorization_db.authorization_system_clients (authorization-server 관리)
- Local read-model: jwt_db.authorization_system_clients — 배포 시 DML로 수동 동기화 (Kafka sync 없음, 클라이언트는 런타임에 변경 없음)
- 조회 시 posts/comments에서 COALESCE(u.username, sc.display_name) AS author_username 사용
  - client_id가 있는 행은 sc.display_name을 author_username으로 표시
- 초기 데이터: jwt-server, opaque-server (authorization_db + jwt_db 모두 DML 삽입 필요)

authorization_users (Local Read-Model)
- jwt_db.authorization_users(id BIGINT PK, username, version BIGINT, updated_at TIMESTAMPTZ) — authorization_db users의 로컬 복사본
- sync(userId, username, version): authorization_users에 UPSERT (id 기준, username/version 갱신)
  - 호출 경로 1: authorization-server가 user-sync 토픽 발행 → UserSyncEventConsumer 소비
  - 호출 경로 2: username 변경 시 user.username.updated 토픽 → UsernameUpdateEventConsumer 소비 → sync() 호출 (단 1행 UPDATE)
  - 호출 경로 3: 서비스 내부 직접 호출 (write 서비스 선행 sync, version = 0L 기본값)
- resolveId(username): username으로 로컬 DB 조회 → userId 반환 (공개 엔드포인트용)
- authorization_users.id는 authorization_db users.id와 동일값 (논리적 FK, BIGSERIAL 아님)
- 모든 도메인(post/comment/like/follow 등)은 authorization_users.id를 FK로 참조
- posts/comments는 author_username 컬럼 없음 — 조회 시 authorization_users LEFT JOIN으로 현재 username 획득
- 장점: authorization-server 장애 시에도 jwt-server 독립 운영 가능, username 변경 시 단일 테이블만 업데이트

Idempotent Consumer (Kafka 메시지 중복 처리 방지)
- processed_kafka_events 테이블: event_id(topic:partition:offset), topic, processed_at
  - PRIMARY KEY(event_id), INSERT ON CONFLICT DO NOTHING → affectedRows 0이면 중복으로 판정
  - 30일 경과 레코드 자동 정리 (@Scheduled cron="0 0 3 * * *")
- IdempotentEventGuard.runIfNew(eventId, topic, block): @Transactional
  - insertIfAbsent() 성공 시(새 이벤트) block() 실행, 실패 시(중복) 건너뜀
  - processed_kafka_events 삽입과 도메인 변경이 같은 트랜잭션 → 원자적 보장

out-of-order 보호 (순서 역전 메시지 무시)
- authorization_users.version: authorization-server가 DB 업데이트마다 +1 증가시켜 payload에 포함
- UPSERT WHERE 조건: WHERE authorization_users.version < EXCLUDED.version
  - Kafka 파티션 키(userId)로 순서 보장되지만, 파티션 수 변경 등 예외 상황에서도 방어
  - updated_at은 감사 컬럼으로 유지 (ON CONFLICT 시 NOW() 갱신, payload 무관)
  - version 없는 구버전 메시지는 0L 처리 (신규 row면 INSERT, 기존 row면 WHERE 차단)

Outbox Pattern (알림 이벤트)
- 도메인 이벤트 발생 시 outbox_events 테이블에 먼저 저장 (같은 트랜잭션)
- 릴레이 (@Scheduled fixedDelay=1s, initialDelay=5s):
  - findAndClaim: UPDATE SET claimed_at=NOW() ... FOR UPDATE SKIP LOCKED RETURNING (원자적 pick-up, 다중 인스턴스 중복 방지)
  - Kafka 동기 발행 (.get(5s)) → markSent: UPDATE SET sent_at=NOW()
  - 발행 실패 시 unclaim (claimed_at=NULL) → 다음 사이클 재시도
- stale claim 복구 (@Scheduled fixedDelay=30s): claimed_at > 30초 AND sent_at IS NULL → claimed_at=NULL
- 처리 완료 이벤트 정리 (@Scheduled fixedDelay=1h): sent_at > 7일 → DELETE
- 직접 KafkaTemplate 발행 제거 (이중 발행 방지) — 모든 서비스가 outbox 저장만 수행
- 미처리 추적: 테이블에 남아있는 레코드 = 미처리 (sent_at IS NULL)
- 토픽 라우팅: POST_CREATED → outbox.events / REPORT_CREATED → reports / 나머지 → notifications
- 이벤트 타입: POST_CREATED, POST_LIKED, POST_COMMENTED, USER_FOLLOWED, REPORT_CREATED

DDL (jwt_db) — FK 논리적 사용 (REFERENCES 제약 없음)
- authorization_system_clients: client_id(VARCHAR PK), display_name
- authorization_users: user_id(BIGINT PK, authorization_db users.id와 동일값), username (UNIQUE CONSTRAINT), enabled BOOLEAN DEFAULT true, status VARCHAR(20) DEFAULT 'ACTIVE', version BIGINT DEFAULT 0 (out-of-order 판정 기준), updated_at TIMESTAMPTZ (감사 컬럼 — ON CONFLICT 시 NOW() 갱신)
  - id → user_id 컬럼명 변경 (authorization-server users.id와 일관성)
  - enabled/status 추가: Kafka user-sync 이벤트로 계정 상태 로컬 동기화 (인증 시 DB 직접 조회 없이 판단 가능)
- processed_kafka_events: event_id VARCHAR(200) PK (topic:partition:offset), topic VARCHAR(100), processed_at TIMESTAMPTZ DEFAULT NOW()
- posts: id, author_id BIGINT NULL (logical FK → authorization_users.id), client_id VARCHAR NULL (logical FK → authorization_system_clients.client_id), title, content, image_url, like_count, comment_count, status, created_at, updated_at
  - XOR CHECK: (author_id IS NOT NULL AND client_id IS NULL) OR (author_id IS NULL AND client_id IS NOT NULL)
  - 조회: LEFT JOIN authorization_users + LEFT JOIN authorization_system_clients, COALESCE(u.username, sc.display_name) AS author_username
- comments: id, post_id (logical FK → posts.id), author_id BIGINT NULL (logical FK → authorization_users.id), client_id VARCHAR NULL (logical FK → authorization_system_clients.client_id), content
  - XOR CHECK 동일 적용, 조회 시 COALESCE(u.username, sc.display_name) AS author_username
- likes: post_id (logical FK → posts.id), user_id (logical FK → authorization_users.id) — PK(post_id, user_id)
- follows: follower_id / following_id (logical FK → authorization_users.id) — PK(follower_id, following_id), CHECK(follower ≠ following)
- hashtags: id, name(UNIQUE) + post_hashtags: post_id / hashtag_id (logical FK)
- reports: id, reporter_id (logical FK → authorization_users.id), target_type, target_id, reason
- outbox_events: id, aggregate_id, aggregate_type, event_type, payload(JSONB), claimed_at(TIMESTAMPTZ), sent_at(TIMESTAMPTZ), created_at
  - 부분 인덱스: idx_outbox_unclaimed ON (created_at ASC, id ASC) WHERE claimed_at IS NULL AND sent_at IS NULL

Clean Architecture 구조
- domain: User(id: Long, username, version: Long), Post(authorId: Long? — nullable, clientId: String? — system 작성 시, authorUsername: String? — JOIN으로 채워짐, PostStatus), Comment(authorId: Long? nullable, clientId: String? nullable, authorUsername: String? — JOIN으로 채워짐), Like, Follow, Hashtag, Report(ReportTargetType), OutboxEvent
- domain/event: ProcessedEventRepository (insertIfAbsent / deleteOlderThan)
- application/port/out: AuthoritiesCachePort, SearchPort, ImageStoragePort, EventPublishPort, OutboxRepository (save / findAndClaim / markSent / unclaim / resetStaleClaims / deleteProcessed)
- application/service: UserSyncService (sync(userId, username, enabled, status, version=0L)=authorization_users UPSERT / syncUsername(userId, username, version)=username만 변경(enabled/status 덮어쓰기 방지) / resolveId), IdempotentEventGuard (@Transactional runIfNew(eventId, topic, block) + @Scheduled cleanup 매일 03:00), PostService, CommentService, LikeService, FollowService, FeedService, SearchService (reindexAll: PostgreSQL 전체 배치 조회 → ES 재인덱싱, 500건 단위), ImageService, ReportService
  - PostService/CommentService: CallerPrincipal 파라미터 사용 (resolveAuthor() / canModify() 내부 헬퍼)
    - AuthenticatedUser: sync(id, username) 선행, authorId 사용
    - AuthenticatedClient: sync 없음, clientId 사용, displayName = clientId
  - Post/Comment 저장 시 authorUsername 미저장 — authorization_users/authorization_system_clients JOIN으로 조회 시 획득
  - 알림 이벤트 payload 구조: {postId?, actorUsername, targetUsername} — outbox 저장만, 직접 발행 없음
  - PostService canModify(): AuthenticatedUser → authorId == userId OR ROLE_ADMIN / AuthenticatedClient → clientId 일치
- infrastructure/persistence: UserJdbcRepository (authorization_users 테이블, sync(userId, username, version: Long)=ON CONFLICT DO UPDATE WHERE version < EXCLUDED.version), ProcessedEventJdbcRepository (INSERT ON CONFLICT DO NOTHING → affectedRows 판정), PostJdbcRepository (모든 조회에 LEFT JOIN authorization_users + LEFT JOIN authorization_system_clients, COALESCE AS author_username; save()에서 authorId/clientId nullable 처리; rs.wasNull()로 NULL authorId 감지), CommentJdbcRepository (동일 패턴), LikeJdbcRepository, FollowJdbcRepository, HashtagJdbcRepository, ReportJdbcRepository, OutboxJdbcRepository (findAndClaim: FOR UPDATE SKIP LOCKED RETURNING / markSent / unclaim / resetStaleClaims / deleteProcessed)
- infrastructure/elasticsearch: ElasticsearchSearchAdapter (PostDocument / UserDocument, NativeQuery; 인덱스 없을 때 exception chain 순회로 index_not_found 감지 → 빈 배열 반환)
- infrastructure/minio: MinioImageAdapter (버킷 자동 생성)
- infrastructure/kafka: KafkaEventPublisher (KafkaTemplate<String, String>), OutboxRelayService (@Scheduled fixedDelay=1s initialDelay=5s — findAndClaim(100) → Kafka 동기 발행(.get(5s)) → markSent; 실패 시 unclaim; cleanupStaleClaims 30s 주기; cleanupProcessed 1h 주기), UserSyncEventConsumer (user-sync 토픽 — enabled/status 파싱 → userSyncService.sync(userId, username, enabled, status, version)), UsernameUpdateEventConsumer (user.username.updated 토픽 — userSyncService.syncUsername() 호출, enabled/status 덮어쓰기 없음)
- infrastructure/cache: RedisAuthoritiesCache (jwt:authorities:{username} read-only)
- infrastructure/security:
  - CallerPrincipal (interface) — AuthenticatedUser / AuthenticatedClient 공통 타입
  - AuthenticatedUser(id: Long, username: String, roles: List<String>) : CallerPrincipal
  - AuthenticatedClient(clientId: String) : CallerPrincipal
  - JwtClaimsFilter (OncePerRequestFilter): Authorization 헤더 → Base64 JWT payload 파싱 → SecurityContext 구성 (서명 재검증 없음, Istio 위임)
- infrastructure/config: SecurityConfig (JwtClaimsFilter 주입, authenticationEntryPoint 401, CORS, OPTIONS permitAll), RedisConfig (Jackson 3.x), MinioConfig, SwaggerConfig, KafkaTopicConfig (notifications/reports/outbox.events, 파티션 3), KafkaProducerConfig (KafkaTemplate<String,String> 명시적 빈), KafkaConsumerConfig (@EnableKafka + ConcurrentKafkaListenerContainerFactory, StringDeserializer, auto-commit), WebMvcConfig (RateLimitInterceptor → /api/**)
- presentation:
  - PostController / CommentController: @AuthenticationPrincipal caller: CallerPrincipal, @PreAuthorize('USER','ADMIN','SYSTEM')
  - LikeController / FollowController / FeedController / ReportController: @AuthenticationPrincipal user: AuthenticatedUser? + null 가드 403, @PreAuthorize('USER','ADMIN','SYSTEM')
  - ImageController: @PreAuthorize('USER','ADMIN','SYSTEM') (user 파라미터 없음)
  - SearchController.reindex: @PreAuthorize('ADMIN','SYSTEM')
  - GlobalExceptionHandler


opaque-server
- port 1030
- OAuth 2.1 Resource Server (Opaque token introspection) — ROLE_ADMIN (관리) + ROLE_USER (결제) + ROLE_SYSTEM (시스템 클라이언트) 공용 서버

Rate Limiting (Redis 토큰 버킷)
- RedisRateLimiter: jwt-server와 동일 구현 (StringRedisTemplate + Lua 스크립트)
  - /admin/**: rl:admin:{principal}, burst=20, refill=10req/s
  - /payments/**: rl:payments:{principal}, burst=40, refill=20req/s
  - else: 패스스루 (rate limiting 없음)
- RateLimitInterceptor: requestURI prefix 분기
- WebMvcConfig: addInterceptors("/admin/**", "/payments/**")

CORS
- SecurityConfig: CorsConfigurationSource 빈 + OPTIONS /** permitAll
- allowedOriginPatterns: * (Bearer token 사용으로 allowCredentials 불필요)

인증 방식
- Opaque token → authorization-server /oauth2/introspect 호출
- CustomOpaqueTokenIntrospector:
  - 토큰 검증 → claims["sub"] 추출
  - sub.toLongOrNull() == null → client_credentials 토큰 → ROLE_SYSTEM 반환 (user-active 이벤트 발행 없음)
  - sub.toLongOrNull() != null → userId(Long), claims["username"] → username
    - Redis jwt:authorities:{username} 조회 → 권한 부여
    - user-active 토픽에 {"userId": Long} 발행 (authorization-server가 user_activity UPSERT)
    - Redis miss 시 OAuth2IntrospectionException 발생 (fail-closed — 권한 불명 시 접근 거부)
  - DefaultOAuth2AuthenticatedPrincipal(name=username or clientId, attributes=claims, authorities)
- authorization-server OAuth2TokenCustomizerConfig에 OAuth2TokenClaimsContext customizer 추가
  - 유저 토큰: sub = userId.toString() (introspector의 toLongOrNull() 기준), username 클레임 추가
  - client_credentials: customizer 미적용 → sub = client_id (Spring 기본값, 비숫자 문자열)
- Spring Security 7 OpaqueTokenIntrospector 구현체로 등록 (RestClient 직접 호출)
- application-k8s.yaml introspection 설정: introspection-uri / client-id: opaque-server-client / client-secret: ${OPAQUE_CLIENT_SECRET}
  - client-id 누락 시 authorization-server introspect 엔드포인트 인증 실패 → 모든 토큰 검증 불가
- SecurityConfig 경로별 권한:
  - /admin/** → hasAnyRole("ADMIN", "SYSTEM")
  - /payments/** → hasAnyRole("USER", "ADMIN")

결제 (Saga Pattern)
- PaymentService.initiate() → Payment + PaymentSaga(INITIATED) DB 저장 → payment.saga 토픽 발행
- PaymentSagaConsumer: PAYMENT_INITIATED 수신 → PaymentService.complete() 호출 (실제 환경에서는 외부 PG 연동)
- Saga 상태 이력: payment_sagas 테이블에 단계별 기록 (INITIATED → COMPLETED / COMPENSATION)
- 조회: GET /admin/payments/{id}/saga 로 전체 이력 확인

일반 사용자 결제 API (ROLE_USER)
- POST   /payments         → 결제 시작 (principal에서 userId/username 추출)
- GET    /payments/{id}    → 본인 결제 조회 (DB WHERE id=? AND user_id=? 로 소유권 검증, 타인 접근 시 404)
- GET    /payments         → 본인 결제 목록 (created_at DESC)
- 어드민 API와 동일한 PaymentService.initiate() 재사용, 조회는 getByIdAndUserId / getByUserId 신규 메서드

이메일 알림
- NotificationEventConsumer: jwt-server의 notifications 토픽 구독
  - POST_LIKED, POST_COMMENTED, USER_FOLLOWED 이벤트 수신 → NotificationService → MailAdapter
  - 이벤트 payload에서 targetId(수신자 userId) + target(수신자 username snapshot) 추출
- NotificationService.send(recipientId, recipientUsername, type, content)
  - recipient_id + recipient_username 이중 저장 (설계 원칙 적용)
  - 이메일 조회: UserRestRepository.findById(recipientId) → auth-server /internal/users/{id} 호출
  - 이메일 없으면 PENDING 상태로 보존 (경고 로그)
- MailAdapter: JavaMailSender (Gmail SMTP) 사용
- notifications 테이블에 발송 이력 저장 (PENDING → SENT / FAILED)

유저 관리
- POST /admin/users/{userId}/suspend — 정지
- POST /admin/users/{userId}/ban — 영구 밴
- POST /admin/users/{userId}/restore — 복구
- DELETE /admin/users/{userId} — 삭제
- 액션 시 user-management 토픽 발행 (payload: {"action":"...", "userId": Long})
  → authorization-server가 소비하여 userId로 DB 업데이트 + Redis 무효화
- actorId: principal.getAttribute("sub") (opaque token claims의 userId 문자열)
- actorUsername: principal.name (opaque token introspection sub = username)
- audit_logs에 actor_id + actor_username 이중 저장 (설계 원칙 적용)

신고 처리
- ReportEventConsumer: jwt-server의 reports 토픽 구독 → opaque_db reports 테이블에 저장
  - external_id(jwt-server report ID)로 중복 수신 방지
- POST /admin/reports/{id}/dismiss — 기각
- POST /admin/reports/{id}/action — 조치 완료 → report-actions 토픽 발행
- ReportController: actorId = principal.getAttribute("sub"), actorUsername = principal.name

감사 로그 (Audit Log)
- 모든 어드민 액션(유저 관리, 신고 처리, 결제)은 audit_logs 테이블에 기록
- AuditService.log(actorId, actorUsername, action, targetType, targetId, detail)
- GET /admin/audit-logs (offset/limit 페이지네이션)

통계
- GET /admin/statistics: 신고 상태별 수, 총 결제 수익 집계 (유저 상태 통계는 authorization-server가 담당)

Kafka 토픽
- 구독(Consumer): notifications, reports, payment.saga
- 발행(Producer): payment.saga, report-actions, user-active (KafkaTemplate 직접 발행)
- Outbox 릴레이 발행: user-management (OutboxRelayScheduler → 1초 주기, FOR UPDATE SKIP LOCKED, markSent 후 7일 보존)

authorization-server Kafka
- 구독(Consumer): user-management, user-active
  - user-management payload: {"action":"...", "userId": Long}
    - userId로 DB 조회하여 username 확인 후 Redis 무효화
    - SUSPEND / BAN / DELETE → setStatusAndEnabled(false) + jwt:authorities 삭제 + user 캐시 evict
    - RESTORE → setStatusAndEnabled(true, "ACTIVE") + user 캐시 evict
  - user-active payload: {"userId": Long} → user_activity UPSERT
- 발행(Producer): user-sync, user.username.updated
  - user-sync payload: {"userId": Long, "username": String, "enabled": Boolean, "status": String, "version": Long}
    - UserJdbcRepository.save() 완료 후 발행 (INSERT: version=1 고정 / UPDATE: version=version+1 RETURNING)
    - setStatusAndEnabled() 완료 후 발행 (UPDATE...RETURNING으로 변경, version 포함)
    - version: DB 업데이트마다 단조 증가 — jwt-server Consumer의 out-of-order 판정 기준
    - jwt-server가 소비하여 authorization_users UPSERT (user_id, username, enabled, status, version)
  - user.username.updated payload: {"userId": Long, "newUsername": String, "version": Long}
    - UserJdbcRepository.updateUsername() 완료 후 발행 (version=version+1 RETURNING)
    - jwt-server UsernameUpdateEventConsumer → syncUsername() 호출 (username만 업데이트, enabled/status 유지)

DDL (opaque_db) — FK 논리적 사용
- payments: id, user_id (logical FK → authorization_db.users.id), order_id(UNIQUE), amount, status
- payment_sagas: id, payment_id (logical FK → payments.id), step, status, detail
- reports: id, external_id(UNIQUE), reporter_username, target_type, target_id, reason, status, reviewed_by, reviewed_at
- audit_logs: id, actor_id VARCHAR(50) (logical FK → authorization_db.users.id), actor_username VARCHAR(50) (snapshot), action, target_type, target_id, detail, created_at
  - actor_id: 행위자 식별 기준 (변경 불변), actor_username: 행위 시점 username 보존 (감사 이력)
- notifications: id, recipient_id VARCHAR(50) (logical FK → authorization_db.users.id), recipient_username VARCHAR(50) (snapshot), type, content, status, sent_at, created_at
  - recipient_id: 수신자 식별/이메일 조회 기준, recipient_username: 발송 시점 username 보존
- outbox_events: id, aggregate_id, aggregate_type, event_type, payload(JSONB), claimed_at(TIMESTAMPTZ), sent_at(TIMESTAMPTZ), created_at
  - 부분 인덱스: idx_outbox_unclaimed ON (created_at ASC, id ASC) WHERE claimed_at IS NULL AND sent_at IS NULL

Clean Architecture 구조
- domain: Payment(PaymentStatus), PaymentSaga(SagaStep/SagaStatus), Report(ReportStatus), AuditLog(actorId+actorUsername), Notification(recipientId+recipientUsername, NotificationStatus), User(id/username/email), OutboxEvent(id/aggregateId/aggregateType/eventType/payload/claimedAt/sentAt/createdAt), OutboxRepository(save/findAndClaim/markSent/unclaim/resetStaleClaims/deleteProcessed)
- domain/user: User(id: String, username, email), UserRepository(findById(id: String))
- application/port/out: EventPublishPort, EmailPort
- application/service: UserManagementService (actorId+actorUsername 모두 수신 — audit_log + outbox_events 단일 트랜잭션), PaymentService (userId.toString()을 actorId로 사용), ReportProcessingService (actorId+actorUsername 수신), AuditService (log(actorId, actorUsername, ...)), NotificationService (send(recipientId, recipientUsername, ...)), StatisticsService (유저 통계 제외)
- infrastructure/persistence: PaymentJdbcRepository (findByIdAndUserId/findByUserId 추가), PaymentSagaJdbcRepository, ReportJdbcRepository, AuditJdbcRepository (actor_id+actor_username INSERT/SELECT), NotificationJdbcRepository (recipient_id+recipient_username INSERT), OutboxJdbcRepository (findAndClaim: UPDATE SET claimed_at=NOW() FOR UPDATE SKIP LOCKED RETURNING / markSent / unclaim / resetStaleClaims / deleteProcessed)
- infrastructure/relay: OutboxRelayScheduler (@Scheduled fixedDelay=1s initialDelay=5s — findAndClaim → Kafka send(.get(5s)) → markSent; 실패 시 unclaim + return; cleanupStaleClaims 30s 주기; cleanupProcessed 1h 주기)
- infrastructure/rest: UserRestRepository (UserRepository 구현체, auth-server /internal/users/{id} REST 호출 — 실패 시 null 반환, notification email 조회용)
- infrastructure/kafka: KafkaEventPublisher, NotificationEventConsumer (targetId+target 추출 → send(recipientId, recipientUsername, ...)), ReportEventConsumer, PaymentSagaConsumer
- infrastructure/mail: MailAdapter (JavaMailSender)
- infrastructure/security: CustomOpaqueTokenIntrospector (sub.toLongOrNull()로 유저/시스템 분기 — Long이면 유저(Redis 조회+user-active 발행), null이면 ROLE_SYSTEM 반환, Redis miss 시 예외 발생 fail-closed)
- infrastructure/config: SecurityConfig (CORS + OPTIONS permitAll), RedisConfig (Jackson 3.x, authorization-server와 동일 직렬화), SwaggerConfig, WebMvcConfig (RateLimitInterceptor → /admin/**, /payments/**)
- application/service: UserManagementService — ObjectMapper import를 tools.jackson.databind.ObjectMapper로 수정 (Spring Boot 4.0 Jackson 3.x 호환), KafkaTopicConfig (payment.saga/user-management/report-actions/user-active, 파티션 3), KafkaProducerConfig (KafkaTemplate<String,String> 명시적 빈), SchedulingConfig (@EnableScheduling)
- presentation:
  - UserManagementController ({userId} path var, @PreAuthorize ADMIN+SYSTEM, actorId=principal.getAttribute("sub"), actorUsername=principal.name)
  - PaymentController (ADMIN 전용, /admin/payments — initiate에서 sub.toLongOrNull() 필요하므로 SYSTEM 불가)
  - UserPaymentController (ROLE_USER 전용, /payments — 소유권 검증)
  - ReportController (@PreAuthorize ADMIN+SYSTEM, actorId+actorUsername 추출)
  - StatisticsController (@PreAuthorize ADMIN+SYSTEM)
  - GlobalExceptionHandler
    
~~gateway-server~~ (Deprecated — K8s Gateway API + Istio로 대체)
- ~~port 1100~~
- ~~WebFlux (Spring Cloud Gateway) 기반~~

~~JWT 검증 (Zero Trust — 서명 검증만, 인가 없음)~~
- ~~authorization-server JWK Set으로 JWT 서명 검증 (issuer-uri / jwk-set-uri → localhost:1010)~~
- ~~헤더 주입 없음 — Authorization 헤더를 그대로 downstream 전달~~
- ~~인가 결정은 각 서비스(jwt-server, opaque-server)가 독립적으로 수행~~
- ~~Redis 의존성 없음 (rate limiting은 Spring Cloud Gateway 내장 Redis 연동)~~
- ~~authorization-server는 gateway를 거치지 않음 (OAuth2 브라우저 리다이렉트 특성상 직접 접근)~~

~~라우팅~~
- ~~/auth/**      → authorization-server (permitAll, JWT 불필요)~~
- ~~/api/**       → jwt-server     (authenticated, rate limit 20req/s burst 40)~~
- ~~/admin/**     → opaque-server  (authenticated, rate limit 10req/s burst 20)~~
- ~~/payments/**  → opaque-server  (authenticated, rate limit 20req/s burst 40)~~

~~필터 실행 순서~~
~~1. LoggingFilter (order=HIGHEST_PRECEDENCE) — 요청 수신 로그~~
~~2. Spring Security WebFilter — JWT 서명 검증 / permitAll 경로 통과~~
~~3. Spring Cloud Gateway 라우팅 + RequestRateLimiter~~
~~4. LoggingFilter doFinally — 응답 상태·소요시간 로그~~

Rate Limiting (→ 각 서비스로 이관)
- ~~기존: Redis 기반 Token Bucket (RequestRateLimiter 필터, gateway-server 담당)~~
- 현재: jwt-server / opaque-server가 자체 HandlerInterceptor + Redis Lua 스크립트로 처리
  - gateway-server 의존 없이 각 서비스가 독립적으로 제한 (Zero Trust 일관성)

CORS (→ 각 서비스로 이관)
- ~~기존: globalcors + DedupeResponseHeader 필터 (gateway-server 담당)~~
- 현재: jwt-server / opaque-server / authorization-server SecurityConfig에서 자체 처리

~~Clean Architecture 구조~~
- ~~infrastructure/config: SecurityConfig (JWT 서명 검증), RateLimiterConfig, SwaggerConfig~~
- ~~infrastructure/filter: LoggingFilter (GlobalFilter)~~

~~config-server~~ (Deprecated — K8s ConfigMap/Secret으로 대체)
- ~~port 1110~~
- ~~native profile (classpath:/config)~~
- ~~환경별 프로파일 (dev / prod)~~
- ~~암호화된 설정값 관리 ('{cipher}' prefix, encrypt.key)~~
- ~~Spring Cloud Bus + Kafka로 설정 변경 자동 반영~~
- ~~서버별 설정 파일 구조: {service}.yaml / {service}-dev.yaml / {service}-prod.yaml~~

~~eureka-server~~ (Deprecated — K8s Service DNS로 대체)
- ~~port 1120~~
- ~~Spring Security 기본 인증 (admin/admin)~~
- ~~self-preservation 비활성화~~
- ~~eviction-interval-timer-in-ms: 5000 (기본 60000 → 만료 인스턴스 빠른 제거)~~
- ~~response-cache-update-interval-ms: 3000 (기본 30000 → 레지스트리 캐시 갱신 주기)~~

K8s 배포 (k8s/)
Phase 1 (완료): Helm values — Bitnami PostgreSQL·Redis·Kafka·MinIO + LGTM (Loki·Tempo·Mimir·Alloy·Grafana)
Phase 2 (완료): 서비스 매니페스트 — Deployment·Service·ConfigMap·Secret·HPA·PodDisruptionBudget
- Deployment 공통 설정 (authorization-server / jwt-server / opaque-server)
  - replicas: 2
  - strategy: RollingUpdate (maxSurge: 1, maxUnavailable: 0) — 신규 파드 Ready 후 구 파드 제거 (무중단)
Phase 3 (완료): Istio + K8s Gateway API + cert-manager

k8s/gateway/
- gatewayclass.yaml: Istio GatewayClass (controllerName: istio.io/gateway-controller)
- gateway.yaml: HTTP(80) + HTTPS(443) 리스너, cert-manager.io/cluster-issuer: letsencrypt-prod
  - hostname: api.example.com (배포 전 실제 도메인으로 교체 필요)
- httproutes.yaml: HTTP→HTTPS 301 리다이렉트 + 경로별 라우팅 (5초 타임아웃)
  - /auth → authorization-server:80
  - /api  → jwt-server:80
  - /admin, /payments → opaque-server:80

k8s/istio/
- peer-authentication.yaml: mTLS STRICT (평문 접근 차단, 사이드카 없는 호출자 거부)
- destination-rules.yaml: 서비스별 outlierDetection (consecutiveGatewayErrors:5, interval:10s, baseEjectionTime:10s)
  - Resilience4j Circuit Breaker를 Istio DestinationRule outlierDetection으로 대체
- request-authentication.yaml: jwt-server 대상 JWT 검증 위임
  - issuer: AUTH_ISSUER_URI 실제 외부 공개 도메인 (배포 전 교체 필요)
  - jwksUri: http://authorization-server.msa.svc.cluster.local:1010/oauth2/jwks (클러스터 내부 조회)
  - forwardOriginalToken: true (앱까지 Authorization 헤더 전달, JwtClaimsFilter 파싱에 필요)
- authorization-policy.yaml: jwt-server 인가 정책
  - jwt-server-allow-public: /actuator/health, /v3/api-docs/**, /swagger-ui/**, OPTIONS → 인증 없이 허용
  - jwt-server-require-jwt: requestPrincipals: ["*"] → 유효한 JWT 있는 요청만 허용
  - opaque-server: 앱 레벨 introspection 유지 (Istio 위임 없음)

k8s/cert-manager/
- cluster-issuer.yaml: Let's Encrypt ACME prod, HTTP-01 solver (gatewayHTTPRoute)
- certificate.yaml: secretName: msa-tls (배포 전 실제 도메인으로 교체 필요)

k8s/helm/
- reloader-values.yaml: Stakater Reloader (watchGlobally: false)
  - Deployment annotation: secret.reloader.stakater.com/reload 으로 시크릿 변경 시 자동 롤링 재시작

⚠️ 배포 전 교체 필요: api.example.com (도메인), user@example.com (cert-manager 이메일), auth.example.com (request-authentication.yaml issuer — AUTH_ISSUER_URI 실제 값)
⚠️ Git 커밋 금지: k8s/apps/*-secret.yaml (실제 값 채운 후 kubectl apply만)

LGTM 모니터링 스택 (ELK·Zipkin·Prometheus 대체)
- 도입 목적: 관찰성의 세 가지 기둥(로그·메트릭·트레이스)을 하나의 플랫폼에서 통합적으로 다룰 수 있다
- Loki: 로그 집계 (Loki4jAppender가 K8s 환경에서 직접 push)
  - 핵심 철학: 로그 내용을 인덱싱하지 말고, 메타데이터만 인덱싱하자 → 저장 비용 절감, 스트림 기반 쿼리
- Tempo: 분산 추적 (OTLP HTTP 4318 / gRPC 4317 수신)
- Mimir: 메트릭 장기 저장 (Prometheus 호환 remote_write API)
- Alloy: 메트릭 수집기 (Spring Boot Actuator /actuator/prometheus 스크레이프 → Mimir 전송), 인프라 Pod 로그 수집
- Grafana: 통합 시각화 (Mimir·Loki·Tempo 데이터소스 자동 프로비저닝, TraceID ↔ 로그 연동)
- Elasticsearch: jwt-server 검색 기능 전용으로 유지 (로그 수집 목적 제거)
