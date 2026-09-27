package com.example.thasmathjagratha.manager

import android.content.Context
import com.example.thasmathjagratha.model.DeviceRole

class DeviceRoleManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("device_role_prefs", Context.MODE_PRIVATE)

    fun getRole(): DeviceRole {
        val name = prefs.getString("selected_role", DeviceRole.UNSET.name) ?: DeviceRole.UNSET.name
        return runCatching { DeviceRole.valueOf(name) }.getOrDefault(DeviceRole.UNSET)
    }

    fun setRole(role: DeviceRole) {
        prefs.edit().putString("selected_role", role.name).commit()
    }

    fun clearRole() {
        prefs.edit().remove("selected_role").commit()
    }
}
