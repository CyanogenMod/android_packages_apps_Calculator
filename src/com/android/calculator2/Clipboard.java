package com.android.calculator2;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

/**
 * Simplify Android copy/paste
 */
public class Clipboard {
    public static void copy(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
        String toastText = String.format(context.getResources().getString(R.string.text_copied_toast), text);
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show();
    }

    public static String paste(Context context) {
        ClipData clip = getPrimaryClip(context);
        if(clip != null) {
            for(int i = 0; i < clip.getItemCount(); i++) {
                CharSequence paste = clip.getItemAt(i).coerceToText(context);
                if(paste.length() > 0) {
                    return paste.toString();
                }
            }
        }
        return null;
    }

    public static boolean canPaste(Context context) {
        return paste(context) != null;
    }


    private static ClipData getPrimaryClip(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        return clipboard.getPrimaryClip();
    }

    private static void setPrimaryClip(Context context, ClipData clip) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(clip);
    }
}
