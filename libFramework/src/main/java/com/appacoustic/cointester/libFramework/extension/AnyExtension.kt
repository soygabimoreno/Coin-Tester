package com.appacoustic.cointester.libFramework.extension

import com.google.gson.Gson
import org.json.JSONObject

val Any?.exhaustive
    get() = Unit

fun Any.toJSONObject(): JSONObject {
    return JSONObject(
        Gson().toJson(
            this,
            Any::class.java
        )
    )
}
