package com.comp90018.backend.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "users")
class User(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,

	@Column(nullable = false, unique = true, length = 50)
	var account: String = "",

	@Column(name = "password_hash", nullable = false, length = 255)
	var passwordHash: String = "",

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	var gender: Gender = Gender.UNSPECIFIED,

	@Column(name = "avatar_url", length = 2048)
	var avatarUrl: String? = null,

	@Column(length = 500)
	var bio: String? = null,

	@Column(name = "created_at", nullable = false, updatable = false)
	var createdAt: Instant? = null,

	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant? = null,
)
