package org.json

import com.google.gson.JsonArray
import com.google.gson.JsonParser

/** JVM-only replacement for the Android JSON API used by ReviewRepository. */
class JSONArray {
    private val array: JsonArray
    constructor() { array = JsonArray() }
    constructor(json: String?) { array = JsonParser.parseString(json).asJsonArray }
    fun length(): Int = array.size()
    fun optJSONObject(index: Int): JSONObject? = array.get(index)?.takeIf { it.isJsonObject }?.let { JSONObject(it.asJsonObject) }
    fun put(value: Any?): JSONArray { array.add((value as JSONObject).objectValue); return this }
    override fun toString(): String = array.toString()
}
