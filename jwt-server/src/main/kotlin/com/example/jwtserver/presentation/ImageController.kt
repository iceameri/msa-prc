package com.example.jwtserver.presentation

import com.example.jwtserver.application.service.ImageService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

data class ImageUploadResponse(val url: String)

@RestController
@RequestMapping("/api/images")
class ImageController(private val imageService: ImageService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    fun upload(@RequestParam("file") file: MultipartFile): ImageUploadResponse =
        ImageUploadResponse(url = imageService.upload(file))
}
