package com.clouddisk.filter;

import com.clouddisk.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.header}")
    private String tokenHeader;

    @Value("${jwt.token-prefix}")
    private String tokenPrefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(tokenHeader);

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(tokenPrefix + " ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(tokenPrefix.length() + 1);

        try {
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String email = jwtUtil.getEmailFromToken(token);
                Integer userType = jwtUtil.getUserTypeFromToken(token);

                if (userId != null && email != null) {
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    switch (userType) {
                        case 0:
                            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                            break;
                        case 1:
                            authorities.add(new SimpleGrantedAuthority("ROLE_VIP"));
                            break;
                        case 2:
                            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                            break;
                        default:
                            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("JWT认证成功：userId={}, email={}, userType={}", userId, email, userType);
                }
            } else {
                log.warn("无效的JWT Token");
            }
        } catch (Exception e) {
            log.error("JWT认证失败", e);
        }

        filterChain.doFilter(request, response);
    }
}
