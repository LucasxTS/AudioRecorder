package com.example.audiorecorder.models

import kotlinx.serialization.Serializable

@Serializable
data class Audio(
    val title : String,
    val filePath : String
)
