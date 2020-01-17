package io.horizontalsystems.bitcoincore.network.messages
import java.io.ByteArrayInputStream
import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.io.BitcoinOutput

class VerAckMessage(val vKnownChains: Long) : IMessage {
    override fun toString(): String {
        return "VerAckMessage()"
    }
}

class VerAckMessageParser : IMessageParser {
    override val command: String = "verack"

    override fun parseMessage(payload: ByteArray): IMessage {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val vKnownChains = input.readUnsignedInt()
            return PongMessage(vKnownChains)
        }
    }
}

class VerAckMessageSerializer : IMessageSerializer {
    override val command: String = "verack"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is VerAckMessage) {
            return null
        }

        return BitcoinOutput()
                .writeUnsignedInt(message.vKnownChains)
                .toByteArray()
    }
}
