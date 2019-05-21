package com.ub700.securedocs;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.FixedHeightViewSizer;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/**
 * Node for rendering an augmented image. The image is framed by placing the virtual picture frame
 * at the corners of the augmented image trackable.
 */
@SuppressWarnings({"AndroidApiChecker"})
public class AugmentedImageNode extends AnchorNode {

    private static final String TAG = "AugmentedImageNode";

    // The augmented image represented by this node.
    private AugmentedImage image;
    private ImageView imgView;

    private CompletableFuture<ViewRenderable> secret;
    private TransformationSystem transformationSystem;
    private ViewRenderable img;

    public AugmentedImageNode(Context context, TransformationSystem transformationSystem, String augmentedImage, int accessLevel) {
        // Upon construction, start loading the models for the corners of the frame.
        if (secret == null) {
            String[] imageParams = augmentedImage.split(":");
            imgView = new ImageView(context);
            if (Integer.parseInt(imageParams[1]) <= accessLevel) {
                imgView.setImageBitmap(loadImageBitmap(context, imageParams[2]));
            } else {
                imgView.setImageBitmap(loadImageBitmap(context, "default"));
            }
            secret = ViewRenderable.builder()
                    .setView(context, imgView)
                    .setVerticalAlignment(ViewRenderable.VerticalAlignment.CENTER)
                    .setSizer(new FixedHeightViewSizer((float) 0.4))
                    .build();
            this.transformationSystem = transformationSystem;
        }
    }

    /**
     * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
     * created based on an Anchor created from the image. The corners are then positioned based on the
     * extents of the image. There is no need to worry about world coordinates since everything is
     * relative to the center of the image, which is the parent node of the corners.
     */
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    public void setImage(AugmentedImage image) {
        this.image = image;

        // If any of the models are not loaded, then recurse when all are loaded.
        if (!secret.isDone()) {
            CompletableFuture.allOf(secret)
                    .thenAccept((Void aVoid) -> setImage(image))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
        }

        img = secret.getNow(null);

        if (img != null) {
            // Set the anchor based on the center of the image.
            setAnchor(image.createAnchor(image.getCenterPose()));
            TransformableNode andy = new TransformableNode(this.transformationSystem);
            andy.setParent(this);
            Node node = new Node();
            node.setParent(andy);
            node.setRenderable(img);
            node.setLocalRotation(Quaternion.lookRotation(new Vector3(0, 1, 0), new Vector3(0, 0, -1)));
            transformationSystem.setSelectionVisualizer(new FootprintSelectionVisualizer());
            andy.select();
            node.getRenderable().setShadowCaster(false);
        }
    }

    private Bitmap loadImageBitmap(Context context, String document) {
        Log.e(TAG, "Loading document :"+document);
        if (document.equals("default")) {
            Log.e(TAG, "Loading default");
            try (InputStream is = context.getAssets().open("RenderImages/doc0.png")) {
                return BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                Log.e(TAG, "IO exception loading augmented image bitmap.", e);
            }
        }
        try (FileInputStream inputStream = context.openFileInput(context.getString(R.string.document_view) + ":" + document)) {
            Log.e(TAG, "Loading original");
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e);
        }
        return null;
    }
}
