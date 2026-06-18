package com.example.jwtserver.domain.post

interface PostRepository {
    fun findById(id: Long): Post?
    fun findAll(offset: Int, limit: Int): List<Post>
    fun findByAuthorId(authorId: Long, offset: Int, limit: Int): List<Post>
    fun findFeedPosts(followingIds: List<Long>, offset: Int, limit: Int): List<Post>
    fun findByHashtag(hashtagName: String, offset: Int, limit: Int): List<Post>
    fun save(post: Post): Post
    fun update(post: Post)
    fun delete(id: Long)
    fun incrementLikeCount(id: Long)
    fun decrementLikeCount(id: Long)
    fun incrementCommentCount(id: Long)
    fun decrementCommentCount(id: Long)
}
