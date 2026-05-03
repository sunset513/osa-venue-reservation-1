package tw.edu.ncu.osa.venue_reservation_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider
    ) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/test/**").authenticated()
                        .anyRequest().permitAll()
                );

        ClientRegistrationRepository clientRegistrationRepository = clientRegistrationRepositoryProvider.getIfAvailable();
        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> oauth2
                        // [關鍵修正 1] 強制自定義請求解析器以關閉 PKCE
                        .authorizationEndpoint(auth -> auth
                                .authorizationRequestResolver(authorizationRequestResolver(clientRegistrationRepository))
                        )
                        // [關鍵修正 2] 處理 Token 交換階段的 Header
                        .tokenEndpoint(token -> token.accessTokenResponseClient(accessTokenResponseClient()))
                );
        } else {
            log.warn("OAuth2 client registration is not configured; OAuth2 login endpoints are disabled.");
        }

        return http.build();
    }

    // 關閉 PKCE 的處理器
    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(ClientRegistrationRepository repository) {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(repository, "/oauth2/authorization");

        // 直接使用標準 OAuth2 參數名稱字串，避開版本相容性導致的常數解析問題
        resolver.setAuthorizationRequestCustomizer(requestBuilder -> {
            requestBuilder.attributes(attrs -> {
                attrs.remove("code_challenge");
                attrs.remove("code_challenge_method");
                attrs.remove("code_verifier");
            });
            requestBuilder.additionalParameters(params -> {
                params.remove("code_challenge");
                params.remove("code_challenge_method");
            });
        });
        return resolver;
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        RestClientAuthorizationCodeTokenResponseClient tokenResponseClient = new RestClientAuthorizationCodeTokenResponseClient();

        // 1. 建立 OAuth2 專用的訊息轉換器
        OAuth2AccessTokenResponseHttpMessageConverter tokenResponseConverter = new OAuth2AccessTokenResponseHttpMessageConverter();
        // 它允許 Body 被多次讀取
        BufferingClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        // 2. 將此轉換器注入 RestClient
        RestClient restClient = RestClient.builder()
                .requestFactory(factory) // 注入緩衝工廠
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // 使用最新的 Lambda 方式配置轉換器，解決棄用與解析問題
                .messageConverters(converters -> {
                    converters.add(0, tokenResponseConverter);
                })
                // 加入安全的日誌攔截器
                .requestInterceptor((request, body, execution) -> {
                    // 紀錄 Request 日誌
                    log.info("=== Token Request Start ===");
                    log.info("URL: {} {}", request.getMethod(), request.getURI());
                    log.info("Body: {}", new String(body, StandardCharsets.UTF_8));

                    var response = execution.execute(request, body);

                    // 紀錄 Response 日誌
                    // 因為使用了 BufferingClientHttpRequestFactory，這裡讀取 body 後，
                    // 後續 Spring 的 Converter 依然能再次讀取，不會報 Empty Response
                    byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());
                    log.info("Response Status: {}", response.getStatusCode());
                    log.info("Response Body: {}", new String(responseBody, StandardCharsets.UTF_8));
                    log.info("=== Token Request End ===");

                    return response;
                })
                .build();


        // [核心修正] 中大 Portal 有時要求 Body 也要有 client_id
        tokenResponseClient.setParametersConverter(grantRequest -> {
            MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
            log.info("Preparing token request parameters for grant type: {}", grantRequest.getGrantType().getValue());
            parameters.add("grant_type", grantRequest.getGrantType().getValue());
            log.info("Adding authorization code to token request parameters: {}", grantRequest.getAuthorizationExchange().getAuthorizationResponse().getCode());
            parameters.add("code", grantRequest.getAuthorizationExchange().getAuthorizationResponse().getCode());
            log.info("Adding redirect URI to token request parameters: {}", grantRequest.getAuthorizationExchange().getAuthorizationRequest().getRedirectUri());
            parameters.add("redirect_uri", grantRequest.getAuthorizationExchange().getAuthorizationRequest().getRedirectUri());
            // 補上 client_id
            log.info("Adding client_id to token request parameters: {}", grantRequest.getClientRegistration().getClientId());
            parameters.add("client_id", grantRequest.getClientRegistration().getClientId());
            log.info("Adding client_secret to token request parameters: {}", grantRequest.getClientRegistration().getClientSecret());
            parameters.add("client_secret", grantRequest.getClientRegistration().getClientSecret());
            return parameters;
        });

        tokenResponseClient.setRestClient(restClient);
        return tokenResponseClient;
    }
}
