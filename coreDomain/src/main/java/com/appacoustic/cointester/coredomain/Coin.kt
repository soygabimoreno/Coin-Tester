package com.appacoustic.cointester.coredomain

data class Coin(
    var name: String,
    var place: String,
    var head: Int
) {
    var tones: IntArray? = null
    var tail: Int = 0
    var diameter: Float = 0F
    var weight: Float = 0F
    var purity: Float = 0F
    var pmContent: Float = 0F
    var thickness: Float = 0F
}
