package com.example.util

import android.util.Base64

object InviteCode {
    fun encode(ip: String, port: Int): String {
        val payload = "$ip:$port"
        return Base64.encodeToString(payload.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)
    }

    fun decode(code: String): Pair<String, Int>? {
        return try {
            val decoded = String(Base64.decode(code, Base64.NO_WRAP or Base64.URL_SAFE), Charsets.UTF_8)
            val parts = decoded.split(":")
            if (parts.size == 2) {
                Pair(parts[0], parts[1].toInt())
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
