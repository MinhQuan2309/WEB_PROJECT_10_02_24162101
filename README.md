# WEB_PROJECT_10_02_24162101 - JWT với Spring Boot + Spring Security (Nimbus JOSE + JWT)

Bài tập môn Lập Trình Web (WEBPR330479) - bài giảng **04_JWT** (ThS. Nguyễn Hữu Trung).

Phần 2: dùng thư viện **Nimbus JOSE + JWT 10.10** (`com.nimbusds:nimbus-jose-jwt`) thay thế jjwt trong ví dụ của bài giảng.
Phần 1 (ví dụ gốc dùng jjwt): https://github.com/MinhQuan2309/WEB_PROJECT_10_01_24162101

## Công nghệ

- Java 26, Spring Boot 4.1.1, Spring Security 7, Spring Data JPA, Thymeleaf
- Nimbus JOSE + JWT 10.10, SQL Server, Lombok
- Giao diện: Bootstrap 5 + jQuery AJAX (`static/js/mainjs.js`)
- Port: **8006**, database: `jwt_nimbus`

## Thay jjwt bằng Nimbus JOSE + JWT

Toàn bộ thay đổi nằm ở `pom.xml`, [`services/JwtService.java`](src/main/java/vn/iotstar/services/JwtService.java) và các exception JWT trong package `exception`; các lớp còn lại giữ nguyên như bài giảng.

| Việc | jjwt | Nimbus |
|---|---|---|
| Dependency | `jjwt-api`, `jjwt-impl`, `jjwt-jackson` | `nimbus-jose-jwt` |
| Tạo claims | `Jwts.builder().claims(..).subject(..)` | `new JWTClaimsSet.Builder().claim(..).subject(..)` |
| Header | tự sinh | `new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT)` |
| Ký HS256 | `.signWith(key, Jwts.SIG.HS256).compact()` | `signedJWT.sign(new MACSigner(key)); signedJWT.serialize()` |
| Parse + verify | `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` | `SignedJWT.parse(token)` + `signedJWT.verify(new MACVerifier(key))` |
| Đọc claims | `Claims::getSubject`, `Claims::getExpiration` | `JWTClaimsSet::getSubject`, `JWTClaimsSet::getExpirationTime` |
| Token hết hạn | jjwt tự ném `ExpiredJwtException` | tự kiểm tra `exp`, ném `JwtExpiredException` |
| Sai chữ ký | `SignatureException` | `JwtSignatureException` |
| Sai định dạng | `MalformedJwtException` (`JwtException`) | `ParseException` -> `JwtTokenException` |

Ngoài ra `JwtService` chỉ chấp nhận token có `alg` là `HS256` để chặn kiểu tấn công đổi thuật toán trong header.

## Cấu trúc (theo các bước trong bài giảng)

```
src/main/java/vn/iotstar
├── entity/User.java                         Bước 2: Entity implements UserDetails
├── models/                                  Bước 3: LoginResponse, LoginUserModel, RegisterUserModel
├── repository/UserRepository.java           Bước 4: Repository
├── services/                                Bước 4: UserService, AuthenticationService, JwtService (Nimbus)
├── config/ApplicationConfiguration.java     Bước 5: UserDetailsService, PasswordEncoder, AuthenticationProvider
├── filter/JwtAuthenticationFilter.java      Bước 6: đọc header "Authorization: Bearer <token>"
├── config/SecurityConfiguration.java        Bước 7: SecurityFilterChain STATELESS + CORS
├── controller/                              Bước 8: AuthenticationController (/auth), UserController (/users)
│                                            Bước 10: AuthController (trang login, profile)
└── exception/                               Bước 10: GlobalExceptionHandler + JwtTokenException,
                                                      JwtSignatureException, JwtExpiredException
src/main/resources
├── templates/login.html, profile.html
└── static/js/mainjs.js, static/images/user.png
```

## Chạy project

1. Tạo database bằng file [`database.sql`](database.sql).
2. Tài khoản SQL Server mặc định là `sa` / `Sa@123456`; có thể đổi trong `application.properties` hoặc truyền biến môi trường `DB_USERNAME`, `DB_PASSWORD`.
3. Chạy `mvn spring-boot:run` (hoặc Run As > Spring Boot App trong STS).

## Test

**Đăng ký** - `POST http://localhost:8006/auth/signup`

```json
{ "email": "trungnh@hcmute.edu.vn", "password": "123456", "fullName": "Nguyễn Hữu Trung" }
```

**Đăng nhập lấy JWT** - `POST http://localhost:8006/auth/login`

```json
{ "email": "trungnh@hcmute.edu.vn", "password": "123456" }
```

Kết quả (header của token là `{"typ":"JWT","alg":"HS256"}`):

```json
{ "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9....", "expiresIn": 3600000 }
```

**Gọi API cần xác thực** - thêm header `Authorization: Bearer <token>` (Postman: tab Authorization > Bearer Token)

- `GET http://localhost:8006/users/me` - thông tin người dùng đang đăng nhập
- `GET http://localhost:8006/users/` - danh sách người dùng

**Các trường hợp lỗi**

| Trường hợp | Exception | HTTP | description |
|---|---|---|---|
| Sai email/mật khẩu | `BadCredentialsException` | 401 | The username or password is incorrect |
| Không gửi token | | 401 | |
| Token sai định dạng (vd `dfdfdfdf`) | `JwtTokenException` | 401 | The JWT token is invalid |
| Token bị sửa (sai chữ ký) | `JwtSignatureException` | 401 | The JWT signature is invalid |
| Token hết hạn | `JwtExpiredException` | 401 | The JWT token has expired |

**Giao diện AJAX**: mở `http://localhost:8006/login`, đăng nhập -> token lưu vào `localStorage` -> chuyển sang `/user/profile`, trang này gọi `/users/me` kèm Bearer token để hiển thị tên và ảnh. Nút Logout xoá token.

## Ghi chú khi chuyển từ Spring Boot 3 / Security 6 (bài giảng) lên Spring Boot 4 / Security 7

- `DaoAuthenticationProvider` không còn constructor rỗng và `setUserDetailsService()`; truyền `UserDetailsService` qua constructor.
- `AntPathRequestMatcher` đã bị bỏ; dùng thẳng `requestMatchers("/images/**")`.
- `HandlerExceptionResolver` có nhiều bean nên inject bằng `@Qualifier("handlerExceptionResolver")`.
- Cột `images` cho phép null và khi đăng ký gán ảnh mặc định `/images/user.png`.
