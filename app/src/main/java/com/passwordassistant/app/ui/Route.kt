package com.passwordassistant.app.ui

object Route {
    const val Home = "home"
    const val Settings = "settings"
    const val GroupDetail = "group/{groupId}"
    const val GroupEdit = "group/{groupId}/edit"
    const val EntryEdit = "entry/{groupId}/{entryId}"
    const val EntryView = "entry/{groupId}/{entryId}/view"

    const val ArgGroupId = "groupId"
    const val ArgEntryId = "entryId"

    fun groupDetail(groupId: Long) = "group/$groupId"

    fun groupEdit(groupId: Long) = "group/$groupId/edit"

    fun entryEdit(groupId: Long, entryId: Long) = "entry/$groupId/$entryId"

    fun entryView(groupId: Long, entryId: Long) = "entry/$groupId/$entryId/view"
}
