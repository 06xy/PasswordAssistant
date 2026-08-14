package com.passwordassistant.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FieldType(val label: String) {
    @SerialName("text")
    TEXT("文本"),

    @SerialName("number")
    NUMBER("数字"),

    @SerialName("password")
    PASSWORD("密码"),

    @SerialName("multiline")
    MULTILINE("多行文本"),
}
