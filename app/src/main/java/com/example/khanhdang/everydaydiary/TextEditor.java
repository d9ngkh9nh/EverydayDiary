/*
  RMIT University Vietnam
  Course: COSC2657 - Android Development
  Semester: 2017C
  Assignment: 2
  Author: Dang Dinh Khanh
  ID: s3618748
  Created date: 05/12/2017
  Acknowledgement:
  -https://firebase.google.com/docs/android/setup
  -https://www.udacity.com
  -https://developers.google.com/maps/documentation/android-api/marker
  -https://www.lynda.com
*/

package com.example.khanhdang.everydaydiary;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class TextEditor extends AppCompatActivity {

    private Button btnSend;
    private EditText editTextEdit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_editor);

        btnSend = (Button) findViewById(R.id.buttonSend);
        editTextEdit = (EditText) findViewById(R.id.editTextEdit);

        final SharedPreferences sharedPref = this.getSharedPreferences("com.example.app",
                Context.MODE_PRIVATE);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPref.edit().putString("text",(String) editTextEdit.getText().toString()).apply();
                Intent goToMain = new Intent(TextEditor.this, MainActivity.class);
                startActivity(goToMain);
            }
        });
    }
}
