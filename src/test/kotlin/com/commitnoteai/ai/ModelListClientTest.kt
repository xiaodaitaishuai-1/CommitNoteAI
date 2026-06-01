package com.commitnoteai.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModelListClientTest {
    @Test
    fun `parse extracts and sorts model ids with gpt models first`() {
        val models = ModelListClient.parseModels(
            """
            {
              "data": [
                {"id": "text-embedding-3-small"},
                {"id": "gpt-5.5"},
                {"owned_by": "system"},
                {"id": "gpt-5.2"}
              ]
            }
            """.trimIndent(),
        )

        assertEquals(listOf("gpt-5.2", "gpt-5.5", "text-embedding-3-small"), models)
    }

    @Test
    fun `parse reports empty model list`() {
        val error = assertFailsWith<IllegalStateException> {
            ModelListClient.parseModels("""{"data": []}""")
        }

        assertEquals("模型列表为空", error.message)
    }
}
