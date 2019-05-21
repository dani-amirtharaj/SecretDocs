package com.ub700.securedocs;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Session;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MenuActivity extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 0;
    private static final String TAG = MenuActivity.class.toString();
    private String emailId;
    private int accessLevel;
    private static int docId = 0;
    private View loadDocButton;
    private View sendEmailButton;
    private View uploadButton;
    private View viewDocumentsFirstButton;
    private View uploadAnotherButton;
    private View viewDocumentsButton;
    private Bitmap loadedImage;
    private String accessSelection;
    private Bitmap qrCode;
    private SharedPreferences sharedPref;

    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final String ACCESS_STRING = "Access";
    private static final String EMAIL_STRING = "Email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Intent intent = getIntent();
        accessLevel = intent.getExtras().getInt(ACCESS_STRING);
        emailId = intent.getExtras().getString(EMAIL_STRING);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.access_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                // An item was selected. You can retrieve the selected item using
                accessSelection = (String) parent.getItemAtPosition(pos);
                accessSelection = accessSelection.substring(6);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }});

        loadDocButton = findViewById(R.id.uploadButton);
        sendEmailButton = findViewById(R.id.emailButton);
        uploadAnotherButton = findViewById(R.id.againButton);
        viewDocumentsButton = findViewById(R.id.viewButton);
        uploadButton = findViewById(R.id.firstUploadButton);
        viewDocumentsFirstButton = findViewById(R.id.firstViewButton);

        loadDocButton.setOnClickListener((View v) -> {
            // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
            // browser.
            Intent  docIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            docIntent.addCategory(Intent.CATEGORY_OPENABLE);

            // Filter to show only images, using the image MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            docIntent.setType("image/*");
            startActivityForResult(docIntent, READ_REQUEST_CODE);

        });

        sendEmailButton.setOnClickListener((View v) -> {
            if (qrCode != null) {
                sendEmail(qrCode, emailId);
                try {
                    String openGlVersionString =
                            ((ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE))
                                    .getDeviceConfigurationInfo()
                                    .getGlEsVersion();
                    if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
                        Log.e(TAG, "Sceneform requires OpenGL ES 3.0 or later");
                        Toast.makeText(getApplicationContext(), "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG).show();
                    } else {
                        findViewById(R.id.sendEmailLayout).setVisibility(View.GONE);
                        findViewById(R.id.continueLayout).setVisibility(View.VISIBLE);
                        File file = new File(this.getFilesDir(), getString(R.string.document_view) + ":" + docId);
                        try (FileOutputStream out = new FileOutputStream(file)) {
                            loadedImage.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                            // PNG is a lossless format, the compression factor (100) is ignored
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        file = new File(this.getFilesDir(), "augmentedDB.imgdb");
                        Session arSession = new Session(getApplicationContext());
                        AugmentedImageDatabase imageDatabase;
                        try {
                            Log.e(TAG, "File exists");
                            FileInputStream fileInputStream = new FileInputStream(file);
                            imageDatabase = AugmentedImageDatabase.deserialize(arSession, fileInputStream);
                            imageDatabase.addImage(getString(R.string.custom_document) + ":" + accessSelection + ":" + docId, qrCode);
                            fileInputStream.close();
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                            Log.e(TAG, "File does not exist");
                            imageDatabase = new AugmentedImageDatabase(arSession);
                            imageDatabase.addImage(getString(R.string.custom_document) + ":" + accessSelection + ":" + docId, qrCode);
                        }
                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        imageDatabase.serialize(fileOutputStream);
                        arSession.close();
                        fileOutputStream.close();
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("Doc ID", ++docId);
                editor.commit();
            }
        });

        uploadAnotherButton.setOnClickListener((View v) -> {
            findViewById(R.id.continueLayout).setVisibility(View.GONE);
            findViewById(R.id.uploadDocsLayout).setVisibility(View.VISIBLE);
        });

        viewDocumentsButton.setOnClickListener((View v) -> {
            Intent intentNext = new Intent(getApplicationContext(), SecretDocsActivity.class);
            intentNext.putExtra("Access", accessLevel);
            startActivity(intentNext);
        });

        uploadButton.setOnClickListener((View v) -> {
            findViewById(R.id.menuLayout).setVisibility(View.GONE);
            findViewById(R.id.uploadDocsLayout).setVisibility(View.VISIBLE);
        });

        viewDocumentsFirstButton.setOnClickListener((View v) -> {
            Intent intentNext = new Intent(getApplicationContext(), SecretDocsActivity.class);
            intentNext.putExtra("Access", accessLevel);
            startActivity(intentNext);
        });
    }


    public void sendEmail(Bitmap bmp, String emailID) {
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("application/image");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{emailID});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,"QR code for my document!");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Print out this QR code and use the app to view your document!");
        emailIntent.putExtra(Intent.EXTRA_STREAM, getImageUri(this, bmp));
        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        findViewById(R.id.continueLayout).setVisibility(View.VISIBLE);
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        } else {
            String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
            return Uri.parse(path);
        }
        Log.e(TAG, "NULL");
        return null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Log.i(TAG, "Uri: " + uri.toString());
                try {
                    ParcelFileDescriptor parcelFileDescriptor =
                            getContentResolver().openFileDescriptor(uri, "r");
                    FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                    this.loadedImage = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                    if (this.loadedImage != null) {
                        Log.e(TAG, "Image load success!");
                    }
                    parcelFileDescriptor.close();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
                Bitmap bmp = null;
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                sharedPref = this.getPreferences(Context.MODE_PRIVATE);
                if (docId == 0) {
                    docId = sharedPref.getInt("Doc ID", docId);
                }
                Log.e(TAG, "docId"+docId);
                try {
                    BitMatrix bitMatrix = qrCodeWriter.encode(getSHA(getString(R.string.custom_document)+docId), BarcodeFormat.QR_CODE, 500, 500);
                    int width = bitMatrix.getWidth();
                    bmp = Bitmap.createBitmap(width, width, Bitmap.Config.RGB_565);
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < width; y++) {
                            bmp.setPixel(y, x, bitMatrix.get(x, y)? Color.BLACK : Color.WHITE);
                        }
                    }
                } catch (WriterException e) {
                    e.printStackTrace();
                }
                qrCode = bmp;
            }
            findViewById(R.id.uploadDocsLayout).setVisibility(View.GONE);
            findViewById(R.id.sendEmailLayout).setVisibility(View.VISIBLE);
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }
        }
    }

    public static String getSHA(String input)
    {
        try {
            // Static getInstance method is called with hashing SHA
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // digest() method called
            // to calculate message digest of an input
            // and return array of byte
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        }
        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            System.out.println("Exception thrown"
                    + " for incorrect algorithm: " + e);
            return null;
        }
    }

}