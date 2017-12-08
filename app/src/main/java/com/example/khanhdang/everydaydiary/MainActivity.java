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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity  implements SensorEventListener {

    private static final String TAG = "MainActivity";
    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 3000;
    public static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER =  2;

    // Firebase instance variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDiaryDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private DiaryAdapter mDiaryAdapter;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mDiaryPhotosStorageReference;

    //Diary elements
    private String mUsername;
    private String mDate;
    private float mLatitude;
    private float mLongitude;
    private Date mdate;

    private EditText edtEditDiary;
    private Button btnDone;
    private ImageButton imbtnPhoto;
    private ListView mDiaryListView;
    private Button btn_start, btn_stop, btn_map;
    private TextView textView;
    private BroadcastReceiver broadcastReceiver;

    //Light sensor variables
    SensorManager mSensorManager;
    Sensor mLight;
    boolean mCurrentStateIsDark = true;
    boolean mPreviousState = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "now running: onCreate");

        mUsername = ANONYMOUS;
        mDate = "Date";
        mLatitude = 0;
        mLongitude = 0;

        // Initialize references to views
        edtEditDiary = (EditText) findViewById(R.id.editTextEditDiary);
        btnDone = (Button) findViewById(R.id.buttonDone);
        imbtnPhoto = (ImageButton) findViewById(R.id.imageButtonPhoto);
        mDiaryListView = (ListView) findViewById(R.id.diaryListView);

        btn_start = (Button) findViewById(R.id.button1);
        btn_stop = (Button) findViewById(R.id.button2);
        btn_map = (Button) findViewById(R.id.buttonMap);

        // Initialize message ListView and its adapter
        List<Diary> diaries = new ArrayList<>();
        mDiaryAdapter = new DiaryAdapter(this, R.layout.item_diary, diaries);
        mDiaryListView.setAdapter(mDiaryAdapter);

        // Initialize SharedPreferences
        final SharedPreferences sharedPref = this.getSharedPreferences("com.example.app",
                Context.MODE_PRIVATE);

        final MediaPlayer sendSound = MediaPlayer.create(this, R.raw.send);

        // Show welcome dialog
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Welcome to Everydaydiary")
                    .setMessage("Share your day with the whole world")
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setIcon(android.R.drawable.btn_star_big_on)
                    .show();

        // Light sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // Move to MapActivity
        btn_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent map = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(map);
            }
        });

        if(!runtime_permissions())
            enable_buttons();

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();

        mDiaryDatabaseReference = mFirebaseDatabase.getReference().child("diarykai");
        mDiaryPhotosStorageReference = mFirebaseStorage.getReference().child("diary_photos");
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    onSignedInInitialize(user.getDisplayName());
                } else {
                    // User is signed out
                    onSignedOutCleanUp();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(
                                            Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                                    new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

        // Initialize progress bar
        //mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        imbtnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

                // Get Date and location for photos
                Date currentTime = Calendar.getInstance().getTime();
                mdate = currentTime;
                mLatitude = sharedPref.getFloat("latitude", 0);
                mLongitude = sharedPref.getFloat("longitude", 0);

                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        // Enable Send button when there's text to send
        edtEditDiary.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    btnDone.setEnabled(true);
                } else {
                    btnDone.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        edtEditDiary.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a diary and clears the EditText
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendSound.start();
                // Get Date and location for diaries
                Date currentTime = Calendar.getInstance().getTime();
                mdate = currentTime;
                mLatitude = sharedPref.getFloat("latitude", 0);
                mLongitude = sharedPref.getFloat("longitude", 0);

                Diary diary = new Diary(edtEditDiary.getText().toString(), mUsername, null, mdate, mLatitude, mLongitude);
                mDiaryDatabaseReference.push().setValue(diary);

                // Clear input box
                edtEditDiary.setText("");
            }
        });

    }

    //Log in and photo status
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
            } else if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
                Uri selectedImageUri = data.getData();

                // Get a reference to store file at chat_photos/<FILENAME>
                StorageReference photoRef = mDiaryPhotosStorageReference.child(selectedImageUri.getLastPathSegment());

                // Upload file to Firebase Storage
                photoRef.putFile(selectedImageUri)
                        .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // When the image has successfully uploaded, we get its download URL
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                // Set the download URL to the message box, so that the user can send it to the database
                                Diary friendlyMessage = new Diary(null, mUsername, downloadUrl.toString(), mdate, mLatitude, mLongitude);
                                mDiaryDatabaseReference.push().setValue(friendlyMessage);
                            }
                        });
            }
        }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "now running: onPause");

        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        mDiaryAdapter.clear();
        detachDatabaseReadListener();

        // Light sensor
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "now running: onResume");
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);

        // Light sensor
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "now running: onDestroy");

        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        Log.d(TAG, "now running: onRestart");
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "now running: onStop");
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "now running: onStart");
    }

    // On/Off location services
    private void enable_buttons() {
        final MediaPlayer correctSound = MediaPlayer.create(this, R.raw.correct);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                correctSound.start();
                Intent i = new Intent(getApplicationContext(),GPS_Service.class);
                startService(i);
            }
        });
        final MediaPlayer wrongSound = MediaPlayer.create(this, R.raw.wrong);
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wrongSound.start();
                Intent i = new Intent(getApplicationContext(),GPS_Service.class);
                stopService(i);
            }
        });
    }

    // Get location permission
    private boolean runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},100);

            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                enable_buttons();
            }else {
                runtime_permissions();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    // Sign out of the app
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onSignedInInitialize(String username) {

        mUsername = username;
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanUp(){
        mUsername = ANONYMOUS;
        mDiaryAdapter.clear();
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Diary diary = dataSnapshot.getValue(Diary.class);
                    mDiaryAdapter.add(diary);
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            mDiaryDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mDiaryDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        float lux = event.values[0];

        Float luxFloat = new Float(lux);
        //Toast.makeText(this, "Light sensor: " + luxFloat.toString(), Toast.LENGTH_SHORT).show();

        Float luxTooDark = new Float(50.0);
        if (luxFloat < luxTooDark) {
            //Toast.makeText(this, "Outside is too dark, please reduce the screen brightness!", Toast.LENGTH_SHORT).show();
            mCurrentStateIsDark = true;
        } else {
            //Toast.makeText(this, "ok: " + luxFloat.toString(), Toast.LENGTH_SHORT).show();
            mCurrentStateIsDark = false;
        }
        mPreviousState = mCurrentStateIsDark;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}