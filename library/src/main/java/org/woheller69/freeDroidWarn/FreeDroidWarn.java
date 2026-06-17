package org.woheller69.freeDroidWarn;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class FreeDroidWarn {

    private static final String PREF_NAME = "_preferences";
    private static final String KEY_VERSION = "versionCodeWarn";

    /**
     * Legacy Method Shim: EXACT signature as the original (OG) code.
     * This prevents breaking apps already using the old version of the library.
     */
    public static void showWarningOnUpgrade(Context context, int buildVersion) {
        showWarningDialogOnUpgrade(context, buildVersion);
    }

    /**
     * Modern Material Dialog implementation
     */
    public static void showWarningDialogOnUpgrade(Context context, int buildVersion) {
        SharedPreferences prefManager = getPrefs(context);
        int versionCode = prefManager.getInt(KEY_VERSION, 0);

        // DATA MIGRATION: Check legacy SharedPreferences from the OG version.
        // This ensures existing users who already clicked "OK" don't see the warning again.
        if (versionCode == 0) {
            @SuppressWarnings("deprecation")
            SharedPreferences legacyPrefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
            if (legacyPrefs.contains(KEY_VERSION)) {
                versionCode = legacyPrefs.getInt(KEY_VERSION, 0);
                // Migrate the value to the new private preference file
                prefManager.edit().putInt(KEY_VERSION, versionCode).apply();
            }
        }

        if (buildVersion > versionCode) {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(context);
            materialAlertDialogBuilder.setMessage(R.string.dialog_Warning);
            
            materialAlertDialogBuilder.setNegativeButton(context.getString(R.string.dialog_more_info), (dialog, which) -> 
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://keepandroidopen.org"))));
            
            materialAlertDialogBuilder.setPositiveButton(context.getString(android.R.string.ok), (dialog, which) -> {
                SharedPreferences.Editor editor = prefManager.edit();
                editor.putInt(KEY_VERSION, buildVersion);
                editor.apply();
            });

            materialAlertDialogBuilder.setNeutralButton(context.getString(R.string.solution), (dialog, which) -> 
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/woheller69/FreeDroidWarn?tab=readme-ov-file#solutions"))));

            AlertDialog alertDialog = materialAlertDialogBuilder.create();
            alertDialog.show();

            // Highlight the solution button using theme-aware colorError
            Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            if (neutralButton != null) {
                TypedValue tv = new TypedValue();
                if (context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorError, tv, true)) {
                    neutralButton.setTextColor(tv.resourceId != 0 ? ContextCompat.getColor(context, tv.resourceId) : tv.data);
                } else {
                    // Fallback for non-material themes or very old devices
                    neutralButton.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                }
            }
        }
    }

    /**
     * New Feature: Implementation of a SnackBar warning for a non-intrusive UI
     */
    public static void showWarningSnackBarOnUpgrade(Context context, View view, int buildVersion) {
        SharedPreferences prefManager = getPrefs(context);
        int versionCode = prefManager.getInt(KEY_VERSION, 0);

        if (buildVersion > versionCode) {
            Snackbar snackbar = Snackbar.make(view, R.string.dialog_Warning, Snackbar.LENGTH_INDEFINITE);
            View snackbarView = snackbar.getView();

            TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            if (textView != null) {
                textView.setMaxLines(10); 
                textView.setSingleLine(false);
            }

            snackbar.setAction(R.string.dialog_more_info, v -> {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://keepandroidopen.org")));
                SharedPreferences.Editor editor = prefManager.edit();
                editor.putInt(KEY_VERSION, buildVersion);
                editor.apply();
            });

            snackbar.setDuration(8000); // 8 seconds to ensure the user has time to read
            snackbar.show();
        }
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(context.getPackageName() + PREF_NAME, Context.MODE_PRIVATE);
    }
}
