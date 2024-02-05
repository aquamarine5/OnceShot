package org.aquarngd.stackbricks

import android.icu.util.VersionInfo
import org.aquarngd.stackbricks.WeiboCmtsMsgPvder

class MsgPvderManager {
    companion object {
        private val MsgPvderMatchDict = mapOf<String, IMsgPvder>(
            WeiboCmtsMsgPvder.MsgPvderID to WeiboCmtsMsgPvder()
        )

        fun ParseFromId(msgPvderId: String): IMsgPvder? {
            return MsgPvderMatchDict[msgPvderId]
        }
    }
}

data class UpdateMessage(
    val version: VersionInfo,
    val pkgPvderId: String,
    val pkgPvderData: String
)

interface IMsgPvder {
    suspend fun GetUpdateMessage(msgPvderData: String): UpdateMessage
    val ID: String
}