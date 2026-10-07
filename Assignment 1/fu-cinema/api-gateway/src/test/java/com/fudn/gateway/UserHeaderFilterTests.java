package com.fudn.gateway;

import com.fudn.gateway.filter.UserHeaderFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.function.ServerRequest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UserHeaderFilterTests {
    @AfterEach void clearContext() {SecurityContextHolder.clearContext();}
    @Test void overwritesEveryClientUserHeaderFromJwt() {
        var request=new MockHttpServletRequest();
        request.addHeader("X-User-Id","2");request.addHeader("x-user-role","ADMIN");request.addHeader("X-User-Custom","spoof");
        Jwt jwt=Jwt.withTokenValue("token").header("alg","HS256").subject("an@gmail.com").claim("uid",1L).claim("role","CUSTOMER").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        var forwarded=UserHeaderFilter.forwardUserInfo().apply(ServerRequest.create(request,List.of()));
        assertEquals(List.of("1"),forwarded.headers().header("X-User-Id"));
        assertEquals(List.of("CUSTOMER"),forwarded.headers().header("X-User-Role"));
        assertEquals(List.of("an@gmail.com"),forwarded.headers().header("X-User-Email"));
        assertTrue(forwarded.headers().header("X-User-Custom").isEmpty());
    }
    @Test void publicRequestCannotForwardSpoofedUserContext() {
        var request=new MockHttpServletRequest();request.addHeader("X-User-Id","2");
        var forwarded=UserHeaderFilter.forwardUserInfo().apply(ServerRequest.create(request,List.of()));
        assertTrue(forwarded.headers().header("X-User-Id").isEmpty());
    }
}
