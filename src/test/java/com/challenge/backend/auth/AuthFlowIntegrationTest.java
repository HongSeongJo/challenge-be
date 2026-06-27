package com.challenge.backend.auth;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.challenge.backend.auth.dto.EmailVerifyRequest;
import com.challenge.backend.auth.dto.LoginRequest;
import com.challenge.backend.auth.dto.RefreshRequest;
import com.challenge.backend.auth.dto.RegisterRequest;
import com.challenge.backend.auth.email.EmailVerificationService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Test
    void 회원가입_로그인_보호된API_접근_리프레시_전체_흐름이_동작한다() throws Exception {
        String email = "flow-test@example.com";
        RegisterRequest registerRequest = new RegisterRequest(email, "password1234", "플로우테스터");

        // 실제 메일 발송 없이, 코드 발급만 직접 호출해서 가입 게이트를 통과시킨다.
        String code = emailVerificationService.issueCode(email);
        mockMvc.perform(post("/api/auth/email/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmailVerifyRequest(email, code))))
                .andExpect(status().isNoContent());

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", not("")))
                .andReturn();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());

        LoginRequest loginRequest = new LoginRequest(email, "password1234");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "wrong-password"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        var loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.get("accessToken").asText();
        String refreshToken = loginBody.get("refreshToken").asText();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not("")));
    }
}
