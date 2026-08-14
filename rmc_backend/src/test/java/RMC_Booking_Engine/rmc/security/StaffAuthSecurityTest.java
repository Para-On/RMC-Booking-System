package RMC_Booking_Engine.rmc.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.task.scheduling.enabled=false")
class StaffAuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedStaffGet_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/staff/bookings")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/staff/arrivals")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedStaffGet_withInvalidBearer_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/staff/bookings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffLogin_withoutToken_isNotUnauthorized() throws Exception {
        mockMvc.perform(post("/api/staff/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void health_isPublic() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }
}
