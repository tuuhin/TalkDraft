package com.sam.talkdraft.model_downloader.domain.exceptions

internal class ModelVerificationFailedException :
    IllegalStateException("Model hashes cannot be matched, data maybe corrupted restart download")

