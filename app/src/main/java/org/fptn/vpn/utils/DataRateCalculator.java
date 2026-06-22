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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DataRateCalculator {
    private final long intervalMillis;
    private long bytes;
    private long lastUpdateTime;
    private long rate;
    private final Lock lock = new ReentrantLock();

    public DataRateCalculator(long intervalMillis) {
        this.lastUpdateTime = System.currentTimeMillis();
        this.intervalMillis = intervalMillis;
        this.bytes = 0;
        this.rate = 0;
    }

    public void update(long len) {
        lock.lock();
        try {
            long now = System.currentTimeMillis();
            long elapsed = now - lastUpdateTime;

            bytes += len;
            if (elapsed >= intervalMillis) {
                rate = bytes / (elapsed / 1000);
                lastUpdateTime = now;
                bytes = 0;
            }
        } finally {
            lock.unlock();
        }
    }

    public long getRateForSecond() {
        lock.lock();
        try {
            long elapsed = System.currentTimeMillis() - lastUpdateTime;
            if (elapsed >= intervalMillis) {
                return 0;
            }
            if (intervalMillis > 0) {
                return rate / (1000 / intervalMillis);
            }
            return 0;
        } finally {
            lock.unlock();
        }
    }

    public String getFormatString() {
        double bitsPerSec = getRateForSecond() * 8.0;
        String speedStr;
        if (bitsPerSec >= 1e9) {
            speedStr = String.format("%.2f Gbps", bitsPerSec / 1e9);
        } else if (bitsPerSec >= 1e6) {
            speedStr = String.format("%.2f Mbps", bitsPerSec / 1e6);
        } else {
            speedStr = String.format("%.2f Kbps", bitsPerSec / 1e3);
        }
        return speedStr;
    }
}
