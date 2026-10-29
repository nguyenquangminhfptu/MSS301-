package fu.de190270.user.service;

import fu.de190270.user.dto.PageDTO;
import fu.de190270.user.dto.UserDTO;
import fu.de190270.user.entity.Role;
import fu.de190270.user.entity.UserStatus;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserDTO createUser(UserDTO dto);
    UserDTO updateUser(Long userId, UserDTO dto);
    UserDTO getUserById(Long userId);
    void deactivateUser(Long userId);
    PageDTO<UserDTO> getUserList(Role role, UserStatus status, Pageable pageable);
}
