package de.heuermannplus.backend.registration

import java.time.Clock
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils

@Service
class RegistrationService(
    private val registrationProperties: RegistrationProperties,
    private val passwordPolicyEvaluator: PasswordPolicyEvaluator,
    private val captchaVerifier: CaptchaVerifier,
    private val keycloakAdminClient: KeycloakAdminClient,
    private val appUserStore: AppUserStore,
    private val termsVersionStore: TermsVersionStore,
    private val termsConsentStore: TermsConsentStore,
    private val verificationStore: RegistrationVerificationStore,
    private val verificationTokenService: VerificationTokenService,
    private val registrationMailService: RegistrationMailService,
    private val clock: Clock
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun policy(): RegistrationPolicyResponse =
        RegistrationPolicyResponse(
            nickname = NicknamePolicyResponse(
                minLength = NICKNAME_MIN_LENGTH,
                maxLength = NICKNAME_MAX_LENGTH
            ),
            password = passwordPolicyEvaluator.policyResponse(),
            captcha = CaptchaPolicyResponse(
                mode = registrationProperties.captcha.mode.name.lowercase(),
                mockPassToken = registrationProperties.captcha.mockPassToken.takeIf {
                    registrationProperties.captcha.mode == CaptchaMode.MOCK
                },
                turnstileSiteKey = registrationProperties.captcha.turnstileSiteKey.takeIf {
                    registrationProperties.captcha.mode == CaptchaMode.TURNSTILE
                }
            ),
            terms = currentTermsVersion().let { termsVersion ->
                TermsPolicyResponse(
                    currentVersion = termsVersion.version,
                    contentSlug = termsVersion.contentSlug,
                    url = "/terms/${termsVersion.contentSlug}"
                )
            }
        )

    @Transactional
    fun register(request: RegistrationRequest): RegistrationAcceptedResponse {
        val now = Instant.now(clock)
        cleanupExpiredPendingRegistrations(now)
        val currentTermsVersion = currentTermsVersion()

        val nickname = request.nickname.requireField("nickname", "Bitte Nickname eingeben")
        val password = request.password.requireField("password", "Bitte Passwort eingeben")
        val passwordRepeat = request.passwordRepeat.requireField("passwordRepeat", "Bitte Passwort-Wiederholung eingeben")
        val email = request.email.requireField("email", "Bitte Email-Adresse eingeben")
        val captchaToken = request.captchaToken.requireField("captchaToken", "Bitte Captcha eingeben")
        val firstName = request.firstName.normalizeOptional()
        val lastName = request.lastName.normalizeOptional()

        if (nickname.length !in NICKNAME_MIN_LENGTH..NICKNAME_MAX_LENGTH) {
            throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "INVALID_NICKNAME",
                message = "Nickname muss zwischen $NICKNAME_MIN_LENGTH und $NICKNAME_MAX_LENGTH Zeichen lang sein",
                field = "nickname"
            )
        }

        if (!EMAIL_REGEX.matches(email)) {
            throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "INVALID_EMAIL",
                message = "Ungültiges Format der Email Adresse",
                field = "email"
            )
        }

        if (password != passwordRepeat) {
            throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "PASSWORD_REPEAT_MISMATCH",
                message = "Passwort-Wiederholung falsch",
                field = "passwordRepeat"
            )
        }

        if (!passwordPolicyEvaluator.isSatisfied(password)) {
            throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "PASSWORD_POLICY_FAILED",
                message = "Passwort entspricht nicht den Mindestanforderungen",
                field = "password"
            )
        }

        if (!captchaVerifier.verify(captchaToken)) {
            throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "INVALID_CAPTCHA",
                message = "Das Captcha wurde falsch eingegeben",
                field = "captchaToken"
            )
        }

        if (request.acceptTerms != true) {
            throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "TERMS_NOT_ACCEPTED",
                message = "Bitte die Nutzungsbedingungen akzeptieren",
                field = "acceptTerms"
            )
        }

        if (keycloakAdminClient.findUserByUsername(nickname) != null) {
            throw RegistrationException(
                status = HttpStatus.CONFLICT,
                code = "NICKNAME_ALREADY_EXISTS",
                message = "Nickname existiert bereits",
                field = "nickname",
                suggestedNickname = suggestNickname(nickname)
            )
        }

        if (keycloakAdminClient.findUserByEmail(email) != null) {
            throw RegistrationException(
                status = HttpStatus.CONFLICT,
                code = "EMAIL_ALREADY_EXISTS",
                message = "Email-Adresse existiert bereits",
                field = "email"
            )
        }

        val keycloakUserId = keycloakAdminClient.createPendingUser(
            RegistrationUserDraft(
                username = nickname,
                email = email,
                firstName = firstName,
                lastName = lastName,
                password = password
            )
        )

        try {
            keycloakAdminClient.assignRealmRoles(keycloakUserId, setOf(REGISTRATION_PENDING_ROLE))
            appUserStore.save(
                AppUser(
                    keycloakUserId = keycloakUserId,
                    nickname = nickname,
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    status = AppUserStatus.PENDING,
                    enabled = false,
                    emailVerified = false,
                    keycloakRole = REGISTRATION_PENDING_ROLE,
                    createdAt = now,
                    updatedAt = now
                )
            )
            termsConsentStore.save(
                TermsConsent(
                    keycloakUserId = keycloakUserId,
                    termsVersionId = currentTermsVersion.id
                        ?: throw IllegalStateException("Aktive AGB-Version hat keine ID"),
                    consentType = TermsConsentType.EXPLICIT_YES,
                    consentedAt = now
                )
            )

            val token = verificationTokenService.generateToken()
            val verification = verificationStore.save(
                RegistrationVerification(
                    keycloakUserId = keycloakUserId,
                    nickname = nickname,
                    email = email,
                    tokenHash = verificationTokenService.hash(token),
                    status = RegistrationVerificationStatus.PENDING,
                    expiresAt = now.plus(registrationProperties.verificationTtl),
                    createdAt = now
                )
            )

            registrationMailService.sendVerificationEmail(email, nickname, token)
            logger.info("Created pending registration {} for user {}", verification.id, nickname)
        } catch (exception: Exception) {
            keycloakAdminClient.deleteUser(keycloakUserId)
            termsConsentStore.deleteByKeycloakUserId(keycloakUserId)
            runCatching {
                markAppUserDeleted(keycloakUserId, now)
            }.onFailure { cleanupError ->
                logger.warn("Failed to mark app user {} as deleted after registration rollback", keycloakUserId, cleanupError)
            }
            throw exception
        }

        return RegistrationAcceptedResponse(
            message = "Die Verifizierungs-E-Mail wurde versendet"
        )
    }

    @Transactional
    fun verify(request: RegistrationVerifyRequest): RegistrationVerifyResponse {
        val token = request.token.requireField("token", "Der Verifizierungslink ist ungültig")
        val now = Instant.now(clock)
        val tokenHash = verificationTokenService.hash(token)
        val verification = verificationStore.findByTokenHash(tokenHash)
            ?: throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "INVALID_TOKEN",
                message = "Der Verifizierungslink ist ungültig",
                field = "token"
            )

        if (verification.status != RegistrationVerificationStatus.PENDING) {
            throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "INVALID_TOKEN",
                message = "Der Verifizierungslink ist ungültig",
                field = "token"
            )
        }

        if (verification.expiresAt.isBefore(now)) {
            appUserStore.save(
                appUserStore.findById(verification.keycloakUserId)?.copy(
                    nickname = verification.nickname,
                    email = verification.email,
                    status = AppUserStatus.EXPIRED,
                    enabled = false,
                    emailVerified = false,
                    updatedAt = now
                ) ?: AppUser(
                    keycloakUserId = verification.keycloakUserId,
                    nickname = verification.nickname,
                    email = verification.email,
                    firstName = null,
                    lastName = null,
                    status = AppUserStatus.EXPIRED,
                    enabled = false,
                    emailVerified = false,
                    keycloakRole = REGISTRATION_PENDING_ROLE,
                    createdAt = verification.createdAt ?: now,
                    updatedAt = now
                )
            )
            verificationStore.save(
                verification.copy(status = RegistrationVerificationStatus.EXPIRED)
            )

            throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "TOKEN_EXPIRED",
                message = "Der Verifizierungslink ist abgelaufen",
                field = "token"
            )
        }

        keycloakAdminClient.enableUser(verification.keycloakUserId)
        keycloakAdminClient.removeRealmRoles(verification.keycloakUserId, setOf(REGISTRATION_PENDING_ROLE))
        keycloakAdminClient.assignRealmRoles(verification.keycloakUserId, setOf(APP_USER_ROLE))
        appUserStore.save(
            appUserStore.findById(verification.keycloakUserId)?.copy(
                nickname = verification.nickname,
                email = verification.email,
                status = AppUserStatus.ACTIVE,
                enabled = true,
                emailVerified = true,
                keycloakRole = APP_USER_ROLE,
                updatedAt = now,
                verifiedAt = now,
                deletedAt = null
            ) ?: AppUser(
                keycloakUserId = verification.keycloakUserId,
                nickname = verification.nickname,
                email = verification.email,
                firstName = null,
                lastName = null,
                status = AppUserStatus.ACTIVE,
                enabled = true,
                emailVerified = true,
                keycloakRole = APP_USER_ROLE,
                createdAt = verification.createdAt ?: now,
                updatedAt = now,
                verifiedAt = now
            )
        )

        verificationStore.save(
            verification.copy(
                status = RegistrationVerificationStatus.VERIFIED,
                verifiedAt = now
            )
        )

        return RegistrationVerifyResponse(
            message = "Die Registrierung wurde erfolgreich bestätigt"
        )
    }

    @Transactional
    fun cleanupExpiredPendingRegistrations(now: Instant = Instant.now(clock)) {
        verificationStore.findExpiredPending(now).forEach { verification ->
            runCatching {
                keycloakAdminClient.deleteUser(verification.keycloakUserId)
            }.onFailure { error ->
                logger.warn("Failed to cleanup expired registration {}", verification.id, error)
            }

            appUserStore.save(
                appUserStore.findById(verification.keycloakUserId)?.copy(
                    nickname = verification.nickname,
                    email = verification.email,
                    status = AppUserStatus.EXPIRED,
                    enabled = false,
                    emailVerified = false,
                    updatedAt = now,
                    deletedAt = now
                ) ?: AppUser(
                    keycloakUserId = verification.keycloakUserId,
                    nickname = verification.nickname,
                    email = verification.email,
                    firstName = null,
                    lastName = null,
                    status = AppUserStatus.EXPIRED,
                    enabled = false,
                    emailVerified = false,
                    keycloakRole = REGISTRATION_PENDING_ROLE,
                    createdAt = verification.createdAt ?: now,
                    updatedAt = now,
                    deletedAt = now
                )
            )
            verificationStore.save(verification.copy(status = RegistrationVerificationStatus.EXPIRED))
        }
    }

    private fun markAppUserDeleted(keycloakUserId: String, now: Instant) {
        val existing = appUserStore.findById(keycloakUserId) ?: return
        appUserStore.save(
            existing.copy(
                status = AppUserStatus.DELETED,
                enabled = false,
                emailVerified = false,
                updatedAt = now,
                deletedAt = now
            )
        )
    }

    private fun suggestNickname(nickname: String): String {
        var suffix = 1
        while (true) {
            val suggestion = "$nickname$suffix"
            if (keycloakAdminClient.findUserByUsername(suggestion) == null) {
                return suggestion
            }

            suffix += 1
        }
    }

    private fun String?.requireField(field: String, message: String): String {
        val value = this.normalizeOptional()
        if (!StringUtils.hasText(value)) {
            throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "FIELD_REQUIRED",
                message = message,
                field = field
            )
        }

        return value!!
    }

    private fun String?.normalizeOptional(): String? =
        this?.trim()?.takeIf(StringUtils::hasText)

    private fun currentTermsVersion(): TermsVersion {
        val activeVersions = termsVersionStore.findActive()

        return when (activeVersions.size) {
            1 -> activeVersions.single()
            0 -> throw IllegalStateException("Keine aktive AGB-Version konfiguriert")
            else -> throw IllegalStateException("Mehrere aktive AGB-Versionen konfiguriert")
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        private const val NICKNAME_MIN_LENGTH = 3
        private const val NICKNAME_MAX_LENGTH = 255
        private const val REGISTRATION_PENDING_ROLE = "registration-pending"
        private const val APP_USER_ROLE = "app-user"
    }
}
