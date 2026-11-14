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

package org.fptn.vpn.network;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Glob-style domain pattern with '*' matching any characters WITHIN a single
 * DNS label (it does not cross the '.' boundary). Used in advanced blacklist
 * and split-tunnel domain matching alongside exact HashSet lookups.
 *
 * Semantics:
 *   "kaspersky*server.com"  matches "kaspersky1server.com", "kasperskyABCserver.com"
 *                           does NOT match "kaspersky.server.com" (star cannot cross dot)
 *                           does NOT match "kaspersky1server.ru"   (TLD label mismatch)
 *   "*-ads.example.com"     matches "doubleclick-ads.example.com"
 *   "kaspersky*.com"        matches "kaspersky1.com", "kasperskyXYZ.com"
 *
 * Suffix matching: a pattern matches any domain whose trailing labels match,
 * so "kaspersky*server.com" also matches "update.kaspersky1server.com".
 *
 * Patterns are stored post-IDN.toASCII and lowercased, so Unicode labels are
 * already Punycode (xn--...). The caller strips '*' before IDN conversion and
 * reinserts it into the converted pattern (see DomainWildcard.idnWithWildcards).
 */
public final class DomainWildcard {

    private final String original;
    private final Pattern[] labelPatterns;  // compiled regex per label; null = exact label
    private final String[] exactLabels;     // exact label value when labelPatterns[i] == null
    private final int labelCount;

    private DomainWildcard(String original, Pattern[] labelPatterns, String[] exactLabels) {
        this.original = original;
        this.labelPatterns = labelPatterns;
        this.exactLabels = exactLabels;
        this.labelCount = labelPatterns.length;
    }

    /**
     * Compile a glob domain pattern into a DomainWildcard.
     * Pattern must already be lowercased and IDN-converted (use idnWithWildcards
     * to preserve '*' across Punycode conversion). Returns null if the pattern
     * contains no '*' (caller should use the exact HashSet instead) or is invalid.
     */
    public static DomainWildcard compile(String pattern) {
        if (pattern == null || pattern.isEmpty() || !pattern.contains("*")) {
            return null;
        }
        String[] labels = pattern.split("\\.");
        if (labels.length == 0) {
            return null;
        }
        Pattern[] labelPatterns = new Pattern[labels.length];
        String[] exactLabels = new String[labels.length];
        boolean hasAnyWildcard = false;
        for (int i = 0; i < labels.length; i++) {
            String label = labels[i];
            if (label.contains("*")) {
                hasAnyWildcard = true;
                try {
                    labelPatterns[i] = Pattern.compile(buildLabelRegex(label));
                } catch (PatternSyntaxException e) {
                    return null;
                }
                exactLabels[i] = null;
            } else {
                labelPatterns[i] = null;
                exactLabels[i] = label;
            }
        }
        if (!hasAnyWildcard) {
            return null;
        }
        return new DomainWildcard(pattern, labelPatterns, exactLabels);
    }

    /**
     * Convert a glob label to a regex. '*' becomes '[^.]*' (any chars except dot,
     * so the star stays within one DNS label). All other regex metacharacters are
     * escaped so the pattern is treated literally except for '*'.
     */
    private static String buildLabelRegex(String label) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c == '*') {
                sb.append("[^.]*");
            } else if ("\\.[]{}()*+-?^$|".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Check whether domain matches this pattern. Domain must be lowercased and
     * Punycode (as delivered by DNS). Supports suffix matching: the domain may
     * have more labels than the pattern (e.g. pattern "kaspersky*server.com"
     * matches "update.kaspersky1server.com").
     */
    public boolean matches(String domain) {
        if (domain == null || domain.isEmpty()) {
            return false;
        }
        String[] domainLabels = domain.split("\\.");
        if (domainLabels.length < labelCount) {
            return false;
        }
        int offset = domainLabels.length - labelCount;
        for (int i = 0; i < labelCount; i++) {
            String domainLabel = domainLabels[offset + i];
            if (labelPatterns[i] == null) {
                if (!domainLabel.equals(exactLabels[i])) {
                    return false;
                }
            } else if (!labelPatterns[i].matcher(domainLabel).matches()) {
                return false;
            }
        }
        return true;
    }

    public String original() {
        return original;
    }

    /** True if any pattern in the list matches the domain. */
    public static boolean matchesAny(List<DomainWildcard> patterns, String domain) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (DomainWildcard p : patterns) {
            if (p.matches(domain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convert Unicode to Punycode while preserving '*' wildcards. Splits the
     * input on '*', IDN-converts each segment, and rejoins with '*'. Use this
     * instead of IDN.toASCII when the domain may contain wildcards.
     *
     *   "kaspersky*server.com"            -> "kaspersky*server.com"          (ASCII unchanged)
     *   "касперский*сервер.рф"            -> "xn--...*xn--....xn--p1ai"      (each segment Punycode'd)
     */
    public static String idnWithWildcards(String domain) {
        if (domain == null || domain.isEmpty()) {
            return domain;
        }
        if (!domain.contains("*")) {
            return java.net.IDN.toASCII(domain, java.net.IDN.ALLOW_UNASSIGNED);
        }
        String[] parts = domain.split("\\*", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append("*");
            }
            if (!parts[i].isEmpty()) {
                sb.append(java.net.IDN.toASCII(parts[i], java.net.IDN.ALLOW_UNASSIGNED));
            }
        }
        return sb.toString();
    }
}