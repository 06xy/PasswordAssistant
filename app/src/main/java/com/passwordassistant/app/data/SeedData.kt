package com.passwordassistant.app.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

object SeedData {
    private val json = AppJson.json

    private fun newKey(): String = "f_" + UUID.randomUUID().toString().take(8)

    fun defaultGroups(): List<GroupEntity> = listOf(
        GroupEntity(
            name = "SSH 密码",
            icon = "terminal",
            colorIndex = 2,
            fieldsJson = json.encodeToString(
                listOf(
                    FieldDefinition(newKey(), "服务器名称", FieldType.TEXT, required = true),
                    FieldDefinition(newKey(), "IP 地址", FieldType.TEXT),
                    FieldDefinition(newKey(), "端口", FieldType.NUMBER, defaultValue = "22"),
                    FieldDefinition(newKey(), "用户名", FieldType.TEXT),
                    FieldDefinition(newKey(), "密码", FieldType.PASSWORD, required = true),
                    FieldDefinition(newKey(), "备注", FieldType.MULTILINE),
                ),
            ),
        ),
        GroupEntity(
            name = "QQ 密码",
            icon = "chat",
            colorIndex = 4,
            fieldsJson = json.encodeToString(
                listOf(
                    FieldDefinition(newKey(), "QQ 号码", FieldType.TEXT),
                    FieldDefinition(newKey(), "昵称", FieldType.TEXT),
                    FieldDefinition(newKey(), "密码", FieldType.PASSWORD, required = true),
                    FieldDefinition(newKey(), "备注", FieldType.MULTILINE),
                ),
            ),
        ),
    )
}
