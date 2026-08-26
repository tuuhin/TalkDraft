package com.sam.talkdraft.player.exceptions

internal class PlayerCommandNotFoundException(val commandCode: Int) :
    Exception("Cannot set player media item please configure it correctly")
