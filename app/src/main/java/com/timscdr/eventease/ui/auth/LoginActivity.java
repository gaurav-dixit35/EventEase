package com.timscdr.eventease.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.ActivityOptionsCompat;
import com.timscdr.eventease.R;
import com.timscdr.eventease.ui.organizer.OrganizerLoginActivity;

public final class LoginActivity extends AuthFormActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindForm(R.layout.activity_login);
        findViewById(R.id.submit).setOnClickListener(view -> model.signIn(input(R.id.name), input(R.id.roll)));
        findViewById(R.id.alternate).setOnClickListener(view -> {
            startActivity(new Intent(this, SignupActivity.class), ActivityOptionsCompat.makeCustomAnimation(
                    this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle());
        });
        findViewById(R.id.organizer_login).setOnClickListener(view -> startActivity(
                new Intent(this, OrganizerLoginActivity.class), ActivityOptionsCompat.makeCustomAnimation(
                        this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()));
    }
}
