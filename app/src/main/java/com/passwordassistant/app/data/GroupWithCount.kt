package com.passwordassistant.app.data

import androidx.room.Embedded

data class GroupWithCount(
    @Embedded val group: GroupEntity,
    val entryCount: Int,
)
