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

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

public class ViewUtils {

    public static void linkifySubstring(TextView textView, String linkText, Runnable onClick) {
        CharSequence fullText = textView.getText();
        int linkStart = fullText.toString().indexOf(linkText);
        if (linkStart < 0) {
            return;
        }
        SpannableString spannable = new SpannableString(fullText);
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                onClick.run();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(0xB3FFFFFF); // white at 70% alpha, like other links on this screen
                ds.setUnderlineText(true);
            }
        }, linkStart, linkStart + linkText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannable);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static void hideView(View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    public static void showView(View view) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }
}
