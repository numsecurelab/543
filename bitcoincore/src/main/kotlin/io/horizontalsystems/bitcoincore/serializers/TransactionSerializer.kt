package io.horizontalsystems.bitcoincore.serializers

import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.HashUtils

object TransactionSerializer {
    fun deserialize(input: BitcoinInput): FullTransaction {
        val transaction = Transaction()
        val inputs = mutableListOf<TransactionInput>()
        val outputs = mutableListOf<TransactionOutput>()

        transaction.version = input.readInt()

        val marker = 0xff and input.readUnsignedByte()
        val inputCount = if (marker == 0) {  // segwit marker: 0x00
            input.read()  // skip segwit flag: 0x01
            transaction.segwit = true
            input.readVarInt()
        } else {
            input.readVarInt(marker)
        }

        //  inputs
        for (i in 0 until inputCount) {
            inputs.add(InputSerializer.deserialize(input))
        }

        //  outputs
        val outputCount = input.readVarInt()
        for (i in 0 until outputCount) {
            outputs.add(OutputSerializer.deserialize(input, i))
        }

        //  extract witness data
        if (transaction.segwit) {
            inputs.forEach {
                it.witness = InputSerializer.deserializeWitness(input)
            }
        }

        transaction.m_nSrcChain = input.readUnsignedInt()
        transaction.m_nDestChain = input.readUnsignedInt()
        transaction.lockTime = input.readUnsignedInt()

        val fullTransaction = FullTransaction(transaction, inputs, outputs)

        fullTransaction.header.hash = HashUtils.doubleSha256(serialize(fullTransaction, withWitness = false))
        fullTransaction.inputs.forEach {
            it.transactionHash = fullTransaction.header.hash
        }

        fullTransaction.outputs.forEach {
            it.transactionHash = fullTransaction.header.hash
        }

        return fullTransaction
    }

    fun serialize(transaction: FullTransaction, withWitness: Boolean = true): ByteArray {
        val header = transaction.header
        val buffer = BitcoinOutput()
        buffer.writeInt(header.version)

        if (header.segwit && withWitness) {
            buffer.writeByte(0) // marker 0x00
            buffer.writeByte(1) // flag 0x01
        }

        // inputs
        buffer.writeVarInt(transaction.inputs.size.toLong())
        transaction.inputs.forEach { buffer.write(InputSerializer.serialize(it)) }

        // outputs
        buffer.writeVarInt(transaction.outputs.size.toLong())
        transaction.outputs.forEach { buffer.write(OutputSerializer.serialize(it)) }

        //  serialize witness data
        if (header.segwit && withWitness) {
            transaction.inputs.forEach { buffer.write(InputSerializer.serializeWitness(it.witness)) }
        }

        buffer.writeUnsignedInt(header.m_nSrcChain)
        buffer.writeUnsignedInt(header.m_nDestChain)
        buffer.writeUnsignedInt(header.lockTime)
        return buffer.toByteArray()
    }

    fun serializeForSignature(transaction: Transaction, inputsToSign: List<InputToSign>, outputs: List<TransactionOutput>, inputIndex: Int): ByteArray {
        val buffer = BitcoinOutput().writeInt(transaction.version)
        
            // inputs
            buffer.writeVarInt(inputsToSign.size.toLong())
            inputsToSign.forEachIndexed { index, input ->
                buffer.write(InputSerializer.serializeForSignature(input, index == inputIndex))
            }

            // outputs
            buffer.writeVarInt(outputs.size.toLong())
            outputs.forEach { buffer.write(OutputSerializer.serialize(it)) }
        

        buffer.writeUnsignedInt(transaction.m_nSrcChain)
        buffer.writeUnsignedInt(transaction.m_nDestChain)
        buffer.writeUnsignedInt(transaction.lockTime)
        return buffer.toByteArray()
    }
}
