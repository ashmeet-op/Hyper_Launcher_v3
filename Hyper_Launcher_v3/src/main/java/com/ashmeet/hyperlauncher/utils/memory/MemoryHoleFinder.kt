package com.ashmeet.hyperlauncher.utils.memory

import com.ashmeet.hyperlauncher.utils.Architecture
import com.ashmeet.hyperlauncher.utils.SelfMapsParser

class MemoryHoleFinder : SelfMapsParser.Callback {
    private var mPreviousEnd: Long = 0
    var largestHole: Long = -1
        private set
    private val mAddressingLimit: Long = Architecture.getAddressSpaceLimit()

    override fun process(startAddress: Long, endAddress: Long, wholeLine: String?): Boolean {
        var begin = startAddress
        if (begin >= mAddressingLimit) begin = mAddressingLimit
        val holeSize = begin - mPreviousEnd
        if (largestHole < holeSize) largestHole = holeSize
        if (begin == mAddressingLimit) return false
        mPreviousEnd = endAddress
        return true
    }
}
