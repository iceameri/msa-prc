package com.example.authorizationserver.infrastructure.tenant

import com.example.authorizationserver.domain.tenant.TenantRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TenantResolutionFilter(
    private val tenantRepository: TenantRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val slug = request.getParameter("tenant")
        try {
            if (!slug.isNullOrBlank()) {
                val tenant = tenantRepository.findBySlug(slug)
                TenantContext.set(tenant?.id)
            }
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}
