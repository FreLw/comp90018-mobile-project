package com.comp90018.backend.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * A small application endpoint for checking whether the API is available.
 *
 * The Android app can call this at GET /api/v1/health while its real features
 * are being added.
 */
@RestController
@RequestMapping("/api/v1")
class HealthController {
	@GetMapping("/health")
	fun health(): ResponseEntity<HealthResponse> = ResponseEntity.ok(
		HealthResponse(
			status = "ok",
			service = "comp90018-backend",
			timestamp = Instant.now().toString(),
		),
	)
}

data class HealthResponse(
	val status: String,
	val service: String,
	val timestamp: String,
)
