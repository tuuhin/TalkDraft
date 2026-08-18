package com.sam.talkdraft

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform