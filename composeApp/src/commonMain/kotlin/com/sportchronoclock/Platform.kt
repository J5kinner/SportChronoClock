package com.sportchronoclock

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform