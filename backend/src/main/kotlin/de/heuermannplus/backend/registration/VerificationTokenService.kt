package de.heuermannplus.backend.registration

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.springframework.stereotype.Service

@Service
class VerificationTokenService {
    private val secureRandom = SecureRandom()

    fun generateToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
