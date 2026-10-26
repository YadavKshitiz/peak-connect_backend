package com.peakconnect.app

import com.peakconnect.app.core.session.JwtDecoder
import com.peakconnect.app.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class JwtDecoderTest {

    private fun createToken(payloadJson: String): String {
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.toByteArray())
        val signature = "fake_signature"
        return "$header.$payload.$signature"
    }

    @Test
    fun `decode parses valid claims correctly`() {
        val json = """{"sub":"trekker@example.com","userId":"user-42","role":"ROLE_GUIDE","exp":1800000000}"""
        val token = createToken(json)

        val payload = JwtDecoder.decode(token)
        assertNotNull(payload)
        assertEquals("trekker@example.com", payload?.email)
        assertEquals("user-42", payload?.userId)
        assertEquals(Role.GUIDE, payload?.role)
        assertEquals(1800000000L, payload?.expiresAt)
    }

    @Test
    fun `isExpired returns false for future exp and true for past exp`() {
        val futureExp = System.currentTimeMillis() / 1000 + 3600
        val futureToken = createToken("""{"sub":"test@test.com","userId":"1","role":"TREKKER","exp":$futureExp}""")
        assertFalse(JwtDecoder.isExpired(futureToken))

        val pastExp = System.currentTimeMillis() / 1000 - 3600
        val pastToken = createToken("""{"sub":"test@test.com","userId":"1","role":"TREKKER","exp":$pastExp}""")
        assertTrue(JwtDecoder.isExpired(pastToken))
    }

    @Test
    fun `decode returns null for malformed tokens`() {
        assertNull(JwtDecoder.decode(""))
        assertNull(JwtDecoder.decode("single_part"))
        assertNull(JwtDecoder.decode("part1.part2"))
        assertNull(JwtDecoder.decode("part1.part2.part3.part4"))
        assertNull(JwtDecoder.decode("header.invalid_base_64!@#.sig"))

        // Missing required sub or userId
        val missingSub = createToken("""{"userId":"1","role":"TREKKER"}""")
        assertNull(JwtDecoder.decode(missingSub))

        val missingUserId = createToken("""{"sub":"test@test.com","role":"TREKKER"}""")
        assertNull(JwtDecoder.decode(missingUserId))
    }
}
