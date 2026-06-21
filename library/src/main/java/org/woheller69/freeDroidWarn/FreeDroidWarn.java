package org.woheller69.freeDroidWarn;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class FreeDroidWarn {

    private static final String PREF_NAME = "dedicated_preferences";
    private static final String KEY_VERSION = "versionCodeWarn";

    /**
     * Shows a warning dialog when the application is upgraded.
     *
     * @param context      an Activity context
     * @param buildVersion the new build version code
     */
    public static void showWarningOnUpgrade(Context context, int buildVersion) {
        showWarningDialogOnUpgrade(context, buildVersion);
    }

    /**
     * Shows a warning dialog if the app has been upgraded since the last acknowledgment.
     */
    public static void showWarningDialogOnUpgrade(Context context, int buildVersion) {
        if (!isValidActivityContext(context)) return;

        SharedPreferences prefManager = getPrefs(context);
        int versionCode = getStoredVersion(context, prefManager);

        if (buildVersion > versionCode) {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(context);
            materialAlertDialogBuilder.setMessage(R.string.dialog_Warning);
            materialAlertDialogBuilder.setCancelable(false); // Force explicit acknowledgment

            // Primary Action: OK
            materialAlertDialogBuilder.setPositiveButton(android.R.string.ok, (dialog, which) -> 
                    markAsAcknowledged(prefManager, buildVersion));

            // Secondary Action: More Info
            materialAlertDialogBuilder.setNeutralButton(R.string.dialog_more_info, (dialog, which) -> {
                markAsAcknowledged(prefManager, buildVersion);
                safeStartActivity(context, "https://keepandroidopen.org");
            });

            // Tertiary/Warning Action: Solution
            materialAlertDialogBuilder.setNegativeButton(R.string.solution, (dialog, which) -> {
                markAsAcknowledged(prefManager, buildVersion);
                safeStartActivity(context, "https://github.com/woheller69/FreeDroidWarn?tab=readme-ov-file#solutions");
            });

            AlertDialog alertDialog = materialAlertDialogBuilder.create();
            alertDialog.show();

            // Style the "Solution" button with Error color
            Button negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                TypedValue tv = new TypedValue();
                int color = context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorError, tv, true) 
                            ? (tv.resourceId != 0 ? ContextCompat.getColor(context, tv.resourceId) : tv.data)
                            : ContextCompat.getColor(context, android.R.color.holo_red_dark);
                negativeButton.setTextColor(color);
            }
        }
    }

    /**
     * Shows a warning snackbar if the build version exceeds the stored version.
     */
    public static void showWarningSnackBarOnUpgrade(Context context, View view, int buildVersion) {
        // Issue 3: Added lifecycle guard to prevent crashes on background/destroyed activities
        if (!isValidActivityContext(context)) return;

        SharedPreferences prefManager = getPrefs(context);
        int versionCode = getStoredVersion(context, prefManager);

        if (buildVersion > versionCode) {
            Snackbar snackbar = Snackbar.make(view, R.string.dialog_Warning, 8000);
            View snackbarView = snackbar.getView();

            TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            if (textView != null) {
                textView.setMaxLines(10); 
                textView.setSingleLine(false);
            }

            snackbar.setAction(R.string.dialog_more_info, v -> {
                markAsAcknowledged(prefManager, buildVersion);
                safeStartActivity(context, "https://keepandroidopen.org");
            });

            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    // Persist version regardless of how it was dismissed (except if action handled it)
                    if (event != DISMISS_EVENT_ACTION) {
                        markAsAcknowledged(prefManager, buildVersion);
                    }
                }
            });

            snackbar.show();
        }
    }

    private static boolean isValidActivityContext(Context context) {
        if (!(context instanceof Activity)) return false;
        Activity activity = (Activity) context;
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    private static void markAsAcknowledged(SharedPreferences prefs, int version) {
        prefs.edit().putInt(KEY_VERSION, version).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "." + PREF_NAME, Context.MODE_PRIVATE);
    }

    private static int getStoredVersion(Context context, SharedPreferences prefManager) {
        int versionCode = prefManager.getInt(KEY_VERSION, 0);
        // Issue 4: Migration only attempts if current prefs are empty
        if (versionCode == 0) {
            @SuppressWarnings("deprecation")
            SharedPreferences legacyPrefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
            if (legacyPrefs.contains(KEY_VERSION)) {
                versionCode = legacyPrefs.getInt(KEY_VERSION, 0);
                if (versionCode != 0) {
                    markAsAcknowledged(prefManager, versionCode);
                }
            }
        }
        return versionCode;
    }
    
    private static void safeStartActivity(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            // Ensure we don't crash if calling from a non-activity context (though guarded above)
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            // Issue 2: Caught SecurityException (common in Work Profiles/restricted envs)
        }
    }
                    }
