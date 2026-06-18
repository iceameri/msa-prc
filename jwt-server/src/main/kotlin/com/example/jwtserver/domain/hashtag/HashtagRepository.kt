package com.example.jwtserver.domain.hashtag

interface HashtagRepository {
    fun findByName(name: String): Hashtag?
    fun findByPostId(postId: Long): List<Hashtag>
    fun findOrCreate(name: String): Hashtag
    fun linkToPost(postId: Long, hashtagId: Long)
    fun unlinkFromPost(postId: Long)
}
