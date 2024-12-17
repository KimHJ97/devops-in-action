# CORS 문제

CORS(Cross-Origin Resource Sharing)는 웹 브라우저에서 보안 상의 이유로 도메인 간 리소스 공유를 제한하는 메커니즘입니다. 즉, 클라이언트(브라우저)에서 다른 도메인, 프로토콜, 또는 포트에서 제공하는 리소스에 접근하려 할 때 발생할 수 있는 문제를 관리하는 보안 기능입니다.  

 - __CORS 문제__
    - 만약 클라이언트가 다른 출처에서 데이터를 요청하려고 할 때, 서버에서 CORS 정책이 제대로 설정되어 있지 않으면 브라우저는 요청을 차단하고 CORS 오류를 발생시킵니다. 이 오류는 브라우저 콘솔에 보통 "No 'Access-Control-Allow-Origin' header is present on the requested resource"와 같은 메시지로 나타납니다.
 - __CORS 해결 방법__
    - CORS 문제를 해결하려면, 서버 측에서 클라이언트의 도메인을 허용하도록 설정해야 합니다. 서버는 요청을 받은 후 적절한 CORS 헤더를 응답에 포함시켜야 합니다.
    - Access-Control-Allow-Origin: 클라이언트가 요청할 수 있는 출처를 명시합니다. 모든 출처를 허용하려면 *를 사용할 수 있지만, 보안 상 위험할 수 있습니다.
    - Access-Control-Allow-Methods: 클라이언트가 사용할 수 있는 HTTP 메서드를 명시합니다.
    - Access-Control-Allow-Headers: 클라이언트가 요청할 때 사용할 수 있는 헤더를 명시합니다.
    - Access-Control-Allow-Credentials: 클라이언트에서 쿠키와 같은 자격 증명을 포함한 요청을 허용할지 여부를 명시합니다.
```
# 도메인
Access-Control-Allow-Origin: *
Access-Control-Allow-Origin: https://example.com

# HTTP 메서드
Access-Control-Allow-Methods: GET, POST, PUT, DELETE

# HTTP 헤더
Access-Control-Allow-Headers: Content-Type, Authorization

# 쿠키, 자격 증명
Access-Control-Allow-Credentials: true
```

## Spring 애플리케이션 CORS 설정

CorsConfigurationSource는 Spring Security에서 CORS(Cross-Origin Resource Sharing) 설정을 구성하기 위한 인터페이스입니다. CORS는 브라우저에서 다른 도메인으로부터 자원을 요청할 때 발생하는 보안 메커니즘으로, 클라이언트가 다른 출처의 리소스에 접근할 수 있는 권한을 서버가 제어할 수 있도록 합니다.  

 - Spring Security 설정
    - CorsConfigurationSource
```java
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.cors(httpSecurityCorsConfigurer ->
				httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource())
			)
			.authorizeHttpRequests(authorizeRequests ->
				authorizeRequests
					// ..
					.requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
					.anyRequest().authenticated()
			)
            // ..
			.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowCredentials(true); // 쿠키나 인증 정보 허용
		configuration.setAllowedOrigins(
			List.of("http://localhost:3000", "https://domain.com", "https://www.domain.com")
        );
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
		configuration.setExposedHeaders(List.of("*")); 

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
```

 - Swagger 설정
    - Swagger 사용시 servers() 메서드로 도메인을 추가해준다.
```java
@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openApi() {
		return new OpenAPI()
			.info(new Info()
				.title("API 제목")
				.description("API 설명")
				.version("v1.0.0"))
			.servers(
				List.of(
					new Server().url("https://domain.com"),
					new Server().url("https://www.domain.com")
				)
			)
			.addSecurityItem(new SecurityRequirement().addList("token"))
			.components(new Components()
				.addSecuritySchemes(
					"token",
					new SecurityScheme().type(SecurityScheme.Type.HTTP)
						.scheme("Bearer")
						.bearerFormat("JWT")
				)
			);
	}
}
```
