package com.example.jwtserver.infrastructure.elasticsearch

import co.elastic.clients.elasticsearch._types.query_dsl.Query
import com.example.jwtserver.application.port.out.SearchPort
import com.example.jwtserver.domain.post.Post
import com.example.jwtserver.domain.user.UserRepository
import org.springframework.data.elasticsearch.NoSuchIndexException
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates.of
import org.springframework.data.elasticsearch.core.query.IndexQuery
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder
import org.springframework.data.elasticsearch.core.search
import org.springframework.stereotype.Component

@Component
class ElasticsearchSearchAdapter(
    private val elasticsearchOperations: ElasticsearchOperations,
    private val userRepository: UserRepository
) : SearchPort {

    private val postIndex = of("posts")
    private val userIndex = of("users")

    override fun indexPost(post: Post, hashtags: List<String>) {
        val authorUsername = userRepository.findById(post.authorId)?.username ?: return
        val doc = PostDocument(
            id = post.id.toString(),
            authorUsername = authorUsername,
            title = post.title,
            content = post.content,
            hashtags = hashtags,
            createdAt = post.createdAt.toString()
        )
        val query: IndexQuery = IndexQueryBuilder()
            .withId(post.id.toString())
            .withObject(doc)
            .build()
        elasticsearchOperations.index(query, postIndex)

        val userDoc = UserDocument(username = authorUsername)
        val userQuery: IndexQuery = IndexQueryBuilder()
            .withId(authorUsername)
            .withObject(userDoc)
            .build()
        elasticsearchOperations.index(userQuery, userIndex)
    }

    override fun deletePost(postId: Long) {
        elasticsearchOperations.delete(postId.toString(), postIndex)
    }

    override fun searchPosts(keyword: String, offset: Int, limit: Int): List<Long> {
        val query = NativeQuery.builder()
            .withQuery(Query.of { q ->
                q.multiMatch { m -> m.query(keyword).fields("title", "content", "hashtags") }
            })
            .withPageable(org.springframework.data.domain.PageRequest.of(offset / limit, limit))
            .build()
        return try {
            elasticsearchOperations.search<PostDocument>(query, postIndex)
                .searchHits
                .mapNotNull { it.id?.toLongOrNull() }
        } catch (e: Exception) {
            if (isIndexNotFound(e)) emptyList() else throw e
        }
    }

    override fun searchUsers(keyword: String, offset: Int, limit: Int): List<String> {
        val query = NativeQuery.builder()
            .withQuery(Query.of { q ->
                q.wildcard { w -> w.field("username").value("*${keyword.lowercase()}*") }
            })
            .withPageable(org.springframework.data.domain.PageRequest.of(offset / limit, limit))
            .build()
        return try {
            elasticsearchOperations.search<UserDocument>(query, userIndex)
                .searchHits
                .map { it.content.username }
        } catch (e: Exception) {
            if (isIndexNotFound(e)) emptyList() else throw e
        }
    }

    private fun isIndexNotFound(e: Exception): Boolean {
        var cause: Throwable? = e
        while (cause != null) {
            if (cause is NoSuchIndexException) return true
            if (cause.message?.contains("index_not_found") == true) return true
            cause = cause.cause
        }
        return false
    }
}
