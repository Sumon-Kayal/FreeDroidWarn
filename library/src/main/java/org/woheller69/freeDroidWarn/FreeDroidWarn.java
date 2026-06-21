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

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class FreeDroidWarn {

    private static final String PREF_NAME = "dedicated_preferences";
    private static final String KEY_VERSION = "versionCodeWarn";

    /**
     * Shows a warning dialog when the application is upgraded to a new build version.
     * This method is provided for backward compatibility with earlier versions of the library.
     *
     * @param context the activity context
     * @param buildVersion the new build version code
     */
    public static void showWarningOnUpgrade(Context context, int buildVersion) {
        showWarningDialogOnUpgrade(context, buildVersion);
    }

    /**
     * Shows a warning dialog if the app has been upgraded since the last acknowledgment.
     *
     * Validates that the context is an Activity in a valid lifecycle state. Compares the
     * buildVersion with the last acknowledged version stored in app-scoped preferences,
     * performing a one-time migration from legacy default preferences if needed. If an
     * upgrade is detected, displays a Material alert dialog with "more info" and "solution"
     * buttons that launch their respective URLs, and an "OK" button that acknowledges the
     * warning. The solution button text is styled using the theme's colorError attribute,
     * with a fallback to holo_red_dark if the attribute cannot be resolved.
     *
     * @param context     an Activity context; the method returns immediately if the context
     *                    is not an Activity or if the Activity is finishing or destroyed
     * @param buildVersion the current app build version to compare against the last
     *                    acknowledged version
     */
    public static void showWarningDialogOnUpgrade(Context context, int buildVersion) {
        // Guard against non-Activity contexts or Activities in terminal lifecycle states
        if (!(context instanceof Activity)) {
            return;
        }
        Activity activity = (Activity) context;
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        SharedPreferences prefManager = getPrefs(context);
        int versionCode = getStoredVersion(context, prefManager);

        if (buildVersion > versionCode) {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(context);
            materialAlertDialogBuilder.setMessage(R.string.dialog_Warning);
            
            materialAlertDialogBuilder.setNegativeButton(context.getString(R.string.dialog_more_info), (dialog, which) -> {
                safeStartActivity(context, "https://keepandroidopen.org");
            });
            
            materialAlertDialogBuilder.setPositiveButton(context.getString(android.R.string.ok), (dialog, which) ->
                    prefManager.edit().putInt(KEY_VERSION, buildVersion).apply());

            materialAlertDialogBuilder.setNeutralButton(context.getString(R.string.solution), (dialog, which) -> {
                safeStartActivity(context, "https://github.com/woheller69/FreeDroidWarn?tab=readme-ov-file#solutions");
            });

            AlertDialog alertDialog = materialAlertDialogBuilder.create();
            alertDialog.show();

            // Highlight the solution button using theme-aware colorError
            Button neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
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
     * Shows a warning snackbar if the build version exceeds the stored version.
     *
     * <p>The snackbar displays for 8 seconds and includes a "more info" action that opens a reference
     * URL and persists the build version to preferences. The build version is also persisted when the
     * snackbar is dismissed by timeout or swipe, ensuring it does not reappear on subsequent launches.
     *
     * @param context     the application context
     * @param view        the view to anchor the snackbar to
     * @param buildVersion the current build version to compare against the stored version
     */
    public static void showWarningSnackBarOnUpgrade(Context context, View view, int buildVersion) {
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
                safeStartActivity(context, "https://keepandroidopen.org");
                prefManager.edit().putInt(KEY_VERSION, buildVersion).apply();
            });

            // Save the version on timeout or swipe-dismiss too, so the snackbar
            // doesn't reappear every launch for users who saw it but didn't tap the action.
            snackbar.addCallback(new Snackbar.Callback() {
                /**
                 * Writes the build version to preferences when the snackbar is dismissed without tapping the action button.
                 */
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    if (event != DISMISS_EVENT_ACTION) {
                        prefManager.edit().putInt(KEY_VERSION, buildVersion).apply();
                    }
                }
            });

            snackbar.show();
        }
    }

    /**
     * Retrieves the app's dedicated SharedPreferences file for storing the warning state.
     *
     * @param  context the application context
     * @return         the app's private SharedPreferences instance
     */
    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(context.getPackageName() + PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
      * Retrieves the stored version code from preferences, migrating from deprecated storage if necessary.
      *
      * On first retrieval (when the stored value is zero), this method checks the legacy default
      * preferences for a previously stored version code and copies it to the current preferences.
      *
      * @return the stored version code, or zero if none has been recorded
      */
    private static int getStoredVersion(Context context, SharedPreferences prefManager) {
        int versionCode = prefManager.getInt(KEY_VERSION, 0);
        if (versionCode == 0) {
            @SuppressWarnings("deprecation")
            SharedPreferences legacyPrefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
            if (legacyPrefs.contains(KEY_VERSION)) {
                versionCode = legacyPrefs.getInt(KEY_VERSION, 0);
                prefManager.edit().putInt(KEY_VERSION, versionCode).apply();
            }
        }
        return versionCode;
    }
    
    /**
     * Opens the specified URL in an external activity.
     *
     * @param context the context to start the activity from
     * @param url the URL to open
     */
    private static void safeStartActivity(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No browser or app available to handle the intent
            // Silently fail to avoid crashing the app
        }
    }
}
