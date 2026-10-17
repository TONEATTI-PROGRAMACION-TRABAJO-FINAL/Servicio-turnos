package com.toneatti.turnos.user.infrastructure.web.controller;

import com.toneatti.turnos.user.application.exception.EmailAlreadyRegisteredException;
import com.toneatti.turnos.user.application.service.UserService;
import com.toneatti.turnos.user.domain.model.User;
import com.toneatti.turnos.user.infrastructure.web.dto.UserRequest;
import com.toneatti.turnos.user.infrastructure.web.dto.UserResponse;
import com.toneatti.turnos.user.infrastructure.web.mapper.UserDtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserDtoMapper userDtoMapper;

    @PostMapping
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRequest userRequest) {
        User user = userDtoMapper.toDomain(userRequest);
        try {
            User registeredUser = userService.registerUser(user, userRequest.getPassword());
            return new ResponseEntity<>(userDtoMapper.toResponse(registeredUser), HttpStatus.CREATED);
        } catch (EmailAlreadyRegisteredException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
    }
}
