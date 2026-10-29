package fu.de190270.user.service.impl;

import fu.de190270.user.common.ApiException;
import fu.de190270.user.dto.PageDTO;
import fu.de190270.user.dto.UserDTO;
import fu.de190270.user.entity.Role;
import fu.de190270.user.entity.User;
import fu.de190270.user.entity.UserStatus;
import fu.de190270.user.repository.UserRepository;
import fu.de190270.user.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository repository;

    public UserServiceImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public UserDTO createUser(UserDTO dto) {
        if (repository.existsByUsername(dto.username())) {
            throw new ApiException(226, 3, "Username is duplicated");
        }
        User user = new User();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setRole(Role.valueOf(dto.role()));
        user.setRatePerPost(dto.ratePerPost());
        user.setMaxDailyPosts(dto.maxDailyPosts());
        user.setLevel(dto.level());
        user.setStatus(dto.status() == null ? UserStatus.ACTIVE : UserStatus.valueOf(dto.status()));
        return toDTO(repository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long userId, UserDTO dto) {
        User user = find(userId);
        if (dto.username() != null && repository.existsByUsernameAndUserIdNot(dto.username(), userId)) {
            throw new ApiException(226, 3, "Username is duplicated");
        }
        if (dto.username() != null) user.setUsername(dto.username());
        if (dto.email() != null) user.setEmail(dto.email());
        if (dto.role() != null) user.setRole(Role.valueOf(dto.role()));
        if (dto.ratePerPost() != null) user.setRatePerPost(dto.ratePerPost());
        if (dto.maxDailyPosts() != null) user.setMaxDailyPosts(dto.maxDailyPosts());
        if (dto.level() != null) user.setLevel(dto.level());
        if (dto.status() != null) user.setStatus(UserStatus.valueOf(dto.status()));
        return toDTO(repository.saveAndFlush(user));
    }

    @Override
    public UserDTO getUserById(Long userId) {
        return toDTO(find(userId));
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId) {
        User user = find(userId);
        user.setStatus(UserStatus.DEACTIVATED);
        repository.saveAndFlush(user);
    }

    @Override
    public PageDTO<UserDTO> getUserList(Role role, UserStatus status, Pageable pageable) {
        return PageDTO.from(repository.search(role, status, pageable).map(this::toDTO));
    }

    private User find(Long userId) {
        return repository.findById(userId).orElseThrow(() -> ApiException.notFound("User is not found"));
    }

    private UserDTO toDTO(User user) {
        return new UserDTO(user.getUserId(), user.getUsername(), user.getEmail(), user.getRole().name(),
                user.getRatePerPost(), user.getMaxDailyPosts(), user.getLevel(), user.getStatus().name());
    }
}
