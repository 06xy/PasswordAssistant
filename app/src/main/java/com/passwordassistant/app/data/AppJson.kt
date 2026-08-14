package com.passwordassistant.app.data

import kotlinx.serialization.json.Json

object AppJson {
    val json: Json = Json { ignoreUnknownKeys = true }
}
