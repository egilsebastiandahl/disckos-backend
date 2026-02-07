package no.disckos.backend

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    properties = [
        "ADMIN_USERNAME=admin",
        "ADMIN_PASSWORD_HASH=\$2b\$10\$PuyEeB9aUpTSSyqO8NuseeIGaKt8HFAasrQfnNaxWy5aAXq67Jwd.",
        "JWT_SECRET=dev-secret-dev-secret-dev-secret-dev-secret",
        "REFRESH_JWT_SECRET=dev-refresh-dev-refresh-dev-refresh-dev",
        "ACCESS_TOKEN_EXPIRY_SECONDS=900",
        "REFRESH_TOKEN_EXPIRY_SECONDS=604800",
    ],
)
@ActiveProfiles("test")
class DisckosBackendApplicationTests {

    @Test
    fun contextLoads() {
    }

}
