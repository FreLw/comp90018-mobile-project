package com.comp90018.backend.user

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userRepository: UserRepository) {
	private val passwordEncoder = BCryptPasswordEncoder()

	@PostMapping
	fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
		val account = request.account.trim()
		if (userRepository.existsByAccount(account)) {
			throw ResponseStatusException(HttpStatus.CONFLICT, "Account already exists")
		}

		val user = userRepository.save(
			User(
				account = account,
				passwordHash = requireNotNull(passwordEncoder.encode(request.password)),
				gender = request.gender,
				avatarUrl = request.avatarUrl?.trim()?.ifBlank { null },
				bio = request.bio?.trim()?.ifBlank { null },
				createdAt = Instant.now(),
				updatedAt = Instant.now(),
			),
		)

		return ResponseEntity.status(HttpStatus.CREATED).body(user.toResponse())
	}

	@GetMapping("/{id}")
	fun getUser(@PathVariable id: Long): UserResponse = userRepository.findById(id)
		.orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
		.toResponse()

	@PostMapping("/login")
	fun login(@Valid @RequestBody request: LoginRequest): UserResponse {
		val user = userRepository.findByAccount(request.account.trim())
			.orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid account or password") }

		if (!passwordEncoder.matches(request.password, user.passwordHash)) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid account or password")
		}

		return user.toResponse()
	}
}

data class CreateUserRequest(
	@field:NotBlank(message = "Account is required")
	@field:Size(max = 50, message = "Account must be at most 50 characters")
	val account: String,
	@field:NotBlank(message = "Password is required")
	@field:Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
	val password: String,
	val gender: Gender = Gender.UNSPECIFIED,
	@field:Size(max = 2048, message = "Avatar URL must be at most 2048 characters")
	val avatarUrl: String? = null,
	@field:Size(max = 500, message = "Bio must be at most 500 characters")
	val bio: String? = null,
)

data class UserResponse(
	val id: Long,
	val account: String,
	val gender: Gender,
	val avatarUrl: String?,
	val bio: String?,
	val createdAt: Instant,
	val updatedAt: Instant,
)

data class LoginRequest(
	@field:NotBlank(message = "Account is required")
	val account: String,
	@field:NotBlank(message = "Password is required")
	val password: String,
)

private fun User.toResponse() = UserResponse(
	id = requireNotNull(id),
	account = account,
	gender = gender,
	avatarUrl = avatarUrl,
	bio = bio,
	createdAt = requireNotNull(createdAt),
	updatedAt = requireNotNull(updatedAt),
)
