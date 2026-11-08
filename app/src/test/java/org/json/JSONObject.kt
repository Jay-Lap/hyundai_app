package org.json

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

/** Only the methods used by ReviewRepository are implemented in this test shim. */
class JSONObject internal constructor(internal val objectValue: JsonObject) {
    constructor() : this(JsonObject())
    fun put(key: String, value: Boolean): JSONObject { objectValue.addProperty(key, value); return this }
    fun put(key: String, value: Long): JSONObject { objectValue.addProperty(key, value); return this }
    fun put(key: String, value: Any?): JSONObject { objectValue.addProperty(key, value as String?); return this }
    fun opt(key: String): Any? {
        val item = objectValue.get(key) ?: return null
        if (!item.isJsonPrimitive) return item
        val primitive: JsonPrimitive = item.asJsonPrimitive
        return when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isNumber -> primitive.asNumber
            primitive.isString -> primitive.asString
            else -> null
        }
    }
    fun optLong(key: String, fallback: Long): Long = (opt(key) as? Number)?.toLong() ?: fallback
    fun getBoolean(key: String): Boolean = opt(key) as Boolean
    fun getString(key: String): String = opt(key) as String
    fun keys(): MutableIterator<String> = objectValue.keySet().iterator()
    override fun toString(): String = objectValue.toString()
}
