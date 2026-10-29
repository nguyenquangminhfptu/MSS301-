package fu.de190270.post.controller;

import fu.de190270.post.common.ValidationGroups;
import fu.de190270.post.dto.*;
import fu.de190270.post.entity.PostStatus;
import fu.de190270.post.service.PostService;
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
@RequestMapping(value = "/api/posts", produces = MediaType.APPLICATION_JSON_VALUE)
public class DE190270PostController {
    private final PostService service;

    public DE190270PostController(PostService service) {
        this.service = service;
    }

    @Operation(summary = "Book a campaign with an ACTIVE user")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PostDTO>> create(
            @Validated(ValidationGroups.Create.class) @RequestBody PostDTO dto) {
        return ResponseEntity.status(201).body(ApiResponse.success("Post campaign created successfully", service.createPost(dto)));
    }

    @Operation(summary = "Partially update a campaign and recalculate totalCost")
    @PutMapping(value = "/{postId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<PostDTO> update(@PathVariable Long postId, @Valid @RequestBody PostDTO dto) {
        return ApiResponse.success("Post campaign updated successfully", service.updatePost(postId, dto));
    }

    @Operation(summary = "Get a campaign including the user fetched through OpenFeign")
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailDTO> detail(@PathVariable Long postId) {
        return ApiResponse.success("Successful", service.getPostById(postId));
    }

    @Operation(summary = "Cancel a campaign")
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> delete(@PathVariable Long postId) {
        service.cancelPost(postId);
        return ApiResponse.success("Post campaign is cancelled successfully", null);
    }

    @Operation(summary = "List campaigns with pagination and advertiser/status filters")
    @GetMapping
    public ApiResponse<PageDTO<PostDetailDTO>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String advertiserName,
            @RequestParam(required = false) PostStatus status) {
        return ApiResponse.success("Successful", service.getPostList(advertiserName, status,
                PageRequest.of(page, size, Sort.by("postId"))));
    }
}
