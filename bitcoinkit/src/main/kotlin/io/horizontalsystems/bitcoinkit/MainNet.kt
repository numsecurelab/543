package io.horizontalsystems.bitcoinkit

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils

class MainNet : Network() {
    override val protocolVersion = 80000
    override var port: Int = 20445

    override var magic: Long = 0x34feafaa
    override var bip32HeaderPub: Int = 0x043410d2   // The 4 byte header that serializes in base58 to "xpub".
    override var bip32HeaderPriv: Int = 0x0434113b  // The 4 byte header that serializes in base58 to "xprv"
    override var addressVersion: Int = 127
    override var addressSegwitHrp: String = "tk"
    override var addressScriptVersion: Int = 120
    override var coinType: Int = 292

    override val maxBlockSize = 1_000_000
    override val dustRelayTxFee = 3000 // https://github.com/bitcoin/bitcoin/blob/c536dfbcb00fb15963bf5d507b7017c241718bf6/src/policy/policy.h#L50

    override var dnsSeeds = listOf(
            "151.248.122.158",
            "37.193.170.31",
            "195.133.73.60",
            "194.67.90.128",
            "128.70.156.181",
            "46.147.88.238",
            "109.248.249.136",
            "46.219.12.4",
            "5.206.55.64",
            "91.76.178.35",
            "94.231.145.96",
            "84.201.159.243",
            "78.24.223.211",
            "185.195.27.171",
            "46.229.133.110",
            "193.34.161.123",
            "176.99.3.9",
            "78.24.219.248",
            "165.22.68.9",
            "87.224.148.105",
            "84.201.157.212",
            "95.30.235.70",
            "84.201.129.229",
            "87.248.242.214",
            "168.62.164.145",
            "104.248.138.190",
            "136.243.68.121",
            "178.62.86.220",
            "84.201.158.226",
            "84.201.132.54",
            "37.230.114.18",
            "195.133.74.60",
            "46.146.101.164",
            "37.193.170.31",
            "195.133.73.60",
            "136.243.68.123",
            "128.70.156.181",
            "46.147.88.238",
            "109.248.249.136",
            "46.219.12.4",
            "5.206.55.64",
            "178.62.84.12",
            "84.201.158.130",
            "128.74.177.222",
            "92.249.120.33",
            "84.201.129.67",
            "195.3.143.6",
            "188.32.160.130",
            "109.172.104.29",
            "94.231.145.96",
            "178.128.117.45",
            "79.172.23.13"
    )

    override val bip44CheckpointBlock = Block(BlockHeader(
            version = 541065216,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("000000000019b7c3af53085ee4a38521693e9414c950775c1af5e10f8d81a839"),
            merkleRoot = HashUtils.toBytesAsLE("8c3808e941a250f9f261a8eba1be706293e761ba72292c26f6a79dabb34fef7b"),
            timestamp = 1578912054,
            bits = 455785186,
            nonce = 60916029,
            m_chain_number = 0,
            hash = HashUtils.toBytesAsLE("00000000001fc7fd20d3db7a9769b88839b5e4aebebb61a82b0dc06362007e8d")
    ), 56448)

}
