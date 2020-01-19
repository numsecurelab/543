package io.horizontalsystems.bitcoinkit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCore.SyncMode
import io.horizontalsystems.bitcoincore.BitcoinCoreBuilder
import io.horizontalsystems.bitcoincore.blocks.BlockMedianTimeHelper
import io.horizontalsystems.bitcoincore.blocks.validators.BitsValidator
import io.horizontalsystems.bitcoincore.blocks.validators.LegacyDifficultyAdjustmentValidator
import io.horizontalsystems.bitcoincore.blocks.validators.LegacyTestNetDifficultyValidator
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoincore.managers.*
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.CoreDatabase
import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.PaymentAddressParser
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.hodler.HodlerPlugin
import io.reactivex.Single

class BitcoinKit : AbstractKit {
    enum class NetworkType {
        MainNet,
        TestNet,
        RegTest
    }

    interface Listener : BitcoinCore.Listener

    override var bitcoinCore: BitcoinCore
    override var network: Network

    var listener: Listener? = null
        set(value) {
            field = value
            bitcoinCore.listener = value
        }

    constructor(
            context: Context,
            words: List<String>,
            walletId: String,
            networkType: NetworkType = NetworkType.MainNet,
            peerSize: Int = 10,
            syncMode: SyncMode = SyncMode.Api(),
            confirmationsThreshold: Int = 6,
            bip: Bip = Bip.BIP44
    ) : this(context, Mnemonic().toSeed(words), walletId, networkType, peerSize, syncMode, confirmationsThreshold, bip)

    constructor(
            context: Context,
            seed: ByteArray,
            walletId: String,
            networkType: NetworkType = NetworkType.MainNet,
            peerSize: Int = 10,
            syncMode: SyncMode = SyncMode.Api(),
            confirmationsThreshold: Int = 6,
            bip: Bip = Bip.BIP44
    ) {
        val database = CoreDatabase.getInstance(context, getDatabaseName(networkType, walletId, syncMode, bip))
        val storage = Storage(database)
        var initialSyncUrl = ""

        network = when (networkType) {
            NetworkType.MainNet -> {
                initialSyncUrl = "https://3301.pro"
                MainNet()
            }
            NetworkType.TestNet -> {
                initialSyncUrl = "http://3301.pro"
                TestNet()
            }
            NetworkType.RegTest -> RegTest()
        }

        val paymentAddressParser = PaymentAddressParser("tkeycoin", removeScheme = true)
        val initialSyncApi = BCoinApi(initialSyncUrl)

        val coreBuilder = BitcoinCoreBuilder()

        bitcoinCore = coreBuilder
                .setContext(context)
                .setSeed(seed)
                .setNetwork(network)
                .setBip(bip)
                .setPaymentAddressParser(paymentAddressParser)
                .setPeerSize(peerSize)
                .setSyncMode(syncMode)
                .setConfirmationThreshold(confirmationsThreshold)
                .setStorage(storage)
                .setInitialSyncApi(initialSyncApi)
                .addPlugin(HodlerPlugin(coreBuilder.addressConverter, storage, BlockMedianTimeHelper(storage)))
                .build()

        //  extending bitcoinCore

        val bech32AddressConverter = SegwitAddressConverter(network.addressSegwitHrp)
        val base58AddressConverter = Base58AddressConverter(network.addressVersion, network.addressScriptVersion)

        bitcoinCore.prependAddressConverter(bech32AddressConverter)

        val blockHelper = BlockValidatorHelper(storage)

        if (networkType == NetworkType.MainNet) {
            bitcoinCore.addBlockValidator(LegacyDifficultyAdjustmentValidator(blockHelper, BitcoinCore.heightInterval, BitcoinCore.targetTimespan, BitcoinCore.maxTargetBits))
            bitcoinCore.addBlockValidator(BitsValidator())
        } else if (networkType == NetworkType.TestNet) {
            bitcoinCore.addBlockValidator(LegacyDifficultyAdjustmentValidator(blockHelper, BitcoinCore.heightInterval, BitcoinCore.targetTimespan, BitcoinCore.maxTargetBits))
            bitcoinCore.addBlockValidator(LegacyTestNetDifficultyValidator(storage, BitcoinCore.heightInterval, BitcoinCore.targetSpacing, BitcoinCore.maxTargetBits))
            bitcoinCore.addBlockValidator(BitsValidator())
        }

        when (bip) {
            Bip.BIP44 -> {
                bitcoinCore.addRestoreKeyConverter(Bip44RestoreKeyConverter(base58AddressConverter))
                bitcoinCore.addRestoreKeyConverter(Bip49RestoreKeyConverter(base58AddressConverter))
                bitcoinCore.addRestoreKeyConverter(Bip84RestoreKeyConverter(bech32AddressConverter))
            }
            Bip.BIP49 -> {
                bitcoinCore.addRestoreKeyConverter(Bip49RestoreKeyConverter(base58AddressConverter))
            }
            Bip.BIP84 -> {
                bitcoinCore.addRestoreKeyConverter(Bip84RestoreKeyConverter(bech32AddressConverter))
            }
        }
    }

    fun transactions(fromUid: String? = null, limit: Int? = null): Single<List<TransactionInfo>> {
        return bitcoinCore.transactions(fromUid, limit)
    }

    companion object {

        private fun getDatabaseName(networkType: NetworkType, walletId: String, syncMode: SyncMode, bip: Bip): String = "Bitcoin-${networkType.name}-$walletId-${syncMode.javaClass.simpleName}-${bip.name}"

        fun clear(context: Context, networkType: NetworkType, walletId: String) {
            for (syncMode in listOf(SyncMode.Api(), SyncMode.Full(), SyncMode.NewWallet())) {
                for (bip in Bip.values())
                    try {
                        SQLiteDatabase.deleteDatabase(context.getDatabasePath(getDatabaseName(networkType, walletId, syncMode, bip)))
                    } catch (ex: Exception) {
                        continue
                    }
            }
        }
    }

}
