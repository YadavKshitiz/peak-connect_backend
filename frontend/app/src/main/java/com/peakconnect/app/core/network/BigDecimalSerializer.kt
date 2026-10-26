package com.peakconnect.app.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return try {
            val jsonDecoder = decoder as? JsonDecoder
            if (jsonDecoder != null) {
                val element = jsonDecoder.decodeJsonElement()
                BigDecimal(element.jsonPrimitive.content)
            } else {
                BigDecimal(decoder.decodeString())
            }
        } catch (_: Exception) {
            BigDecimal.ZERO
        }
    }
}
