package com.ashmeet.hyperlauncher.plugin.renderer_v2.data

import com.tencent.mmkv.kmp.MMKV

fun rendererEnvMMKV(): MMKV = MMKV.mmkvWithID("RendererEnvConfig")
