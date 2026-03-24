package de.heuermannplus.backend

import de.heuermannplus.backend.api.PublicController
import kotlin.test.Test
import kotlin.test.assertEquals

class PublicControllerTest {

    @Test
    fun `health endpoint payload exposes up status`() {
        val payload = PublicController().health()

        assertEquals("UP", payload["status"])
        assertEquals("heuermannplus-backend", payload["application"])
    }
}
