package com.vieterp.auth;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PermissionTest {

    @Test
    void permissionValuesAreNotNull() {
        for (Permission p : Permission.values()) {
            assertNotNull(p.name());
        }
    }

    @Test
    void hrmPermissionsExist() {
        assertNotNull(Permission.HRM_EMPLOYEE_READ);
        assertNotNull(Permission.HRM_EMPLOYEE_WRITE);
        assertNotNull(Permission.HRM_EMPLOYEE_DELETE);
    }

    @Test
    void jwtClaimsBuilderWorks() {
        var claims = JwtClaims.builder()
            .sub(UUID.randomUUID())
            .email("test@vieterp.com")
            .name("Test User")
            .roles(Set.of("ADMIN"))
            .realm("vieterp")
            .exp(System.currentTimeMillis() / 1000 + 3600)
            .iat(System.currentTimeMillis() / 1000)
            .build();

        assertNotNull(claims.sub());
        assertEquals("test@vieterp.com", claims.email());
        assertEquals("Test User", claims.name());
        assertEquals(Set.of("ADMIN"), claims.roles());
    }

    @Test
    void jwtClaimsEqualsAndHashCode() {
        UUID sub = UUID.randomUUID();
        long now = System.currentTimeMillis() / 1000;
        var c1 = JwtClaims.builder().sub(sub).email("a@b.com").name("A").roles(Set.of()).realm("r").exp(now).iat(now).build();
        var c2 = JwtClaims.builder().sub(sub).email("a@b.com").name("A").roles(Set.of()).realm("r").exp(now).iat(now).build();
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }
}
