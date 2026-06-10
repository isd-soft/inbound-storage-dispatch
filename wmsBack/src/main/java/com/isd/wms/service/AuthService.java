package com.isd.wms.service;


import com.isd.wms.exception.InvalidCredentialsException;
import com.isd.wms.exception.UserNotVerifiedException;
import com.isd.wms.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public String authenticateAndGenerateToken(String username, String password) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (DisabledException e) {
            throw new UserNotVerifiedException("Please verify your email before logging in.");
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Incorrect username or password.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return jwtUtil.generateToken(
                userDetails.getUsername(),
                userDetails.getAuthorities().iterator().next().getAuthority()
        );
    }
}