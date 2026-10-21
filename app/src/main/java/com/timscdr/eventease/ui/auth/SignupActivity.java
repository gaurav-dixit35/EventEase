package com.timscdr.eventease.ui.auth;

import android.os.Bundle;
import com.timscdr.eventease.R;
import com.timscdr.eventease.util.ScreenNavigation;

public final class SignupActivity extends AuthFormActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindForm(R.layout.activity_signup);
        if (savedInstanceState == null) {
            ((com.google.android.material.textfield.TextInputEditText) findViewById(R.id.college))
                    .setText(R.string.college_default);
        }
        findViewById(R.id.submit).setOnClickListener(view -> model.signUp(input(R.id.name), input(R.id.roll),
                input(R.id.course), input(R.id.year), input(R.id.email), input(R.id.college)));
        findViewById(R.id.alternate).setOnClickListener(view -> ScreenNavigation.login(this));
    }
}
