package com.example.util

import android.util.Base64

object InviteCode {
    fun encode(ip: String, port: Int, chatPort: Int): String {
        val payload = "$ip:$port:$chatPort"
        return Base64.encodeToString(payload.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)
    }

    fun decode(code: String): Triple<String, Int, Int>? {
        return try {
            val decoded = String(Base64.decode(code, Base64.NO_WRAP or Base64.URL_SAFE), Charsets.UTF_8)
            val parts = decoded.split(":")
            if (parts.size == 3) {
                Triple(parts[0], parts[1].toInt(), parts[2].toInt())
            } else if (parts.size == 2) {
                Triple(parts[0], parts[1].toInt(), -1)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
