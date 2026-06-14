package com.example.data

import java.util.UUID

data class MessageItem(
    val id: String = UUID.randomUUID().toString(),
    var text: String = "",
    var delaySeconds: Int = 2 // 1, 2, or 3 seconds
)
