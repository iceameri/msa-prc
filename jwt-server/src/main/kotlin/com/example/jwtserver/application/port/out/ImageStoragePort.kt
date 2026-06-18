package com.example.jwtserver.application.port.out

import java.io.InputStream

interface ImageStoragePort {
    fun upload(filename: String, contentType: String, size: Long, stream: InputStream): String
    fun delete(objectName: String)
}
