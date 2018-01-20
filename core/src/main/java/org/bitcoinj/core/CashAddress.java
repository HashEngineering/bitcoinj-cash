package org.bitcoinj.core;

import com.subgraph.orchid.data.exitpolicy.Network;
import javafx.util.Pair;
import org.bitcoinj.params.Networks;
import org.bitcoinj.script.Script;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.bitcoinj.core.CashAddressHelper.ConvertBits;

/**
 * Created by Hash Engineering on 1/19/2018.
 */
public class CashAddress extends Address {

    public enum CashAddressType
    {
        PubKey (0),
        Script(1);

        private int value;
        CashAddressType(int value) {
            this.value = value;
        }
        byte getValue() { return (byte)value;}
    }

    CashAddressType type;
    //byte [] hash;
    //NetworkParameters params;


    CashAddress(NetworkParameters params, CashAddressType type, byte [] hash)
    {
        super(params, getLegacyVersion(params, type), hash);
        this.type = type;
    }

    /*
     *
     * @param params
     * @param version
     * @param hash160
     */
    CashAddress(NetworkParameters params, int version, byte [] hash160)
    {
        super(params, version, hash160);
        this.type = getType(params, version);
    }


    /** Returns an Address that represents the given P2SH script hash. */
    public static CashAddress fromP2SHHash(NetworkParameters params, byte[] hash160) {
        try {
            return new CashAddress(params, CashAddressType.Script, hash160);
        } catch (WrongNetworkException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    /** Returns an Address that represents the script hash extracted from the given scriptPubKey */
    public static CashAddress fromP2SHScript(NetworkParameters params, Script scriptPubKey) {
        checkArgument(scriptPubKey.isPayToScriptHash(), "Not a P2SH script");
        return fromP2SHHash(params, scriptPubKey.getPubKeyHash());
    }
    /**
     * Construct an address from its Base58 representation.
     * @param params
     *            The expected NetworkParameters or null if you don't want validation.
     * @param base58
     *            The textual form of the address, such as "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL".
     * @throws AddressFormatException
     *             if the given base58 doesn't parse or the checksum is invalid
     * @throws WrongNetworkException
     *             if the given address is valid but for a different chain (eg testnet vs mainnet)
     */
    public static CashAddress fromBase58(@Nullable NetworkParameters params, String base58) throws AddressFormatException {
        VersionedChecksummedBytes parsed = new VersionedChecksummedBytes(base58);
        NetworkParameters addressParams;
        if (params != null) {
            if (!isAcceptableVersion(params, parsed.version)) {
                throw new WrongNetworkException(parsed.version, params.getAcceptableAddressCodes());
            }
            addressParams = params;
        } else {
            NetworkParameters paramsFound = null;
            for (NetworkParameters p : Networks.get()) {
                if (isAcceptableVersion(p, parsed.version)) {
                    paramsFound = p;
                    break;
                }
            }
            if (paramsFound == null)
                throw new AddressFormatException("No network found for " + base58);

            addressParams = paramsFound;
        }
        return new CashAddress(addressParams, parsed.version, parsed.bytes);
    }


//    CTxDestination DecodeCashAddr(const std::string &addr,
 //                             const CChainParams &params);

    public static CashAddress fromCashAddrContent(NetworkParameters params, String addr)
            throws AddressFormatException {

        Pair<String, byte[]> pair = CashAddressHelper.decode(addr, params.getCashAddrPrefix());
        String prefix = pair.getKey();
        byte[] payload = pair.getValue();

        if (!prefix.equals(params.getCashAddrPrefix())) {
            throw new AddressFormatException("Invalid prefix for network: " + prefix + " != " + params.getCashAddrPrefix() + " (expected)");
        }

        if (payload.length == 0) {
            throw new AddressFormatException("No payload");
        }

        // Check that the padding is zero.
        byte extrabits = (byte) (payload.length * 5 % 8);
        if (extrabits >= 5) {
            // We have more padding than allowed.
            throw new AddressFormatException("More than allowed padding");
        }

        byte last = payload[payload.length - 1];
        byte mask = (byte) ((1 << extrabits) - 1);
        if ((last & mask) != 0) {
            // We have non zero bits as padding.
            throw new AddressFormatException("Nonzero bytes ");
        }

        byte[] data = new byte[payload.length * 5 / 8];
        ConvertBits(data, payload, 5, 8, false);

        // Decode type and size from the version.
        byte version = data[0];
        if ((version & 0x80) != 0) {
            // First bit is reserved.
            throw new AddressFormatException("First bit is reserved");
        }

        CashAddressType type = CashAddressType.PubKey;
        switch (version >> 3 & 0x1f)
        {
            case 0: type = CashAddressType.PubKey; break;
            case 1: type = CashAddressType.Script; break;
            default:
                throw new AddressFormatException("Unknown Type");
        }

        int hash_size = 20 + 4 * (version & 0x03);
        if ((version & 0x04) != 0) {
            hash_size *= 2;
        }

        // Check that we decoded the exact number of bytes we expected.
        if (data.length != hash_size + 1) {
            throw new AddressFormatException("Data length " + data.length + " != hash size " + hash_size);
        }

        // Pop the version.
        byte result [] = new byte [data.length -1];
        System.arraycopy(data, 1, result, 0, data.length-1);
        return new CashAddress(params, type, result);
    }
    //CashAddrContent DecodeCashAddrContent(const std::string &addr,
    //                                  const CChainParams &params);
    //CTxDestination DecodeCashAddrDestination(const CashAddrContent &content);

    //std::vector<uint8_t> PackCashAddrContent(const CashAddrContent &content);


    /**
     * Returns true if this address is a Pay-To-Script-Hash (P2SH) address.
     * See also https://github.com/bitcoin/bips/blob/master/bip-0013.mediawiki: Address Format for pay-to-script-hash
     */
    public boolean isP2SHAddress() {
        return type == CashAddressType.Script;
    }

    public String toString()
    {
        return CashAddressHelper.encode(getParameters().getCashAddrPrefix(), CashAddressHelper.packAddrData(getHash160(), type.getValue()));
    }

    static protected int getLegacyVersion(NetworkParameters params, CashAddressType type)
    {
        switch(type) {
            case PubKey: return params.getAddressHeader();
            case Script: return params.getP2SHHeader();
        }
        throw new AddressFormatException("cashaddr:  Invalid type: " + type.value);
    }

    static CashAddressType getType(NetworkParameters params, int version)
    {
        if(version == params.getAddressHeader())
        {
            return CashAddressType.PubKey;
        }
        else if(version == params.getP2SHHeader())
        {
            return CashAddressType.Script;
        }
        throw new AddressFormatException("cashaddr:  Invalid version: " + version);
    }

    /**
     * This implementation narrows the return type to <code>Address</code>.
     */
    @Override
    public Address clone() throws CloneNotSupportedException {
        return (CashAddress) super.clone();
    }
}
