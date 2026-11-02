package com.cleancity.cleancity;

import com.cleancity.cleancity.security.JwtAuthenticationFilter;
import com.cleancity.cleancity.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    private static final String SECRET =
            "VGhpc0lzQVNlY3JldEtleUZvckNsZWFuQ2l0eVRlc3RpbmcxMjM0NQ==";

    private static final String EMAIL = "piyush@example.com";

    @BeforeEach
    void setUp() {

        jwtService = new JwtService(SECRET, 60_000);

        userDetailsService = mock(UserDetailsService.class);

        filter = new JwtAuthenticationFilter(
                jwtService,
                userDetailsService
        );

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipFilterForLoginEndpoint()
            throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/auth/login");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void shouldSkipFilterForRegisterEndpoint()
            throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/auth/register");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void shouldContinueWhenTokenIsMissing()
            throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/citizen/complaints");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);

        assertEquals(
                HttpServletResponse.SC_OK,
                response.getStatus()
        );
    }

    @Test
    void shouldAuthenticateWhenTokenIsValid()
            throws ServletException, IOException {

        String token = jwtService.generateToken(EMAIL);

        UserDetails userDetails = User.withUsername(EMAIL)
                .password("encoded-password")
                .authorities("ROLE_CITIZEN")
                .build();

        when(userDetailsService.loadUserByUsername(EMAIL))
                .thenReturn(userDetails);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/citizen/complaints");
        request.addHeader("Authorization", "Bearer " + token);

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNotNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        assertEquals(
                EMAIL,
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getName()
        );

        verify(userDetailsService).loadUserByUsername(EMAIL);
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldReturnUnauthorizedForInvalidToken()
            throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/citizen/complaints");

        request.addHeader(
                "Authorization",
                "Bearer invalid.token.value"
        );

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(
                HttpServletResponse.SC_UNAUTHORIZED,
                response.getStatus()
        );

        assertTrue(
                response.getContentAsString()
                        .contains("Invalid or expired JWT token")
        );

        verifyNoInteractions(userDetailsService);
        verify(chain, never()).doFilter(request, response);
    }
}