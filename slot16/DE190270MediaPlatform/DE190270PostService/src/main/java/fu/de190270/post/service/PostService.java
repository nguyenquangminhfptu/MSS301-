package fu.de190270.post.service;

import fu.de190270.post.dto.PageDTO;
import fu.de190270.post.dto.PostDTO;
import fu.de190270.post.dto.PostDetailDTO;
import fu.de190270.post.entity.PostStatus;
import org.springframework.data.domain.Pageable;

public interface PostService {
    PostDTO createPost(PostDTO dto);
    PostDTO updatePost(Long postId, PostDTO dto);
    PostDetailDTO getPostById(Long postId);
    void cancelPost(Long postId);
    PageDTO<PostDetailDTO> getPostList(String advertiserName, PostStatus status, Pageable pageable);
}
