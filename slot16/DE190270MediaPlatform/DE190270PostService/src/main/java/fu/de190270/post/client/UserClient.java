package fu.de190270.post.client;

import fu.de190270.post.dto.ApiResponse;
import fu.de190270.post.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserClient {
    @GetMapping("/api/users/{userId}")
    ApiResponse<UserDTO> getUserById(@PathVariable("userId") Long userId);
}
