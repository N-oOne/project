package com.example.music_chenqianyu;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_AGREEMENT_ACCEPTED = "agreement_accepted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查是否已同意协议
        if (!isAgreementAccepted()) {
            new Handler().postDelayed(this::showDeclarationDialog, 1000);
        } else {
            navigateToHome();
        }
    }

    private boolean isAgreementAccepted() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_AGREEMENT_ACCEPTED, false);
    }

    private void setAgreementAccepted(boolean accepted) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_AGREEMENT_ACCEPTED, accepted);
        editor.apply();
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void showDeclarationDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_declaration, null);
        TextView contentTextView = dialogView.findViewById(R.id.contentTextView);

        String contentText = getString(R.string.declaration_content);
        SpannableString spannableString = new SpannableString(contentText);

        // 用户协议点击
        int userAgreementStart = contentText.indexOf("《用户协议》");
        int userAgreementEnd = userAgreementStart + "《用户协议》".length();
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                openUrl("https://www.mi.com");
            }
        }, userAgreementStart, userAgreementEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 隐私政策点击
        int privacyPolicyStart = contentText.indexOf("《隐私政策》");
        int privacyPolicyEnd = privacyPolicyStart + "《隐私政策》".length();
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                openUrl("https://www.xiaomiev.com/");
            }
        }, privacyPolicyStart, privacyPolicyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        contentTextView.setText(spannableString);
        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialogView.findViewById(R.id.disagreeButton).setOnClickListener(v -> {
            dialog.dismiss();
            new Handler(Looper.getMainLooper()).postDelayed(() -> finishAffinity(), 300);
        });

        dialogView.findViewById(R.id.agreeButton).setOnClickListener(v -> {
            setAgreementAccepted(true); // 保存同意状态
            navigateToHome();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}