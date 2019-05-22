package com.ub700.securedocs;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformationSystem;

import androidx.appcompat.app.AppCompatActivity;

    public class SecretDocsActivity extends AppCompatActivity {

        private ArFragment arFragment;
        private ImageView fitToScanView;
        private TransformationSystem transformationSystem;
        private int accessLevel;

        /* Augmented image and its associated center pose anchor, keyed by the augmented image in the database. */
        private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            /* Start ARSession by setting up sceneform's AR fragment. */
            arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
            arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);

            fitToScanView = findViewById(R.id.image_view_fit_to_scan);

            Intent intent = getIntent();
            accessLevel = intent.getExtras().getInt("Access");
        }

        @Override
        protected void onResume() {
            super.onResume();
            if (augmentedImageMap.isEmpty()) {
                fitToScanView.setVisibility(View.VISIBLE);
            }
        }

        /* Called once everytime a frame is updated in the AR session. */
        private void onUpdateFrame(FrameTime frameTime) {
            Frame frame = arFragment.getArSceneView().getArFrame();

            /* If there is no frame or ARCore is not tracking yet, just return. */
            if (frame == null || frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                return;
            }
            Collection<AugmentedImage> updatedAugmentedImages =
                    frame.getUpdatedTrackables(AugmentedImage.class);
            for (AugmentedImage augmentedImage : updatedAugmentedImages) {
                switch (augmentedImage.getTrackingState()) {
                    case PAUSED:
                        // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                        // but not yet tracked.
                        break;

                    case TRACKING:
                        /* Have to switch to UI Thread to update View. */
                        fitToScanView.setVisibility(View.GONE);

                        /* Create a new anchor for newly detected images. */
                        if (!augmentedImageMap.containsKey(augmentedImage)) {
                            transformationSystem = arFragment.getTransformationSystem();
                            AugmentedImageNode node = new AugmentedImageNode(this, transformationSystem, augmentedImage.getName(), accessLevel);
                            node.setImage(augmentedImage);
                            augmentedImageMap.put(augmentedImage, node);
                            arFragment.getArSceneView().getScene().addChild(node);
                        }
                        break;

                    case STOPPED:
                        AugmentedImageNode remNode = augmentedImageMap.get(augmentedImage);
                        if (remNode != null) {
                            arFragment.getArSceneView().getScene().removeChild(remNode);
                            remNode.getAnchor().detach();
                        }
                        augmentedImageMap.remove(augmentedImage);
                        break;
                }
            }
        }
    }