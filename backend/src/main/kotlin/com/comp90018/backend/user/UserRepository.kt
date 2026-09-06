package com.comp90018.backend.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
	fun existsByAccount(account: String): Boolean
	fun findByAccount(account: String): Optional<User>
}
