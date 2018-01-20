package org.bitcoinj.core;

import org.bitcoinj.params.MainNetParams;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by Hash Engineering Soltuions on 1/19/2018.
 */
public class CashAddressTest {
    @Test
    public void cashAddressCompareToLegacy() {
        String[][] map = {
                {"1BpEi6DfDAUFd7GtittLSdBeYJvcoaVggu", "bitcoincash:qpm2qsznhks23z7629mms6s4cwef74vcwvy22gdx6a"},
                {"1KXrWXciRDZUpQwQmuM1DbwsKDLYAYsVLR", "bitcoincash:qr95sy3j9xwd2ap32xkykttr4cvcu7as4y0qverfuy"},
                {"16w1D5WRVKJuZUsSRzdLp9w3YGcgoxDXb", "bitcoincash:qqq3728yw0y47sqn6l2na30mcw6zm78dzqre909m2r"},
                {"3CWFddi6m4ndiGyKqzYvsFYagqDLPVMTzC", "bitcoincash:ppm2qsznhks23z7629mms6s4cwef74vcwvn0h829pq"},
                {"3LDsS579y7sruadqu11beEJoTjdFiFCdX4", "bitcoincash:pr95sy3j9xwd2ap32xkykttr4cvcu7as4yc93ky28e"},
                {"31nwvkZwyPdgzjBJZXfDmSWsC4ZLKpYyUw", "bitcoincash:pqq3728yw0y47sqn6l2na30mcw6zm78dzq5ucqzc37"},
        };

        NetworkParameters params = MainNetParams.get();
        for (int i = 0; i < 6; ++i) {
            Address legacy = Address.fromBase58(params, map[i][0]);

            CashAddress cashaddr = CashAddress.fromCashAddrContent(params, map[i][1]);

            assertArrayEquals(legacy.getHash160(), cashaddr.getHash160());
            assertEquals(legacy.isP2SHAddress(), cashaddr.isP2SHAddress());

            assertEquals(cashaddr.toBase58(), map[i][0]);

            assertEquals(cashaddr.toString(), map[i][1]);
        }
    }

    @Test
    public void cashAddressIsValid() {
        NetworkParameters params = MainNetParams.get();
        CashAddress cashAddress = CashAddress.fromCashAddrContent(params, "bitcoincash:qpm2qsznhks23z7629mms6s4cwef74vcwvy22gdx6a");
        assertEquals(cashAddress.isP2SHAddress(), false);
        assertEquals(cashAddress.toString(), "bitcoincash:qpm2qsznhks23z7629mms6s4cwef74vcwvy22gdx6a");
        assertNotEquals(cashAddress.toString(), "bitcoincash:qqq3728yw0y47sqn6l2na30mcw6zm78dzqre909m2r");
        try {
            cashAddress = CashAddress.fromCashAddrContent(params,"bitcoinCash:Qpm2qsznhks23z7629mms6s4cwef74vcwvy22gdx6a");
            fail();
        }
        catch(AddressFormatException x)
        {
            //expected
        }
    }
}
