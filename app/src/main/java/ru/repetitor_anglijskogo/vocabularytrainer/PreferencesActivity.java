package ru.repetitor_anglijskogo.vocabularytrainer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class PreferencesActivity extends Activity {

    static CheckBox checkbox_ResetRating=null;

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );
        setContentView( R.layout.activity_preferences );

        checkbox_ResetRating = (CheckBox)findViewById(R.id.checkboxResetRating);
        checkbox_ResetRating.setChecked( false );
    }

    public void fnButtonSave( View view ) {

        Intent intentAnswer = new Intent();

        if( checkbox_ResetRating != null ) {
            Boolean bReset = checkbox_ResetRating.isChecked();
            intentAnswer.putExtra( "resetrating", bReset );
        }

        setResult(RESULT_OK, intentAnswer );
        finish();
    }

    public void fnButtonCancel( View view ) {
        setResult(RESULT_CANCELED);
        finish();
    }
}
