package com.example.jwtserver.presentation

import com.example.jwtserver.application.service.SearchService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

data class SearchPostsResponse(val postIds: List<Long>, val posts: List<PostResponse>)

@RestController
@RequestMapping("/api/search")
class SearchController(private val searchService: SearchService) {

    @GetMapping("/posts")
    fun searchPosts(
        @RequestParam q: String,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): List<PostResponse> = searchService.searchPosts(q, offset, limit).map { PostResponse.from(it) }

    @GetMapping("/users")
    fun searchUsers(
        @RequestParam q: String,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): List<String> = searchService.searchUsers(q, offset, limit)

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    fun reindex(): Map<String, Int> = mapOf("indexed" to searchService.reindexAll())
}
