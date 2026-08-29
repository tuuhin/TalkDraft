package com.sam.talkdraft.model_manager.domain.exceptions

import kotlin.uuid.Uuid

internal class ModelWithGivenIdNotFoundException(val uuid: Uuid) :
    Exception("Cannot find the model with given id :$uuid")

