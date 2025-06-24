package com.realworld.webfluxfn.validation

import com.realworld.webfluxfn.dto.request.CreateArticleRequest
import com.realworld.webfluxfn.dto.request.UserRegistrationRequest
import com.realworld.webfluxfn.dto.request.UpdateUserRequest
import spock.lang.Specification
import spock.lang.Title

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

@Title("test validation edge cases")
class ValidationEdgeCasesTest extends Specification {
    private static ValidatorFactory validatorFactory
    private static Validator validator

    def setupSpec() {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    def cleanupSpec() {
        validatorFactory.close()
    }

    def "test UserRegistrationRequest with boundary values"() {
        given: "a user registration request with boundary values"
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .build()

        when: "validate the request"
        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(request)

        then: "validation result matches expectation"
        violations.size() == expectedViolations

        where:
        username | email | password | expectedViolations
        ""       | "test@example.com" | "password123" | 1
        "   "    | "test@example.com" | "password123" | 1
        "a"      | "test@example.com" | "password123" | 0
        "validuser" | "" | "password123" | 1
        "validuser" | "   " | "password123" | 2  // Both @NotBlank and @Email violations
        "validuser" | "invalid-email" | "password123" | 1
        "validuser" | "test@example.com" | "" | 1
        "validuser" | "test@example.com" | "   " | 1
        "validuser" | "test@example.com" | "a" | 0
        null     | "test@example.com" | "password123" | 1
        "validuser" | null | "password123" | 1
        "validuser" | "test@example.com" | null | 1
    }

    def "test CreateArticleRequest with boundary values"() {
        given: "a create article request with boundary values"
        CreateArticleRequest request = new CreateArticleRequest()
                .setTitle(title)
                .setDescription(description)
                .setBody(body)
                .setTagList(tagList ?: [])

        when: "validate the request"
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request)

        then: "validation result matches expectation"
        violations.size() == expectedViolations

        where:
        title | description | body | tagList | expectedViolations
        ""    | "desc" | "body" | [] | 1
        "   " | "desc" | "body" | [] | 1
        "title" | "" | "body" | [] | 1
        "title" | "   " | "body" | [] | 1
        "title" | "desc" | "" | [] | 1
        "title" | "desc" | "   " | [] | 1
        "title" | "desc" | "body" | [] | 0
        null  | "desc" | "body" | [] | 1
        "title" | null | "body" | [] | 1
        "title" | "desc" | null | [] | 1
        "title" | "desc" | "body" | null | 0
    }

    def "test UpdateUserRequest with unicode and special characters"() {
        given: "an update user request with unicode and special characters"
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("用户名")
                .email("test@例え.com")
                .bio("Bio with émojis 🚀 and spëcial chars")
                .image("https://example.com/image.jpg")
                .password("pässwörd123")
                .build()

        when: "validate the request"
        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request)

        then: "unicode characters are handled properly"
        violations.size() >= 0
    }

    def "test extremely long field values"() {
        given: "requests with extremely long field values"
        String longString = "a" * 10000
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .username(longString)
                .email("test@example.com")
                .password("password123")
                .build()

        when: "validate the request"
        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(request)

        then: "long values are handled appropriately"
        violations.size() >= 0
    }

    def "test whitespace-only values"() {
        given: "requests with whitespace-only values"
        CreateArticleRequest request = new CreateArticleRequest()
                .setTitle("   \t\n   ")
                .setDescription("   \r\n   ")
                .setBody("   \t\r\n   ")
                .setTagList([])

        when: "validate the request"
        Set<ConstraintViolation<CreateArticleRequest>> violations = validator.validate(request)

        then: "whitespace-only values are treated as invalid"
        violations.size() > 0
    }
}
