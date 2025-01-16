package com.example.numscope;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.InputType;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private SurfaceView cameraView;
    private CameraSource cameraSource;
    private TextView resultView;
    private EditText numberInput;
    private Button captureButton;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private boolean processingImage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupInputHandling();
        checkCameraPermission();
    }

    private void initializeViews() {
        cameraView = findViewById(R.id.camera_view);
        resultView = findViewById(R.id.result_view);
        numberInput = findViewById(R.id.number_input);
        captureButton = findViewById(R.id.capture_button);
        numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    private void setupInputHandling() {
        captureButton.setOnClickListener(v -> captureImage());

        numberInput.setOnEditorActionListener((v, actionId, event) -> {
            processInputNumber();
            return true;
        });
    }

    private void processInputNumber() {
        String input = numberInput.getText().toString().trim();
        if (!input.isEmpty()) {
            try {
                int number = Integer.parseInt(input);
                resultView.setText(NumberUtils.analyzeNumber(number));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE
            );
        } else {
            startCameraSource();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraSource();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startCameraSource() {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();
        if (!textRecognizer.isOperational()) {
            resultView.setText("Text recognizer is not operational.");
            return;
        }

        setupCamera(textRecognizer);
        setupTextRecognizer(textRecognizer);
    }

    private void setupCamera(TextRecognizer textRecognizer) {
        cameraSource = new CameraSource.Builder(this, textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1280, 720)
                .setRequestedFps(30.0f)
                .setAutoFocusEnabled(true)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(cameraView.getHolder());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });
    }

    private void setupTextRecognizer(TextRecognizer textRecognizer) {
        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {}

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {
                if (processingImage) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() > 0) {
                        StringBuilder detectedText = new StringBuilder();
                        for (int i = 0; i < items.size(); i++) {
                            TextBlock item = items.valueAt(i);
                            detectedText.append(item.getValue()).append("\n");
                        }

                        String text = detectedText.toString().trim();
                        // Extract the first number found in the text
                        Pattern pattern = Pattern.compile("\\d+");
                        Matcher matcher = pattern.matcher(text);

                        runOnUiThread(() -> {
                            if (matcher.find()) {
                                String number = matcher.group();
                                numberInput.setText(number);
                                processInputNumber();
                            }
                            processingImage = false;
                            captureButton.setEnabled(true);
                        });
                    }
                }
            }
        });
    }

    private void captureImage() {
        if (!processingImage) {
            processingImage = true;
            captureButton.setEnabled(false);
            // The actual capture and processing will happen in the text recognizer processor
            Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }
}