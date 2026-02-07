package no.disckos.backend.api.dto.admin

data class LoginRequest(
    val username: String,
    val password: String,
)

data class RefreshRequest(
    val refreshToken: String,
)

data class LogoutRequest(
    val refreshToken: String,
)

data class AdminUserDto(
    val id: String,
    val name: String,
    val roles: List<String>,
)

data class LoginResponse(
    val user: AdminUserDto,
    val token: String,
    val refreshToken: String,
    val expiresIn: Long,
)

data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)

data class ErrorResponse(
    val error: String,
)
