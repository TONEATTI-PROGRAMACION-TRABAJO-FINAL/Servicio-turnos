package com.toneatti.turnos.user.infrastructure.web.controller;

import com.toneatti.turnos.user.application.exception.EmailAlreadyRegisteredException;
import com.toneatti.turnos.user.application.exception.UserException;
import com.toneatti.turnos.user.application.service.UserService;
import com.toneatti.turnos.user.domain.model.AccessToken;
import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.infrastructure.web.dto.LoginRequest;
import com.toneatti.turnos.user.infrastructure.web.dto.TokenResponse;
import com.toneatti.turnos.user.infrastructure.web.dto.UserRequest;
import com.toneatti.turnos.user.infrastructure.web.dto.UserResponse;
import com.toneatti.turnos.user.infrastructure.web.mapper.UserDtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserDtoMapper userDtoMapper;

    @PostMapping
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRequest userRequest) {
        // @RequestBody is required: Spring answers 400 before reaching here if the body is missing.
        Objects.requireNonNull(userRequest, "userRequest");
        User user = userDtoMapper.toDomain(userRequest);
        try {
            User registeredUser = userService.registerUser(user, userRequest.getPassword());
            return new ResponseEntity<>(userDtoMapper.toResponse(registeredUser), HttpStatus.CREATED);
        } catch (EmailAlreadyRegisteredException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            AccessToken accessToken = userService.loginUser(loginRequest.getEmail(), loginRequest.getPassword());
            // The token is a credential: no cache may keep it.
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(userDtoMapper.toTokenResponse(accessToken));
        } catch (UserException e) {
            // Same message for an unknown email and a wrong password, so it does not reveal which emails exist.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }
}
