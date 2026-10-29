package fu.de190270.post.service.impl;

import feign.FeignException;
import fu.de190270.post.client.UserClient;
import fu.de190270.post.common.ApiException;
import fu.de190270.post.dto.*;
import fu.de190270.post.entity.Post;
import fu.de190270.post.entity.PostStatus;
import fu.de190270.post.repository.PostRepository;
import fu.de190270.post.service.PostService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {
    private final PostRepository repository;
    private final UserClient userClient;

    public PostServiceImpl(PostRepository repository, UserClient userClient) {
        this.repository = repository;
        this.userClient = userClient;
    }

    @Override
    @Transactional
    public PostDTO createPost(PostDTO dto) {
        validateDates(dto.startDate(), dto.endDate());
        UserDTO user = fetchUser(dto.userId());
        requireActive(user);
        validateNumber(dto.numberOfPosts(), user);
        BigDecimal cost = computeTotal(user.ratePerPost(), dto.startDate(), dto.endDate());
        Post post = new Post();
        post.setAdvertiserName(dto.advertiserName());
        post.setAdvertiserEmail(dto.advertiserEmail());
        post.setAdvertiserPhone(dto.advertiserPhone());
        post.setStartDate(dto.startDate());
        post.setEndDate(dto.endDate());
        post.setNumberOfPosts(dto.numberOfPosts());
        post.setUserId(dto.userId());
        post.setTotalCost(cost);
        post.setStatus(dto.status() == null ? PostStatus.SCHEDULED : PostStatus.valueOf(dto.status()));
        return toDTO(repository.saveAndFlush(post));
    }

    @Override
    @Transactional
    public PostDTO updatePost(Long postId, PostDTO dto) {
        Post post = find(postId);
        Date start = dto.startDate() == null ? post.getStartDate() : dto.startDate();
        Date end = dto.endDate() == null ? post.getEndDate() : dto.endDate();
        int number = dto.numberOfPosts() == null ? post.getNumberOfPosts() : dto.numberOfPosts();
        Long userId = dto.userId() == null ? post.getUserId() : dto.userId();
        validateDates(start, end);
        UserDTO user = fetchUser(userId);
        if (!Objects.equals(userId, post.getUserId())) requireActive(user);
        validateNumber(number, user);
        BigDecimal cost = computeTotal(user.ratePerPost(), start, end);

        // All business checks precede changes to the managed entity.
        if (dto.advertiserName() != null) post.setAdvertiserName(dto.advertiserName());
        if (dto.advertiserEmail() != null) post.setAdvertiserEmail(dto.advertiserEmail());
        if (dto.advertiserPhone() != null) post.setAdvertiserPhone(dto.advertiserPhone());
        if (dto.status() != null) post.setStatus(PostStatus.valueOf(dto.status()));
        post.setStartDate(start);
        post.setEndDate(end);
        post.setNumberOfPosts(number);
        post.setUserId(userId);
        post.setTotalCost(cost);
        return toDTO(repository.saveAndFlush(post));
    }

    @Override
    public PostDetailDTO getPostById(Long postId) {
        Post post = find(postId);
        return toDetail(post, fetchUser(post.getUserId()));
    }

    @Override
    @Transactional
    public void cancelPost(Long postId) {
        Post post = find(postId);
        post.setStatus(PostStatus.CANCELLED);
        repository.saveAndFlush(post);
    }

    @Override
    public PageDTO<PostDetailDTO> getPostList(String advertiserName, PostStatus status, Pageable pageable) {
        String filter = advertiserName == null || advertiserName.isBlank() ? null : advertiserName;
        Map<Long, UserDTO> users = new HashMap<>();
        return PageDTO.from(repository.search(filter, status, pageable)
                .map(post -> toDetail(post, users.computeIfAbsent(post.getUserId(), this::fetchUser))));
    }

    private Post find(Long postId) {
        return repository.findById(postId).orElseThrow(() -> ApiException.notFound("Post is not found"));
    }

    private UserDTO fetchUser(Long userId) {
        try {
            ApiResponse<UserDTO> response = userClient.getUserById(userId);
            if (response != null && response.status() == 4) throw ApiException.notFound("User ID is not found");
            if (response == null || response.status() != 1 || response.data() == null) {
                throw new IllegalStateException("Invalid response from User Service");
            }
            return response.data();
        } catch (FeignException.NotFound ex) {
            throw ApiException.notFound("User ID is not found");
        }
        // Timeouts and other upstream failures reach GlobalExceptionHandler as 500/status 0.
    }

    private void requireActive(UserDTO user) {
        if (!"ACTIVE".equals(user.status())) throw new ApiException(400, 5, "User is not ACTIVE for posting");
    }

    private void validateNumber(int number, UserDTO user) {
        if (number < 1 || number > user.maxDailyPosts()) {
            throw ApiException.invalid("numberOfPosts must be between 1 and user's maxDailyPosts");
        }
    }

    private void validateDates(Date start, Date end) {
        if (start == null || end == null || !toLocalDate(end).isAfter(toLocalDate(start))) {
            throw ApiException.invalid("endDate must be after startDate");
        }
    }

    private BigDecimal computeTotal(BigDecimal rate, Date start, Date end) {
        long days = ChronoUnit.DAYS.between(toLocalDate(start), toLocalDate(end));
        // The exam formula deliberately does not multiply by numberOfPosts.
        BigDecimal total = rate.multiply(BigDecimal.valueOf(days));
        if (total.precision() - total.scale() > 16) {
            throw ApiException.invalid("totalCost exceeds DECIMAL(18,2)");
        }
        return total;
    }

    private LocalDate toLocalDate(Date date) {
        // java.sql.Date returned by JPA does not support Date.toInstant().
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private PostDTO toDTO(Post post) {
        return new PostDTO(post.getPostId(), post.getAdvertiserName(), post.getAdvertiserEmail(),
                post.getAdvertiserPhone(), post.getStartDate(), post.getEndDate(), post.getNumberOfPosts(),
                post.getTotalCost(), post.getStatus().name(), post.getUserId());
    }

    private PostDetailDTO toDetail(Post post, UserDTO user) {
        return new PostDetailDTO(post.getPostId(), post.getAdvertiserName(), post.getAdvertiserEmail(),
                post.getAdvertiserPhone(), post.getStartDate(), post.getEndDate(), post.getNumberOfPosts(),
                post.getTotalCost(), post.getStatus().name(), user);
    }
}
