package org.fptn.vpn.utils;

import android.util.Log;

import java.util.List;
import java.util.Optional;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

public class IPUtils {
    public static void exclude(IPAddress rootSubnet, List<IPAddress> subnetsToExclude, List<IPAddress> afterExclude, int prefix) {
        Optional<IPAddress> any = subnetsToExclude.stream().filter(subnet -> subnet.equals(rootSubnet)).findAny();
        if (any.isPresent()) {
            // we reach minimum size target subnet
            //Log.d(IPUtils.class.getSimpleName(), "rootSubnet: " + rootSubnet + " == any: " + any.get());
            return;
        }

        int newNetmaskBits = rootSubnet.getNetworkPrefixLength() + 1;
        if (newNetmaskBits > prefix) {
            //System.out.println("EXCEED NETMASK BITS COUNT");
            return;
        }

        IPAddress rootSubnetLower = rootSubnet.getLower();
        IPAddress subnetLeft = new IPAddressString(rootSubnetLower.toAddressString().getHostAddress() + "/" + newNetmaskBits).getAddress();
        //Log.d(IPUtils.class.getSimpleName(), "SubnetLeft: " + subnetLeft + " start from: " + subnetLeft.getLower() + " to: " + subnetLeft.getUpper());
        Optional<IPAddress> checkLeft = subnetsToExclude.stream().filter(subnetLeft::contains).findFirst();
        if (checkLeft.isPresent()) {
            exclude(subnetLeft, subnetsToExclude, afterExclude, prefix);
        } else {
            afterExclude.add(subnetLeft);
        }

        IPAddress[] subtract = rootSubnet.subtract(subnetLeft);
        //System.out.println("subtract: " + subtract);
        if (subtract != null && subtract.length > 0) {
            IPAddress subnetRight = subtract[0];
            //Log.d(IPUtils.class.getSimpleName(), "SubnetRight: " + subnetRight + " start from: " + subnetRight.getLower() + " to: " + subnetRight.getUpper());
            Optional<IPAddress> checkRight = subnetsToExclude.stream()
                    .filter(subnetRight::contains)
                    .findFirst();
            if (checkRight.isPresent()) {
                exclude(subnetRight, subnetsToExclude, afterExclude, prefix);
            } else {
                afterExclude.add(subnetRight);
            }
        }
    }

}
