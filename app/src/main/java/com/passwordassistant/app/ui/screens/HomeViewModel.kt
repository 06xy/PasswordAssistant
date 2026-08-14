package com.passwordassistant.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.passwordassistant.app.PasswordApp
import com.passwordassistant.app.data.GroupWithCount
import kotlinx.coroutines.flow.Flow

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = (application as PasswordApp).container.database

    val groups: Flow<List<GroupWithCount>> = database.groupDao().observeGroupsWithCount()
}
