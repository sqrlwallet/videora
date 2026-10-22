package com.kevan.videorabykevan

import com.kevan.videorabykevan.ai.AiError
import com.kevan.videorabykevan.ai.AiKeyFormat
import com.kevan.videorabykevan.ai.AiTask
import com.kevan.videorabykevan.ai.OpenRouterJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterTest {

    @Test
    fun parsesKeyInfoWithAndWithoutLimit() {
        val capped = OpenRouterJson.parseKeyInfo(
            """{"data":{"label":"sk-or-v1-abc","limit":10,"limit_remaining":7.5,"usage":2.5,"is_free_tier":false,"rate_limit":{}}}"""
        )
        assertEquals(10.0, capped.limitUsd!!, 0.0)
        assertEquals(7.5, capped.remainingUsd!!, 0.0)
        assertEquals(2.5, capped.usageUsd, 0.0)
        assertFalse(capped.isFreeTier)

        val uncapped = OpenRouterJson.parseKeyInfo("""{"data":{"limit":null,"limit_remaining":null,"usage":0,"is_free_tier":true}}""")
        assertNull(uncapped.limitUsd)
        assertNull(uncapped.remainingUsd)
        assertTrue(uncapped.isFreeTier)
    }

    @Test(expected = AiError.BadResponse::class)
    fun rejectsKeyInfoWithoutData() {
        OpenRouterJson.parseKeyInfo("<html>gateway error</html>")
    }

    @Test
    fun filtersAndSortsModelsByTask() {
        val models = OpenRouterJson.parseModels(
            """{"data":[
              {"id":"a/text-pricey","name":"Pricey","architecture":{"input_modalities":["text"]},"pricing":{"prompt":"0.00001"}},
              {"id":"b/omni","name":"Omni","architecture":{"input_modalities":["text","audio","image"]},"pricing":{"prompt":"0.0000001"}},
              {"id":"b/omni:batch","name":"Omni batch","architecture":{"input_modalities":["text","audio"]},"pricing":{"prompt":"0.00000005"}},
              {"id":"c/free","name":"Free","architecture":{"input_modalities":["text"]},"pricing":{"prompt":"0"}},
              {"id":"d/unpriced","architecture":{"input_modalities":["text"]}},
              {"name":"no id"}
            ]}"""
        )
        assertEquals(5, models.size)
        assertEquals("d/unpriced", models.last().name) // name falls back to the id

        assertEquals(listOf("b/omni"), OpenRouterJson.modelsFor(AiTask.AUDIO, models).map { it.id })
        assertEquals(
            listOf("c/free", "b/omni", "a/text-pricey", "d/unpriced"),
            OpenRouterJson.modelsFor(AiTask.TEXT, models).map { it.id }
        )
        assertTrue(models.first { it.id == "c/free" }.isFree)
        assertEquals(0.1, models.first { it.id == "b/omni" }.promptPricePerMillion!!, 1e-9)
    }

    @Test
    fun mapsHttpErrors() {
        assertTrue(OpenRouterJson.errorFor(401, null, null) is AiError.InvalidKey)

        val credits = OpenRouterJson.errorFor(402, """{"error":{"code":402,"message":"Insufficient credits"}}""", null)
        assertTrue(credits is AiError.NoCredits)
        assertEquals("Out of OpenRouter credits: Insufficient credits", credits.message)

        val limited = OpenRouterJson.errorFor(429, "not json", " 12 ")
        assertEquals(12L, (limited as AiError.RateLimited).retryAfterSeconds)

        assertEquals("OpenRouter error 503", OpenRouterJson.errorFor(503, "", null).message)
    }

    @Test
    fun validatesAndMasksKeys() {
        assertTrue(AiKeyFormat.looksValid("  sk-or-v1-0123456789abcdef  "))
        assertFalse(AiKeyFormat.looksValid("sk-proj-0123456789abcdef"))
        assertFalse(AiKeyFormat.looksValid("sk-or-short"))
        assertEquals("sk-or-…cdef", AiKeyFormat.mask("sk-or-v1-0123456789abcdef"))
    }
}
