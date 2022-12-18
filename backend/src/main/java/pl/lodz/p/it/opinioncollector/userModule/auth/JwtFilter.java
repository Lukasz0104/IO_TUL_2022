package pl.lodz.p.it.opinioncollector.userModule.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import pl.lodz.p.it.opinioncollector.userModule.user.UserManager;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserManager userDetailsService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = jwtProvider.getToken(request);

        if (jwt == null || !jwtProvider.validateToken(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }
        Claims claims = jwtProvider.parseJWT(jwt).getBody();
        UserDetails userDetails;

        try {
            userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
        } catch (UsernameNotFoundException enfe) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!userDetails.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.LOCKED);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails.getUsername(),
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
