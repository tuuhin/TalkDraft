package com.sam.talkdraft.recorder.domain.exception

internal class RecorderInitMissingException :
    IllegalStateException("Recorder is not initialized, please initialize it to continue")
