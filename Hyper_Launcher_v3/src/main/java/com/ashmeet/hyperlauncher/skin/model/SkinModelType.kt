package com.ashmeet.hyperlauncher.skin.model

import androidx.annotation.Keep


@Keep
enum class SkinModelType(val targetParity: Int) {
    NONE(-1),
    STEVE(0),
    ALEX(1)
}