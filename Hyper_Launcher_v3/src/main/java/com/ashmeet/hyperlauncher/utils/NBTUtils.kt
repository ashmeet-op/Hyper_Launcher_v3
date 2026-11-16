package com.ashmeet.hyperlauncher.utils

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream


object NBTUtils {
    const val TAG_END = 0
    const val TAG_BYTE = 1
    const val TAG_SHORT = 2
    const val TAG_INT = 3
    const val TAG_LONG = 4
    const val TAG_FLOAT = 5
    const val TAG_DOUBLE = 6
    const val TAG_BYTE_ARRAY = 7
    const val TAG_STRING = 8
    const val TAG_LIST = 9
    const val TAG_COMPOUND = 10
    const val TAG_INT_ARRAY = 11
    const val TAG_LONG_ARRAY = 12

    @Throws(IOException::class)
    fun read(inputStream: InputStream): Tag? {
        DataInputStream(GZIPInputStream(inputStream)).use { dis ->
            val type = dis.readByte().toInt()
            if (type == TAG_END) return null
            val name = dis.readUTF()
            return readTag(dis, type, name)
        }
    }

    @Throws(IOException::class)
    private fun readTag(dis: DataInputStream, type: Int, name: String): Tag {
        return when (type) {
            TAG_BYTE -> Tag(name, dis.readByte(), type)
            TAG_SHORT -> Tag(name, dis.readShort(), type)
            TAG_INT -> Tag(name, dis.readInt(), type)
            TAG_LONG -> Tag(name, dis.readLong(), type)
            TAG_FLOAT -> Tag(name, dis.readFloat(), type)
            TAG_DOUBLE -> Tag(name, dis.readDouble(), type)
            TAG_BYTE_ARRAY -> {
                val bLen = dis.readInt()
                val bData = ByteArray(bLen)
                dis.readFully(bData)
                Tag(name, bData, type)
            }
            TAG_STRING -> Tag(name, dis.readUTF(), type)
            TAG_LIST -> {
                val lType = dis.readByte().toInt()
                val lLen = dis.readInt()
                val lData = ArrayList<Tag>(lLen)
                for (i in  0 until lLen) lData.add(readTag(dis, lType, ""))
                Tag(name, lData, type)
            }
            TAG_COMPOUND -> {
                val cData = HashMap<String, Tag>()
                var cType: Int
                while (dis.readByte().toInt().also { cType = it } != TAG_END) {
                    val cName = dis.readUTF()
                    cData[cName] = readTag(dis, cType, cName)
                }
                Tag(name, cData, type)
            }
            TAG_INT_ARRAY -> {
                val iLen = dis.readInt()
                val iData = IntArray(iLen)
                for (i in 0 until iLen) iData[i] = dis.readInt()
                Tag(name, iData, type)
            }
            TAG_LONG_ARRAY -> {
                val loLen = dis.readInt()
                val loData = LongArray(loLen)
                for (i in 0 until loLen) loData[i] = dis.readLong()
                Tag(name, loData, type)
            }
            else -> throw IOException("Unknown NBT tag type: $type")
        }
    }

    @Suppress("unused")
    class Tag(val name: String, val value: Any, val type: Int) {
        @Suppress("UNCHECKED_CAST")
        operator fun get(key: String): Tag? {
            return if (type == TAG_COMPOUND) (value as Map<String, Tag>)[key] else null
        }

        @Suppress("UNCHECKED_CAST")
        fun asList(): List<Tag> = value as List<Tag>
        fun asString(): String = value as String
        fun asInt(): Int = value as Int
        fun asLong(): Long = value as Long
    }
}
