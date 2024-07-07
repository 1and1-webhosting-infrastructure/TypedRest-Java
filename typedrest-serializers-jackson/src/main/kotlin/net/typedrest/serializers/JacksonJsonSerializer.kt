package net.typedrest.serializers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Serializes and deserializes entities as JSON using Jackson.
 *
 * @param mapper The Jackson object mapper to use for serializing and deserializing.
 */
open class JacksonJsonSerializer @JvmOverloads constructor(
    private val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()
) : AbstractJsonSerializer() {
    override fun <T> serialize(entity: T, type: Class<T>): RequestBody =
        mapper.writeValueAsString(entity).toRequestBody(mediaTypeJson)

    override fun <T> serializeList(entities: Iterable<T>, type: Class<T>): RequestBody =
        mapper.writeValueAsString(entities).toRequestBody(mediaTypeJson)

    override fun <T> deserialize(body: ResponseBody, type: Class<T>): T? =
        body.byteStream().use { mapper.readerFor(type).readValue(it) }

    override fun <T> deserializeList(body: ResponseBody, type: Class<T>): List<T>? =
        body.byteStream().use { mapper.readerForListOf(type).readValue(it) }
}
