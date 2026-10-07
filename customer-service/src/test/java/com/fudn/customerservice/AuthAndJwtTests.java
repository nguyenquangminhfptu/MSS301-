package com.fudn.customerservice;

import com.fudn.customerservice.dto.LoginRequest;
import com.fudn.customerservice.exception.ApiException;
import com.fudn.customerservice.model.*;
import com.fudn.customerservice.repository.CustomerRepository;
import com.fudn.customerservice.security.JwtService;
import com.fudn.customerservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthAndJwtTests {
    static final String SECRET = "fu-cinema-booking-system-secret-key-2026-mss301";
    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final JwtService jwt = new JwtService(SECRET, 60);
    private AuthService auth() {
        AuthService service = new AuthService(repository, passwords, jwt);
        ReflectionTestUtils.setField(service, "adminEmail", "admin@fucinema.com");
        ReflectionTestUtils.setField(service, "adminPassword", "@@abc123@@");
        return service;
    }
    @Test void issuedTokenHasValidSignatureClaimsAndLifetime() {
        var decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
        var decoded = decoder.decode(jwt.generateToken(1L, "an@gmail.com", "CUSTOMER"));
        assertEquals("an@gmail.com", decoded.getSubject());
        assertEquals(1L, ((Number)decoded.getClaim("uid")).longValue());
        assertEquals("CUSTOMER", decoded.getClaimAsString("role"));
        assertEquals(3600L, decoded.getExpiresAt().getEpochSecond()-decoded.getIssuedAt().getEpochSecond());
    }
    @Test void migrationHashMatchesDocumentedPassword() throws Exception {
        String seed = new String(getClass().getResourceAsStream("/db/migration/V2__seed.sql").readAllBytes(), StandardCharsets.UTF_8);
        var matcher = java.util.regex.Pattern.compile("\\$2[a-z]\\$10\\$[./A-Za-z0-9]{53}").matcher(seed);
        int count=0;
        while(matcher.find()) { assertTrue(passwords.matches("123456",matcher.group())); count++; }
        assertEquals(3,count);
        assertTrue(seed.contains("N'Nguyễn Văn An'"));
    }
    @Test void adminLoginDoesNotQueryCustomerDatabase() {
        var response = auth().login(new LoginRequest("admin@fucinema.com", "@@abc123@@"));
        assertEquals("ADMIN",response.role()); assertEquals(0L,response.userId());
        verifyNoInteractions(repository);
    }
    @Test void invalidAdminPasswordReturns401() {
        var error=assertThrows(ApiException.class,()->auth().login(new LoginRequest("admin@fucinema.com", "wrong")));
        assertEquals(401,error.getStatus().value());
    }
    @Test void inactiveCustomerCannotLogin() {
        Customer customer=new Customer();customer.setCustomerStatus(CustomerStatus.INACTIVE);
        customer.setPassword(passwords.encode("123456"));
        when(repository.findByEmailIgnoreCase("chi@gmail.com")).thenReturn(Optional.of(customer));
        var error=assertThrows(ApiException.class,()->auth().login(new LoginRequest("chi@gmail.com","123456")));
        assertEquals(403,error.getStatus().value());
    }
    @Test void wrongPasswordDoesNotRevealInactiveAccount() {
        Customer customer=new Customer();customer.setCustomerStatus(CustomerStatus.INACTIVE);
        customer.setPassword(passwords.encode("123456"));
        when(repository.findByEmailIgnoreCase("chi@gmail.com")).thenReturn(Optional.of(customer));
        var error=assertThrows(ApiException.class,()->auth().login(new LoginRequest("chi@gmail.com","wrong")));
        assertEquals(401,error.getStatus().value());
    }
}
