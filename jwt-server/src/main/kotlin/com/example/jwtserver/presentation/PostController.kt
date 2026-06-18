package com.example.jwtserver.presentation

import com.example.jwtserver.application.service.PostService
import com.example.jwtserver.domain.post.Post
import com.example.jwtserver.infrastructure.security.AuthenticatedUser
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

data class CreatePostRequest(val title: String, val content: String, val imageUrl: String? = null, val hashtags: List<String> = emptyList())
data class UpdatePostRequest(val title: String, val content: String, val imageUrl: String? = null, val hashtags: List<String> = emptyList())
data class PostResponse(
    val id: Long, val authorId: Long, val title: String, val content: String,
    val imageUrl: String?, val likeCount: Int, val commentCount: Int, val status: String,
    val createdAt: String
) {
    companion object {
        fun from(post: Post) = PostResponse(
            id = post.id!!, authorId = post.authorId, title = post.title,
            content = post.content, imageUrl = post.imageUrl, likeCount = post.likeCount,
            commentCount = post.commentCount, status = post.status.name,
            createdAt = post.createdAt.toString()
        )
    }
}

@RestController
@RequestMapping("/api/posts")
class PostController(private val postService: PostService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun create(@AuthenticationPrincipal user: AuthenticatedUser, @RequestBody req: CreatePostRequest): PostResponse =
        PostResponse.from(postService.create(user.id, user.username, req.title, req.content, req.imageUrl, req.hashtags))

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): PostResponse =
        PostResponse.from(postService.getById(id))

    @GetMapping("/user/{username}")
    fun getByAuthor(
        @PathVariable username: String,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): List<PostResponse> = postService.getByAuthor(username, offset, limit).map { PostResponse.from(it) }

    @GetMapping("/hashtag/{name}")
    fun getByHashtag(
        @PathVariable name: String,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): List<PostResponse> = postService.getByHashtag(name, offset, limit).map { PostResponse.from(it) }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun update(@AuthenticationPrincipal user: AuthenticatedUser, @PathVariable id: Long, @RequestBody req: UpdatePostRequest): PostResponse =
        PostResponse.from(postService.update(user.id, user.username, id, req.title, req.content, req.imageUrl, req.hashtags))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun delete(@AuthenticationPrincipal user: AuthenticatedUser, @PathVariable id: Long) {
        postService.delete(user.id, user.username, id, user.roles.contains("ROLE_ADMIN"))
    }
}
