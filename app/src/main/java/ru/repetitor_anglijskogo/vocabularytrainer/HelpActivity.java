package ru.repetitor_anglijskogo.vocabularytrainer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class HelpActivity extends Activity {
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setContentView(R.layout.activity_help);
    }

    public void fnButtonCancel( View view ) {
        finish();
    }

}

