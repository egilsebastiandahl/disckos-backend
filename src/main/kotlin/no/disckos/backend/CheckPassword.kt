package no.disckos.backend

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

fun main() {
    val encoder = BCryptPasswordEncoder()
    val password = ""
    println(encoder.encode("PutAPasswordHere") + " this is the raw password, encoded.")
    println(encoder.matches(password, "PasteEncodedPasswordHere"))
}