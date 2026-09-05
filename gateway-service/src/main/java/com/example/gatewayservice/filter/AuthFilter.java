package com.example.gatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.List;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

  @Value("${jwt.secret}")
  private String secret;

  public AuthFilter() {
    super(Config.class);
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      String path = exchange.getRequest().getURI().getPath();

      // 인증 제외 경로 확인
      if (config.getExcludePaths() != null) {
        List<String> excludeList = Arrays.asList(config.getExcludePaths().split(","));
        if (excludeList.stream().anyMatch(p -> path.equals(p.trim()))) {
          return chain.filter(exchange);
        }
      }

      // Authorization 헤더 확인
      String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return onError(exchange, HttpStatus.UNAUTHORIZED);
      }

      // JWT 검증
      String token = authHeader.substring(7);
      try {
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        // userId를 헤더에 추가해서 각 서비스로 전달
        String userId = claims.getSubject();
        exchange = exchange.mutate()
            .request(r -> r.header("X-User-Id", userId))
            .build();

      } catch (Exception e) {
        return onError(exchange, HttpStatus.UNAUTHORIZED);
      }

      return chain.filter(exchange);
    };
  }

  private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
    exchange.getResponse().setStatusCode(status);
    return exchange.getResponse().setComplete();
  }

  public static class Config {
    private String excludePaths;

    public String getExcludePaths() { return excludePaths; }
    public void setExcludePaths(String excludePaths) { this.excludePaths = excludePaths; }
  }
}