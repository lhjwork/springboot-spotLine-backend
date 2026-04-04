# social-interactions-backend Design

## 1. 엔티티 설계

### 1.1 User.java (신규)

```java
// domain/entity/User.java
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    private String id; // Supabase UUID (JWT sub) — 자동생성 아님

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nickname;

    private String avatar;
    private String bio;
    private String instagramId;

    @Builder.Default
    private Integer followersCount = 0;

    @Builder.Default
    private Integer followingCount = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

- `@Id` — `@GeneratedValue` 없음 (Supabase UUID 직접 사용)
- nickname 기본값: email의 `@` 앞부분

### 1.2 SpotLike.java (신규)

```java
// domain/entity/SpotLike.java
@Entity
@Table(name = "spot_likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "spot_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SpotLike {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

### 1.3 SpotSave.java (신규)

```java
// domain/entity/SpotSave.java
@Entity
@Table(name = "spot_saves", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "spot_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SpotSave {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

### 1.4 RouteLike.java (신규)

```java
// domain/entity/RouteLike.java
@Entity
@Table(name = "spotline_likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "route_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RouteLike {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

### 1.5 RouteSave.java (신규)

```java
// domain/entity/RouteSave.java
@Entity
@Table(name = "spotline_saves", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "route_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RouteSave {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

### 1.6 UserFollow.java (신규)

```java
// domain/entity/UserFollow.java
@Entity
@Table(name = "user_follows", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"follower_id", "following_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserFollow {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "follower_id", nullable = false)
    private String followerId;

    @Column(name = "following_id", nullable = false)
    private String followingId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

### 1.7 UserRoute.java (신규)

```java
// domain/entity/UserRoute.java
@Entity
@Table(name = "user_spotlines", indexes = {
    @Index(name = "idx_user_spotlines_user_id", columnList = "userId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    private String scheduledDate; // "2026-04-01" or null

    @Builder.Default
    private String status = "scheduled"; // "scheduled" | "completed" | "cancelled"

    private LocalDateTime completedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

---

## 2. Repository 설계

### 2.1 UserRepository.java

```java
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
}
```

### 2.2 SpotLikeRepository.java

```java
public interface SpotLikeRepository extends JpaRepository<SpotLike, UUID> {
    Optional<SpotLike> findByUserIdAndSpot(String userId, Spot spot);
    boolean existsByUserIdAndSpot(String userId, Spot spot);
    Page<SpotLike> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
```

### 2.3 SpotSaveRepository.java

```java
public interface SpotSaveRepository extends JpaRepository<SpotSave, UUID> {
    Optional<SpotSave> findByUserIdAndSpot(String userId, Spot spot);
    boolean existsByUserIdAndSpot(String userId, Spot spot);
    Page<SpotSave> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
```

### 2.4 RouteLikeRepository.java

```java
public interface RouteLikeRepository extends JpaRepository<RouteLike, UUID> {
    Optional<RouteLike> findByUserIdAndRoute(String userId, Route route);
    boolean existsByUserIdAndRoute(String userId, Route route);
    Page<RouteLike> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
```

### 2.5 RouteSaveRepository.java

```java
public interface RouteSaveRepository extends JpaRepository<RouteSave, UUID> {
    Optional<RouteSave> findByUserIdAndRoute(String userId, Route route);
    boolean existsByUserIdAndRoute(String userId, Route route);
    Page<RouteSave> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
```

### 2.6 UserFollowRepository.java

```java
public interface UserFollowRepository extends JpaRepository<UserFollow, UUID> {
    Optional<UserFollow> findByFollowerIdAndFollowingId(String followerId, String followingId);
    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);
    Page<UserFollow> findByFollowingIdOrderByCreatedAtDesc(String followingId, Pageable pageable);
    Page<UserFollow> findByFollowerIdOrderByCreatedAtDesc(String followerId, Pageable pageable);
}
```

### 2.7 UserRouteRepository.java

```java
public interface UserRouteRepository extends JpaRepository<UserRoute, UUID> {
    Page<UserRoute> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Page<UserRoute> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status, Pageable pageable);
}
```

---

## 3. 서비스 설계

### 3.1 AuthUtil.java (신규)

```java
// security/AuthUtil.java
@Component
public class AuthUtil {
    /**
     * SecurityContext에서 현재 인증된 userId 추출
     * @return userId or null (미인증 시)
     */
    public String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return (String) auth.getPrincipal();
        }
        return null;
    }

    /**
     * 인증 필수 — 미인증 시 예외
     */
    public String requireUserId() {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        }
        return userId;
    }
}
```

### 3.2 UserSyncService.java (신규)

```java
// service/UserSyncService.java
@Service
@RequiredArgsConstructor
public class UserSyncService {
    private final UserRepository userRepository;

    /**
     * JWT userId + email로 User 조회 또는 생성
     * 첫 API 호출 시 자동 생성 (Lazy Sync)
     */
    public User getOrCreateUser(String userId, String email) {
        return userRepository.findById(userId)
            .orElseGet(() -> userRepository.save(
                User.builder()
                    .id(userId)
                    .email(email)
                    .nickname(email.split("@")[0])
                    .build()
            ));
    }
}
```

- JwtAuthenticationFilter에서 email을 SecurityContext에 저장하는 방식이 필요
- 또는 AuthUtil에서 Claims의 email도 추출

### 3.3 SocialService.java (신규)

```java
// service/SocialService.java
@Service
@RequiredArgsConstructor
@Transactional
public class SocialService {
    private final SpotRepository spotRepository;
    private final RouteRepository routeRepository;
    private final SpotLikeRepository spotLikeRepository;
    private final SpotSaveRepository spotSaveRepository;
    private final RouteLikeRepository routeLikeRepository;
    private final RouteSaveRepository routeSaveRepository;

    /** Spot 좋아요 토글 */
    public SocialToggleResponse toggleSpotLike(String userId, UUID spotId) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<SpotLike> existing = spotLikeRepository.findByUserIdAndSpot(userId, spot);
        boolean liked;
        if (existing.isPresent()) {
            spotLikeRepository.delete(existing.get());
            spot.setLikesCount(spot.getLikesCount() - 1);
            liked = false;
        } else {
            spotLikeRepository.save(SpotLike.builder().userId(userId).spot(spot).build());
            spot.setLikesCount(spot.getLikesCount() + 1);
            liked = true;
        }
        spotRepository.save(spot);
        return new SocialToggleResponse(liked, null, spot.getLikesCount(), spot.getSavesCount());
    }

    /** Spot 저장 토글 */
    public SocialToggleResponse toggleSpotSave(String userId, UUID spotId) {
        // toggleSpotLike와 동일 패턴, SpotSave + savesCount 사용
    }

    /** Route 좋아요 토글 */
    public SocialToggleResponse toggleRouteLike(String userId, UUID spotLineId) {
        // Spot과 동일 패턴, Route + RouteLike 사용
    }

    /** Route 저장 토글 */
    public SocialToggleResponse toggleRouteSave(String userId, UUID spotLineId) {
        // 동일 패턴
    }

    /** Spot 소셜 상태 조회 (로그인 유저용) */
    public SocialStatusResponse getSpotSocialStatus(String userId, UUID spotId) {
        Spot spot = spotRepository.findById(spotId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new SocialStatusResponse(
            spotLikeRepository.existsByUserIdAndSpot(userId, spot),
            spotSaveRepository.existsByUserIdAndSpot(userId, spot)
        );
    }

    /** Route 소셜 상태 조회 */
    public SocialStatusResponse getRouteSocialStatus(String userId, UUID spotLineId) {
        // Spot과 동일 패턴
    }
}
```

### 3.4 FollowService.java (신규)

```java
// service/FollowService.java
@Service
@RequiredArgsConstructor
@Transactional
public class FollowService {
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;

    /** 팔로우 */
    public FollowResponse follow(String followerId, String followingId) {
        if (followerId.equals(followingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신을 팔로우할 수 없습니다");
        }
        if (userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 팔로우 중입니다");
        }
        userFollowRepository.save(UserFollow.builder()
            .followerId(followerId).followingId(followingId).build());

        User following = userRepository.findById(followingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        following.setFollowersCount(following.getFollowersCount() + 1);
        userRepository.save(following);

        User follower = userRepository.findById(followerId).orElseThrow();
        follower.setFollowingCount(follower.getFollowingCount() + 1);
        userRepository.save(follower);

        return new FollowResponse(true, following.getFollowersCount());
    }

    /** 언팔로우 */
    public FollowResponse unfollow(String followerId, String followingId) {
        UserFollow uf = userFollowRepository.findByFollowerIdAndFollowingId(followerId, followingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        userFollowRepository.delete(uf);

        User following = userRepository.findById(followingId).orElseThrow();
        following.setFollowersCount(Math.max(0, following.getFollowersCount() - 1));
        userRepository.save(following);

        User follower = userRepository.findById(followerId).orElseThrow();
        follower.setFollowingCount(Math.max(0, follower.getFollowingCount() - 1));
        userRepository.save(follower);

        return new FollowResponse(false, following.getFollowersCount());
    }

    /** 팔로우 상태 */
    public boolean isFollowing(String followerId, String followingId) {
        return userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    /** 팔로워 목록 */
    public Page<User> getFollowers(String userId, Pageable pageable) {
        return userFollowRepository.findByFollowingIdOrderByCreatedAtDesc(userId, pageable)
            .map(uf -> userRepository.findById(uf.getFollowerId()).orElse(null));
    }

    /** 팔로잉 목록 */
    public Page<User> getFollowing(String userId, Pageable pageable) {
        return userFollowRepository.findByFollowerIdOrderByCreatedAtDesc(userId, pageable)
            .map(uf -> userRepository.findById(uf.getFollowingId()).orElse(null));
    }
}
```

### 3.5 UserRouteService.java (신규)

```java
// service/UserRouteService.java
@Service
@RequiredArgsConstructor
@Transactional
public class UserRouteService {
    private final UserRouteRepository userRouteRepository;
    private final RouteRepository routeRepository;

    /** Route 복제 */
    public ReplicateRouteResponse replicate(String userId, UUID spotLineId, String scheduledDate) {
        Route route = routeRepository.findById(spotLineId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        UserRoute userRoute = userRouteRepository.save(UserRoute.builder()
            .userId(userId)
            .route(route)
            .scheduledDate(scheduledDate)
            .status("scheduled")
            .build());

        route.setReplicationsCount(route.getReplicationsCount() + 1);
        routeRepository.save(route);

        return ReplicateRouteResponse.from(userRoute, route);
    }

    /** 내 Route 목록 */
    public Page<UserRoute> getMyRoutes(String userId, String status, Pageable pageable) {
        if (status != null) {
            return userRouteRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        }
        return userRouteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /** 상태 변경 */
    public UserRoute updateStatus(String userId, UUID myRouteId, String status) {
        UserRoute ur = userRouteRepository.findById(myRouteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ur.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        ur.setStatus(status);
        if ("completed".equals(status)) {
            ur.setCompletedAt(LocalDateTime.now());
        }
        return userRouteRepository.save(ur);
    }

    /** 삭제 */
    public void delete(String userId, UUID myRouteId) {
        UserRoute ur = userRouteRepository.findById(myRouteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ur.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        userRouteRepository.delete(ur);
    }
}
```

---

## 4. DTO 설계 (Front 스펙 준수)

### 4.1 Response DTOs

```java
// dto/response/SocialToggleResponse.java
@Data @AllArgsConstructor
public class SocialToggleResponse {
    private Boolean liked;    // null if save toggle
    private Boolean saved;    // null if like toggle
    private Integer likesCount;
    private Integer savesCount;
}

// dto/response/SocialStatusResponse.java
@Data @AllArgsConstructor
public class SocialStatusResponse {
    private boolean isLiked;
    private boolean isSaved;
}

// dto/response/FollowResponse.java
@Data @AllArgsConstructor
public class FollowResponse {
    private boolean followed;
    private Integer followersCount;
}

// dto/response/FollowStatusResponse.java
@Data @AllArgsConstructor
public class FollowStatusResponse {
    private boolean isFollowing;
}

// dto/response/UserProfileResponse.java
@Data @Builder
public class UserProfileResponse {
    private String id;
    private String nickname;
    private String avatar;
    private String bio;
    private String joinedAt;  // ISO string
    private String instagramId;
    private UserStatsResponse stats;

    @Data @Builder
    public static class UserStatsResponse {
        private int visited;
        private int liked;
        private int recommended;
        private int spotlines;
        private int followers;
        private int following;
    }

    public static UserProfileResponse from(User user, int likedCount, int savedCount) {
        return UserProfileResponse.builder()
            .id(user.getId())
            .nickname(user.getNickname())
            .avatar(user.getAvatar())
            .bio(user.getBio())
            .joinedAt(user.getCreatedAt().toString())
            .instagramId(user.getInstagramId())
            .stats(UserStatsResponse.builder()
                .visited(0) // 향후 구현
                .liked(likedCount)
                .recommended(0) // 향후 구현
                .spotlines(savedCount)
                .followers(user.getFollowersCount())
                .following(user.getFollowingCount())
                .build())
            .build();
    }
}

// dto/response/MyRouteResponse.java
@Data @Builder
public class MyRouteResponse {
    private String id;
    private String spotLineId;
    private String routeSlug;
    private String title;
    private String area;
    private int spotsCount;
    private String scheduledDate;
    private String status;
    private String completedAt;
    private String parentSpotLineId;
    private String createdAt;

    public static MyRouteResponse from(UserRoute ur) {
        Route route = ur.getRoute();
        return MyRouteResponse.builder()
            .id(ur.getId().toString())
            .spotLineId(route.getId().toString())
            .routeSlug(route.getSlug())
            .title(route.getTitle())
            .area(route.getArea())
            .spotsCount(route.getSpots().size())
            .scheduledDate(ur.getScheduledDate())
            .status(ur.getStatus())
            .completedAt(ur.getCompletedAt() != null ? ur.getCompletedAt().toString() : null)
            .parentSpotLineId(route.getId().toString())
            .createdAt(ur.getCreatedAt().toString())
            .build();
    }
}

// dto/response/ReplicateRouteResponse.java
@Data @AllArgsConstructor
public class ReplicateRouteResponse {
    private MyRouteResponse myRoute;
    private Integer replicationsCount;

    public static ReplicateRouteResponse from(UserRoute ur, Route route) {
        return new ReplicateRouteResponse(
            MyRouteResponse.from(ur),
            route.getReplicationsCount()
        );
    }
}

// dto/response/SimplePageResponse.java — { items, hasMore } 형식
@Data @AllArgsConstructor
public class SimplePageResponse<T> {
    private List<T> items;
    private boolean hasMore;

    public static <T> SimplePageResponse<T> from(Page<T> page) {
        return new SimplePageResponse<>(page.getContent(), page.hasNext());
    }
}
```

### 4.2 Request DTOs

```java
// dto/request/ReplicateRouteRequest.java
@Data
public class ReplicateRouteRequest {
    private String scheduledDate; // nullable
}

// dto/request/UpdateMyRouteStatusRequest.java
@Data
public class UpdateMyRouteStatusRequest {
    private String status; // "completed" | "cancelled"
}
```

---

## 5. 컨트롤러 설계

### 5.1 SocialController.java (신규)

```java
// controller/SocialController.java
@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class SocialController {
    private final SocialService socialService;
    private final AuthUtil authUtil;

    // POST /api/v2/spots/{id}/like
    @PostMapping("/spots/{id}/like")
    public SocialToggleResponse toggleSpotLike(@PathVariable UUID id) {
        return socialService.toggleSpotLike(authUtil.requireUserId(), id);
    }

    // POST /api/v2/spots/{id}/save
    @PostMapping("/spots/{id}/save")
    public SocialToggleResponse toggleSpotSave(@PathVariable UUID id) {
        return socialService.toggleSpotSave(authUtil.requireUserId(), id);
    }

    // POST /api/v2/routes/{id}/like
    @PostMapping("/routes/{id}/like")
    public SocialToggleResponse toggleRouteLike(@PathVariable UUID id) {
        return socialService.toggleRouteLike(authUtil.requireUserId(), id);
    }

    // POST /api/v2/routes/{id}/save
    @PostMapping("/routes/{id}/save")
    public SocialToggleResponse toggleRouteSave(@PathVariable UUID id) {
        return socialService.toggleRouteSave(authUtil.requireUserId(), id);
    }

    // GET /api/v2/spots/{id}/social
    @GetMapping("/spots/{id}/social")
    public SocialStatusResponse getSpotSocial(@PathVariable UUID id) {
        String userId = authUtil.getCurrentUserId();
        if (userId == null) return new SocialStatusResponse(false, false);
        return socialService.getSpotSocialStatus(userId, id);
    }

    // GET /api/v2/routes/{id}/social
    @GetMapping("/routes/{id}/social")
    public SocialStatusResponse getRouteSocial(@PathVariable UUID id) {
        String userId = authUtil.getCurrentUserId();
        if (userId == null) return new SocialStatusResponse(false, false);
        return socialService.getRouteSocialStatus(userId, id);
    }
}
```

### 5.2 FollowController.java (신규)

```java
// controller/FollowController.java
@RestController
@RequestMapping("/api/v2/users")
@RequiredArgsConstructor
public class FollowController {
    private final FollowService followService;
    private final AuthUtil authUtil;
    private final UserSyncService userSyncService;

    // POST /api/v2/users/{userId}/follow
    @PostMapping("/{userId}/follow")
    public FollowResponse follow(@PathVariable String userId) {
        return followService.follow(authUtil.requireUserId(), userId);
    }

    // DELETE /api/v2/users/{userId}/follow
    @DeleteMapping("/{userId}/follow")
    public FollowResponse unfollow(@PathVariable String userId) {
        return followService.unfollow(authUtil.requireUserId(), userId);
    }

    // GET /api/v2/users/{userId}/follow/status
    @GetMapping("/{userId}/follow/status")
    public FollowStatusResponse followStatus(@PathVariable String userId) {
        String currentUserId = authUtil.getCurrentUserId();
        if (currentUserId == null) return new FollowStatusResponse(false);
        return new FollowStatusResponse(followService.isFollowing(currentUserId, userId));
    }

    // GET /api/v2/users/{userId}/followers?page=0&size=20
    @GetMapping("/{userId}/followers")
    public Page<UserProfileResponse> getFollowers(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return followService.getFollowers(userId, PageRequest.of(page, size))
            .map(user -> UserProfileResponse.from(user, 0, 0));
    }

    // GET /api/v2/users/{userId}/following?page=0&size=20
    @GetMapping("/{userId}/following")
    public Page<UserProfileResponse> getFollowing(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return followService.getFollowing(userId, PageRequest.of(page, size))
            .map(user -> UserProfileResponse.from(user, 0, 0));
    }
}
```

### 5.3 UserController.java (신규)

```java
// controller/UserController.java
@RestController
@RequestMapping("/api/v2/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final AuthUtil authUtil;
    private final SpotLikeRepository spotLikeRepository;
    private final SpotSaveRepository spotSaveRepository;
    private final RouteSaveRepository routeSaveRepository;
    private final SocialService socialService;

    // GET /api/v2/users/{userId}/profile
    @GetMapping("/{userId}/profile")
    public UserProfileResponse getProfile(@PathVariable String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // likedCount, savedCount는 간단히 count 쿼리
        return UserProfileResponse.from(user, 0, 0);
    }

    // GET /api/v2/users/{userId}/likes/spots?page=0&size=12
    @GetMapping("/{userId}/likes/spots")
    public Page<SpotDetailResponse> getLikedSpots(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return spotLikeRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
            .map(sl -> SpotDetailResponse.from(sl.getSpot(), null));
    }

    // GET /api/v2/users/{userId}/saves/routes?page=0&size=12
    @GetMapping("/{userId}/saves/routes")
    public Page<RoutePreviewResponse> getSavedRoutes(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return routeSaveRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
            .map(rs -> RoutePreviewResponse.from(rs.getRoute()));
    }

    // GET /api/v2/users/me/saves?type=spot&page=0
    @GetMapping("/me/saves")
    public SimplePageResponse<?> getMySaves(
            @RequestParam String type,
            @RequestParam(defaultValue = "0") int page) {
        String userId = authUtil.requireUserId();
        Pageable pageable = PageRequest.of(page, 20);

        if ("spot".equals(type)) {
            Page<SpotSave> saves = spotSaveRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            return new SimplePageResponse<>(
                saves.getContent().stream().map(s -> SpotDetailResponse.from(s.getSpot(), null)).toList(),
                saves.hasNext()
            );
        } else {
            Page<RouteSave> saves = routeSaveRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            return new SimplePageResponse<>(
                saves.getContent().stream().map(s -> RoutePreviewResponse.from(s.getRoute())).toList(),
                saves.hasNext()
            );
        }
    }
}
```

### 5.4 UserRouteController.java (신규)

```java
// controller/UserRouteController.java
@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class UserRouteController {
    private final UserRouteService userRouteService;
    private final AuthUtil authUtil;
    private final RouteRepository routeRepository;

    // POST /api/v2/routes/{spotLineId}/replicate
    @PostMapping("/routes/{spotLineId}/replicate")
    public ReplicateRouteResponse replicate(
            @PathVariable UUID spotLineId,
            @RequestBody ReplicateRouteRequest request) {
        return userRouteService.replicate(
            authUtil.requireUserId(), spotLineId, request.getScheduledDate());
    }

    // GET /api/v2/users/me/routes?status=scheduled&page=0
    @GetMapping("/users/me/routes")
    public SimplePageResponse<MyRouteResponse> getMyRoutes(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page) {
        Page<UserRoute> routes = userRouteService.getMyRoutes(
            authUtil.requireUserId(), status, PageRequest.of(page, 20));
        return new SimplePageResponse<>(
            routes.getContent().stream().map(MyRouteResponse::from).toList(),
            routes.hasNext()
        );
    }

    // PATCH /api/v2/users/me/routes/{myRouteId}
    @PatchMapping("/users/me/routes/{myRouteId}")
    public MyRouteResponse updateStatus(
            @PathVariable UUID myRouteId,
            @RequestBody UpdateMyRouteStatusRequest request) {
        return MyRouteResponse.from(
            userRouteService.updateStatus(authUtil.requireUserId(), myRouteId, request.getStatus()));
    }

    // DELETE /api/v2/users/me/routes/{myRouteId}
    @DeleteMapping("/users/me/routes/{myRouteId}")
    public void deleteMyRoute(@PathVariable UUID myRouteId) {
        userRouteService.delete(authUtil.requireUserId(), myRouteId);
    }

    // GET /api/v2/routes/{spotLineId}/variations?page=0
    @GetMapping("/routes/{spotLineId}/variations")
    public SimplePageResponse<RoutePreviewResponse> getVariations(
            @PathVariable UUID spotLineId,
            @RequestParam(defaultValue = "0") int page) {
        Route route = routeRepository.findById(spotLineId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // variations는 Route.variations (parentRoute 관계)
        // 간단히 Page로 변환
        List<RoutePreviewResponse> all = route.getVariations().stream()
            .filter(Route::getIsActive)
            .map(RoutePreviewResponse::from)
            .toList();
        int start = Math.min(page * 20, all.size());
        int end = Math.min(start + 20, all.size());
        return new SimplePageResponse<>(all.subList(start, end), end < all.size());
    }
}
```

---

## 6. SecurityConfig 변경

```java
// 추가 규칙
.requestMatchers(HttpMethod.GET, "/api/v2/users/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v2/spots/*/social").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v2/routes/*/social").permitAll()
// PATCH 추가
.requestMatchers(HttpMethod.PATCH, "/api/v2/**").authenticated()
```

---

## 7. 구현 순서 및 체크리스트

| # | 작업 | 파일 | 유형 |
|---|------|------|------|
| 1 | AuthUtil | `security/AuthUtil.java` | 신규 |
| 2 | User 엔티티 + Repository | `entity/User.java`, `repository/UserRepository.java` | 신규 |
| 3 | UserSyncService | `service/UserSyncService.java` | 신규 |
| 4 | SpotLike 엔티티 + Repository | `entity/SpotLike.java`, `repository/SpotLikeRepository.java` | 신규 |
| 5 | SpotSave 엔티티 + Repository | `entity/SpotSave.java`, `repository/SpotSaveRepository.java` | 신규 |
| 6 | RouteLike 엔티티 + Repository | `entity/RouteLike.java`, `repository/RouteLikeRepository.java` | 신규 |
| 7 | RouteSave 엔티티 + Repository | `entity/RouteSave.java`, `repository/RouteSaveRepository.java` | 신규 |
| 8 | UserFollow 엔티티 + Repository | `entity/UserFollow.java`, `repository/UserFollowRepository.java` | 신규 |
| 9 | UserRoute 엔티티 + Repository | `entity/UserRoute.java`, `repository/UserRouteRepository.java` | 신규 |
| 10 | Response DTOs (7개) | `dto/response/Social*.java`, `UserProfileResponse.java`, `MyRouteResponse.java` 등 | 신규 |
| 11 | Request DTOs (2개) | `dto/request/ReplicateRouteRequest.java`, `UpdateMyRouteStatusRequest.java` | 신규 |
| 12 | SocialService | `service/SocialService.java` | 신규 |
| 13 | SocialController (6 endpoints) | `controller/SocialController.java` | 신규 |
| 14 | FollowService | `service/FollowService.java` | 신규 |
| 15 | FollowController (5 endpoints) | `controller/FollowController.java` | 신규 |
| 16 | UserController (4 endpoints) | `controller/UserController.java` | 신규 |
| 17 | UserRouteService | `service/UserRouteService.java` | 신규 |
| 18 | UserRouteController (5 endpoints) | `controller/UserRouteController.java` | 신규 |
| 19 | SecurityConfig 업데이트 | `config/SecurityConfig.java` | 수정 |
| 20 | RoutePreviewResponse 확인 | `dto/response/RoutePreviewResponse.java` | 확인/수정 |

총 **20개 항목**, 신규 파일 약 30개, 수정 1개
