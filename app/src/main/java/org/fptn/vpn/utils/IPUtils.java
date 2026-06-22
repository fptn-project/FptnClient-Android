/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Website: https://fptn.org
 */

package org.fptn.vpn.utils;

import java.util.List;
import java.util.Optional;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

public class IPUtils {
    public static void exclude(IPAddress rootSubnet, List<IPAddress> subnetsToExclude, List<IPAddress> afterExclude, int prefix) {
        Optional<IPAddress> any = subnetsToExclude.stream().filter(subnet -> subnet.equals(rootSubnet)).findAny();
        if (any.isPresent()) {
            // we reach minimum size target subnet
            //XLog.d(IPUtils.class.getSimpleName(), "rootSubnet: " + rootSubnet + " == any: " + any.get());
            return;
        }

        int newNetmaskBits = rootSubnet.getNetworkPrefixLength() + 1;
        if (newNetmaskBits > prefix) {
            //System.out.println("EXCEED NETMASK BITS COUNT");
            return;
        }

        IPAddress rootSubnetLower = rootSubnet.getLower();
        IPAddress subnetLeft = new IPAddressString(rootSubnetLower.toAddressString().getHostAddress() + "/" + newNetmaskBits).getAddress();
        //XLog.d(IPUtils.class.getSimpleName(), "SubnetLeft: " + subnetLeft + " start from: " + subnetLeft.getLower() + " to: " + subnetLeft.getUpper());
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
            //XLog.d(IPUtils.class.getSimpleName(), "SubnetRight: " + subnetRight + " start from: " + subnetRight.getLower() + " to: " + subnetRight.getUpper());
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
