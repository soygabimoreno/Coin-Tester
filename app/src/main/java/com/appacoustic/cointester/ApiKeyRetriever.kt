package com.appacoustic.cointester

object ApiKeyRetriever {

    init {
        System.loadLibrary("api-keys")
    }

    external fun getAmplitudeApiKey(): String
}
