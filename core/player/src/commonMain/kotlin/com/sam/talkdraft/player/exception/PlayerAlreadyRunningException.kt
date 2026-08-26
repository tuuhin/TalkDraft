package com.sam.talkdraft.player.exception

internal class PlayerAlreadyRunningException :
    Exception("Cannot configure as some other thread maybe preparing the player")

