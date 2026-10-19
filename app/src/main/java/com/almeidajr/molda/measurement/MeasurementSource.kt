package com.almeidajr.molda.measurement

import java.time.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * The measurement profile a waist came from, copied as plain values; the date is null when it
 * was not known.
 */
@Serializable
data class MeasurementSource(
    val profileName: String,
    @Serializable(with = IsoLocalDateSerializer::class) val measurementDate: LocalDate?,
)

/** A [LocalDate] as ISO-8601 text, like the dates Molda stores. */
object IsoLocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}
