package fu.de190270.user.controller;

import fu.de190270.user.common.ValidationGroups;
import fu.de190270.user.dto.ApiResponse;
import fu.de190270.user.dto.PageDTO;
import fu.de190270.user.dto.UserDTO;
import fu.de190270.user.entity.Role;
import fu.de190270.user.entity.UserStatus;
import fu.de190270.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class DE190270UserController {
    private final UserService service;

    public DE190270UserController(UserService service) {
        this.service = service;
    }

    @Operation(summary = "Create a user; default status ACTIVE")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserDTO>> create(
            @Validated(ValidationGroups.Create.class) @RequestBody UserDTO dto) {
        return ResponseEntity.status(201).body(ApiResponse.success("User created successfully", service.createUser(dto)));
    }

    @Operation(summary = "Partially update a user")
    @PutMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<UserDTO> update(@PathVariable Long userId, @Valid @RequestBody UserDTO dto) {
        return ApiResponse.success("User updated successfully", service.updateUser(userId, dto));
    }

    @Operation(summary = "Get a user by ID")
    @GetMapping("/{userId}")
    public ApiResponse<UserDTO> detail(@PathVariable Long userId) {
        return ApiResponse.success("Successful", service.getUserById(userId));
    }

    @Operation(summary = "Deactivate a user")
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> delete(@PathVariable Long userId) {
        service.deactivateUser(userId);
        return ApiResponse.success("User is set to DEACTIVATED successfully", null);
    }

    @Operation(summary = "List users with pagination and optional role/status filters")
    @GetMapping
    public ApiResponse<PageDTO<UserDTO>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status) {
        return ApiResponse.success("Successful", service.getUserList(role, status,
                PageRequest.of(page, size, Sort.by("userId"))));
    }
}
