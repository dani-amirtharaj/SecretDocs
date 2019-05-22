package com.ub700.securedocs;


import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.File;
import java.io.FileInputStream;
import androidx.annotation.Nullable;

/**
 * Extend the ArFragment to customize the ARCore session configuration to include Augmented Images.
 */
public class AugmentedImageFragment extends ArFragment {
    private static final String TAG = "AugmentedImageFragment";

    // This is the name of the image in the sample database.  A copy of the image is in the assets
    // directory.  Opening this image on your computer is a good quick way to test the augmented image
    // matching.
    // private static final String DEFAULT_IMAGE_NAME = "/default.jpg";

    // This is a pre-created database containing the sample image.
    private static final String SAMPLE_IMAGE_DATABASE = "sample_database.imgdb";

    // Augmented image configuration and rendering.
    // Load a single image (true) or a pre-generated image database (false).
    private static final boolean USE_SINGLE_IMAGE = true;

    // Do a runtime check for the OpenGL level available at runtime to avoid Sceneform crashing the
    // application.
    private static final double MIN_OPENGL_VERSION = 3.0;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Check for Sceneform being supported on this device.  This check will be integrated into
        // Sceneform eventually.
        String openGlVersionString =
                ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 or later");
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Turn off the plane discovery since we're only looking for images
        getPlaneDiscoveryController().hide();
        getPlaneDiscoveryController().setInstructionView(null);
        getArSceneView().getPlaneRenderer().setEnabled(false);
        return view;
    }

    @Override
    protected Config getSessionConfiguration(Session session) {
        Config config = super.getSessionConfiguration(session);
        config.setFocusMode(Config.FocusMode.AUTO);
        if (!setupAugmentedImageDatabase(config, session)) {
            Toast.makeText(getContext(), "No documents uploaded yet!", Toast.LENGTH_LONG).show();
        }
        return config;
    }

    /* Load Augmented image database from device storage (private to app). */
    private boolean setupAugmentedImageDatabase(Config config, Session session) {
        AugmentedImageDatabase augmentedImageDatabase = null;

        AssetManager assetManager = getContext() != null ? getContext().getAssets() : null;
        if (assetManager == null) {
            Log.e(TAG, "Context is null, cannot intitialize image database.");
            return false;
        }

        try {
            File file = new File(getContext().getFilesDir(), "augmentedDB.imgdb");
            if (file.exists()) {
                FileInputStream fileInputStream = new FileInputStream(file);
                augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, fileInputStream);
                fileInputStream.close();
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return false;
        }

        config.setAugmentedImageDatabase(augmentedImageDatabase);
        return true;
    }
}
