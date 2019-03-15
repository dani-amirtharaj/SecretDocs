package com.ub700.securedocs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;


public class MainPage extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 0;
    private static final String TAG = MainPage.class.toString();
    private String emailId;
    private int accessLevel;
    private boolean isAdmin;
    private static int docId = 0;
    private View loadDocButton;
    private Bitmap loadedImage;
    private String accessSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        accessLevel = intent.getExtras().getInt("Access");
        emailId = intent.getExtras().getString("Email");
        isAdmin = intent.getExtras().getBoolean("Admin");
//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        loadDocButton = findViewById(R.id.load_document);
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

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.access_levels, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
            int pos, long id) {
                // An item was selected. You can retrieve the selected item using
                accessSelection = (String) parent.getItemAtPosition(pos);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }});

            Bitmap bmp = null;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        if (docId == 0) {
            docId = sharedPref.getInt("Doc ID", docId);
        }
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(getString(R.string.custom_document)+Integer.toString(docId), BarcodeFormat.QR_CODE, 300, 300);
            int width = bitMatrix.getWidth();
            bmp = Bitmap.createBitmap(width, width, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < width; y++) {
                    bmp.setPixel(y, x, bitMatrix.get(x, y) == true ? Color.BLACK : Color.WHITE);
                }
            }
        } catch (WriterException e) {
            e.printStackTrace();
        }
        if (bmp != null) {
            sendEmail(bmp, emailId);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("Doc ID", ++docId);
            editor.commit();
        }

//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


     public void sendEmail(Bitmap bmp, String emailID) {
         Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
         emailIntent.setType("application/image");
         emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{emailID});
         emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,"Test Subject");
         emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "From My App");
         emailIntent.putExtra(Intent.EXTRA_STREAM, getImageUri(this, bmp));
         startActivity(Intent.createChooser(emailIntent, "Send mail..."));
     }
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

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
                    parcelFileDescriptor.close();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }
    }

}
