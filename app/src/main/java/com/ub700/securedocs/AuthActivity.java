package com.ub700.securedocs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class AuthActivity extends AppCompatActivity {

    private static final String TAG = AuthActivity.class.getSimpleName();

    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final String ACCESS_STRING = "Access";
    private static final String EMAIL_STRING = "Email";
    private static final String KEY_STRING = "Key";
    private static int CHAR_LIMIT = 8;
    private static int NUM_RETRIES = 5;
    private static int NUM_BUTTONS = 10;
    private static int HIT_WAIT = 15;

    private ArFragment arFragment;
    private Scene scene;
    private AnchorNode anchorNode;
    private static int deviceHeight;
    private static int deviceWidth;
    private boolean firstSelectionDelay;
    private int hitCount;
    private Node selectedNode;
    private Node previousSelection;
    private ImageView buttonSelectorView;
    private static int buttonColor;
    private ValueAnimator buttonAnimator;
    private boolean isKeypadRendered;
    private boolean midAirMode = true;
    private String key;
    private StringBuilder enteredKey;
    private Toast wrongKeyToast;
    private int retries;
    private int accessLevel;
    private String emailID;

    private List<CompletableFuture<ModelRenderable>> buttonCompletable = new ArrayList<>();
    private List<ModelRenderable> buttons = new ArrayList<>();


    public AuthActivity() {
        hitCount = 0;
        isKeypadRendered = false;
        firstSelectionDelay = true;
        enteredKey = new StringBuilder();
        retries = -1;
        buttonColor = android.graphics.Color.argb(1,0.06f,0.06f, 0.85f);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        setContentView(R.layout.activity_auth);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        scene = arFragment.getArSceneView().getScene();

        /* Getting arguments passed from previous activity. */
        Intent intent = getIntent();
        key = intent.getExtras().getString(KEY_STRING);
        accessLevel = intent.getExtras().getInt(ACCESS_STRING);
        emailID = intent.getExtras().getString(EMAIL_STRING);

        buttonSelectorView = findViewById(R.id.button_selector);
        setDisplayHeightWidth();

        /* Building buttons to be rendered. */
        if (!buildButtons()) {
            Log.e(TAG, "Error loading Buttons!");
        }

        if (!midAirMode) {
            /* Plane listener to allow user to place keypad on surface of their choice.*/
            arFragment.setOnTapArPlaneListener(
                    (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                        if (buttons.size() != NUM_BUTTONS) {
                            Log.e(TAG, "Button size: "+buttons.size());
                            return;
                        }
                        if (!isKeypadRendered) {
                            Anchor anchor = hitResult.createAnchor();
                            anchorNode = new AnchorNode(anchor);
                            anchorNode.setParent(scene);

                            buildKeypad();
                            this.runOnUiThread(() -> {
                                buttonSelectorView.setVisibility(View.VISIBLE);
                            });

                            /* To ensure plane detection is stopped when buttons are rendered once. */
                            isKeypadRendered = true;
                            disablePlaneDetection(arFragment);
                        }
                    });
        } else {
            /* Anchoring keypad in front of the phone (Mid air).*/
            disablePlaneDetection(arFragment);
            arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
                if (buttons.size() != NUM_BUTTONS) {
                    return;
                }
                if (!isKeypadRendered) {
                    Frame arFrame = arFragment.getArSceneView().getArFrame();
                    if (arFrame == null || arFrame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                        return;
                    }
                    Pose cameraPose = arFrame.getCamera().getDisplayOrientedPose();
                    Pose newPose = cameraPose.compose(Pose.makeTranslation(0f, 0f, -0.8f));
                    newPose = newPose.compose(Pose.makeRotation((float) (1 / Math.sqrt(2)), 0f, 0f, (float) (1 / Math.sqrt(2))));
                    Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(newPose);
                    anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(scene);

                    buildKeypad();
                    this.runOnUiThread(() -> {
                        buttonSelectorView.setVisibility(View.VISIBLE);
                    });

                    /* To ensure plane detection is stopped when buttons are rendered once. */
                    isKeypadRendered = true;
                }
            });
        }

        /* To render keypad mid-air. */
        scene.addOnUpdateListener(frameTime -> {
            /* Activated only after buttons are rendered on screen. */
            if (isKeypadRendered) {
                /* To add delay the before the first selection. */
                if (firstSelectionDelay) {
                    hitCount++;
                    if (hitCount > HIT_WAIT + 5) {
                        hitCount = 0;
                        firstSelectionDelay = false;
                    }
                } else {
                    if (wrongKeyToast != null) {
                        wrongKeyToast.cancel();
                    }
                    /* HitTestResult holds any node in the scene hit by a ray. */
                    HitTestResult hitTestResult = scene.hitTest(scene.getCamera().screenPointToRay(deviceWidth / 2, deviceHeight / 2));
                    if (hitTestResult != null && hitTestResult.getNode() != null) {
                        /* To ensure same node is hit multiple times to get selected, to avoid over sensitivity in selection. */
                        if (hitCount == 0) {
                            selectedNode = hitTestResult.getNode();
                            hitCount = 1;
                        } else {
                            if (selectedNode.equals(hitTestResult.getNode())) {
                                hitCount++;
                            } else {
                                selectedNode = hitTestResult.getNode();
                                hitCount = 0;
                            }
                        }
                        if (hitCount > HIT_WAIT) {
                            if (buttonAnimator != null && previousSelection!= null)  {
                                previousSelection.getRenderable().getMaterial(1).setFloat3("baseColor", new Color(buttonColor));
                            }
                            previousSelection = selectedNode;
                            buttonAnimation(selectedNode, android.graphics.Color.GREEN);

                            /* Get selection and compare to actual key. */
                            enteredKey.append(selectedNode.getName());
                            Log.e(TAG, "Entered Key: "+enteredKey.toString());
                            if (enteredKey.toString().equals(key)) {
                                for (Node node : anchorNode.getChildren()) {
                                    buttonAnimation(node, android.graphics.Color.GREEN);
                                }
                                startNextActivity(true);
                            } else if (enteredKey.length() >= CHAR_LIMIT) {
                                resetKey();
                            }
                            hitCount = 0;
                        }
                    }
                }
            }
        });
    }

    /* Build all buttons for keypad. */
    private boolean buildButtons() {
        for (int i = 0; i < NUM_BUTTONS; i++) {
            try {
                buttonCompletable.add(i, ModelRenderable.builder()
                        .setSource(this, Uri.parse("button" + i + ".sfb"))
                        .build());
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
        CompletableFuture.allOf((CompletableFuture<ModelRenderable>[])
            buttonCompletable.toArray(new CompletableFuture[buttonCompletable.size()]))
            .handle((notUsed, throwable) -> {
                if (throwable != null) {
                    return null;
                }
                for (int i = 0; i < NUM_BUTTONS; i++) {

                    try {
                        ModelRenderable tempButton = buttonCompletable.get(i).get();
                        buttons.add(i, tempButton);
                    } catch (InterruptedException | ExecutionException ex) {
                        Log.e(TAG, ex.toString());
                    }
                }
                return null;
            });
        return true;
    }

    /* Build keypads using rendered buttons and attach to AR scene. */
    private void buildKeypad() {
        Vector3 localPosition = new Vector3();
        Node buttonNode;
        int index = 0;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                localPosition.set(0.3f * j, 0.0f, 0.3f * i);
                buttonNode = new Node();
                configureButton(buttonNode, localPosition, index);
                index++;
            }
            if (index == NUM_BUTTONS-1) {
                localPosition.set(0.0f, 0.0f,0.6f);
                buttonNode = new Node();
                configureButton(buttonNode, localPosition, -1);
            }
        }
    }

    /* Configure button and its placement on keypad. */
    private void configureButton(Node buttonNode, Vector3 localPosition, int index) {
        buttonNode.setParent(anchorNode);
        buttonNode.setLocalPosition(localPosition);
        buttonNode.setLocalRotation(Quaternion.lookRotation(new Vector3(0, 1, 0), new Vector3(0, 0, -1f)));
        buttonNode.setWorldScale(new Vector3(3, 3, 3));
        buttonNode.setRenderable(buttons.get(index+1));
        buttonNode.setName(Integer.toString(index + 1));
        buttonNode.setOnTapListener(((HitTestResult hitTestResult, MotionEvent motionEve) -> {
            if (hitTestResult != null || hitTestResult.getNode() != null)
                resetKey();
        }));
    }

    /* Disables plane detection for Mid air mode. */
    private void disablePlaneDetection(ArFragment arFragment) {
        arFragment.getArSceneView().getPlaneRenderer().setEnabled(false);
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);
        arFragment.getTransformationSystem().setSelectionVisualizer(new FootprintSelectionVisualizer());
    }

    /* Get screen resolution of device. */
    private void setDisplayHeightWidth() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        deviceHeight = displayMetrics.heightPixels;
        deviceWidth = displayMetrics.widthPixels;
    }

    /* Animation to show button selection. */
    private void buttonAnimation(Node selectedNode, int color) {
        buttonAnimator = new ValueAnimator();
        buttonAnimator.setIntValues(color, buttonColor);
        buttonAnimator.setEvaluator(new ArgbEvaluator());
        buttonAnimator.setInterpolator(new LinearInterpolator());
        if (selectedNode == null || selectedNode.getRenderable() == null || selectedNode.getRenderable().getMaterial(1) == null) {
            return;
        }
        buttonAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                selectedNode.getRenderable().getMaterial(1).setFloat3("baseColor", new Color(buttonColor));
            }});
        buttonAnimator.addUpdateListener((ValueAnimator valueButtonAnimatorator) -> {
            selectedNode.getRenderable().getMaterial(1).setFloat3("baseColor",
                    new Color((int) valueButtonAnimatorator.getAnimatedValue()));
        });
        buttonAnimator.setDuration(150);
        buttonAnimator.start();
    }

    /* Resets key entered if User presses back (touches button)
     * or exceeds character limit. */
    private void resetKey() {
        retries++;
        for (Node node : anchorNode.getChildren()) {
            buttonAnimation(node, android.graphics.Color.RED);
        }
        if (retries == NUM_RETRIES) {
            if (wrongKeyToast != null) {
                wrongKeyToast.cancel();
            }
            startNextActivity(false);
            return;
        }
        if (enteredKey.length() >= CHAR_LIMIT) {
            wrongKeyToast = Toast.makeText(this, "Wrong key entered!", Toast.LENGTH_SHORT);
        } else {
            wrongKeyToast = Toast.makeText(this, "Key reset!", Toast.LENGTH_SHORT);
        }
        enteredKey.delete(0, enteredKey.length());
        wrongKeyToast.setGravity(Gravity.BOTTOM, 0, 0);
        wrongKeyToast.show();
        firstSelectionDelay = true;
    }

    /* Start next activity. */
    private void startNextActivity(boolean authStatus) {
        if (authStatus) {
            Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
            intent.putExtra(ACCESS_STRING, accessLevel);
            intent.putExtra(EMAIL_STRING, emailID);
            startActivity(intent);
            finish();
        } else {
            Toast toast =
                    Toast.makeText(this, "Could not authenticate User!", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 250);
            toast.show();
            finish();
        }
    }

    /*
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}

