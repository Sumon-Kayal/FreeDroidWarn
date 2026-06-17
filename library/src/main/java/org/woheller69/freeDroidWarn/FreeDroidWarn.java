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

    public static void showWarningDialogOnUpgrade(Context context, int buildVersion){
        SharedPreferences prefManager = context.getSharedPreferences(context.getPackageName() + PREF_NAME, Context.MODE_PRIVATE);
        int versionCode = prefManager.getInt(KEY_VERSION, 0);

        if (buildVersion > versionCode){
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

            // Highlight the solution button using colorError
            Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            if (neutralButton != null) {
                TypedValue tv = new TypedValue();
                if (context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorError, tv, true)) {
                    neutralButton.setTextColor(tv.resourceId != 0 ? ContextCompat.getColor(context, tv.resourceId) : tv.data);
                }
            }
        }
    }

    public static void showWarningSnackBarOnUpgrade(Context context, View view, int buildVersion){
        SharedPreferences prefManager = context.getSharedPreferences(context.getPackageName() + PREF_NAME, Context.MODE_PRIVATE);
        int versionCode = prefManager.getInt(KEY_VERSION, 0);

        if (buildVersion > versionCode){
            Snackbar snackbar = Snackbar.make(view, R.string.dialog_Warning, Snackbar.LENGTH_INDEFINITE);
            View snackbarView = snackbar.getView();

            TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            if (textView != null) {
                textView.setMaxLines(10); // Avoid Integer.MAX_VALUE for better performance
                textView.setSingleLine(false);
            }

            snackbar.setAction(R.string.dialog_more_info, v -> {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://keepandroidopen.org")));
                SharedPreferences.Editor editor = prefManager.edit();
                editor.putInt(KEY_VERSION, buildVersion);
                editor.apply();
            });

            snackbar.setDuration(5000);
            snackbar.show();
        }
    }
}
