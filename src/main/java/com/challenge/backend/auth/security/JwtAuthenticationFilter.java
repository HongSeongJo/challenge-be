package com.challenge.backend.auth.security;

import com.challenge.backend.auth.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authorization: Bearer 헤더의 access 토큰만 인증으로 인정한다. 토큰이 없거나 유효하지 않으면 그대로 통과시켜 SecurityConfig의 authorizeHttpRequests가 401을 내리게 한다. */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            try {
                Claims claims = jwtTokenProvider.parseClaims(token);
                if (jwtTokenProvider.isAccessToken(claims)) {
                    AuthenticatedUser principal = new AuthenticatedUser(
                            jwtTokenProvider.getUserId(claims),
                            jwtTokenProvider.getEmail(claims),
                            jwtTokenProvider.getRole(claims));
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
                    var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // 토큰이 유효하지 않으면 인증되지 않은 상태로 다음 필터로 넘어간다.
            }
        }

        filterChain.doFilter(request, response);
    }
}
