package de.heuermannplus.backend.registration

import org.springframework.stereotype.Component

@Component
class PasswordPolicyEvaluator(
    private val registrationProperties: RegistrationProperties
) {

    fun policyResponse(): PasswordPolicyResponse =
        PasswordPolicyResponse(
            minLength = registrationProperties.password.minLength,
            minUpperCase = registrationProperties.password.minUpperCase,
            minLowerCase = registrationProperties.password.minLowerCase,
            minDigits = registrationProperties.password.minDigits,
            minSpecialChars = registrationProperties.password.minSpecialChars
        )

    fun evaluate(password: String): PasswordEvaluation {
        val upperCaseCount = password.count(Char::isUpperCase)
        val lowerCaseCount = password.count(Char::isLowerCase)
        val digitCount = password.count(Char::isDigit)
        val specialCharCount = password.count { !it.isLetterOrDigit() && !it.isWhitespace() }

        return PasswordEvaluation(
            requirements = listOf(
                PasswordRequirementStatus(
                    code = "MIN_LENGTH",
                    label = "Mindestens ${registrationProperties.password.minLength} Zeichen",
                    satisfied = password.length >= registrationProperties.password.minLength
                ),
                PasswordRequirementStatus(
                    code = "UPPER_CASE",
                    label = "Mindestens ${registrationProperties.password.minUpperCase} Grossbuchstabe",
                    satisfied = upperCaseCount >= registrationProperties.password.minUpperCase
                ),
                PasswordRequirementStatus(
                    code = "LOWER_CASE",
                    label = "Mindestens ${registrationProperties.password.minLowerCase} Kleinbuchstabe",
                    satisfied = lowerCaseCount >= registrationProperties.password.minLowerCase
                ),
                PasswordRequirementStatus(
                    code = "DIGIT",
                    label = "Mindestens ${registrationProperties.password.minDigits} Ziffer",
                    satisfied = digitCount >= registrationProperties.password.minDigits
                ),
                PasswordRequirementStatus(
                    code = "SPECIAL_CHAR",
                    label = "Mindestens ${registrationProperties.password.minSpecialChars} Sonderzeichen",
                    satisfied = specialCharCount >= registrationProperties.password.minSpecialChars
                )
            )
        )
    }

    fun isSatisfied(password: String): Boolean =
        evaluate(password).requirements.all(PasswordRequirementStatus::satisfied)
}
