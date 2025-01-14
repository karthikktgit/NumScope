package com.example.numscope;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputType;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.numscope.R;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;  // Import TextBlock
import com.google.android.gms.vision.text.TextRecognizer;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private SurfaceView cameraView;
    private CameraSource cameraSource;
    private TextView resultView;
    private EditText numberInput;
    private static final int CAMERA_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.camera_view);
        resultView = findViewById(R.id.result_view);
        numberInput = findViewById(R.id.number_input);

        numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            startCameraSource();
        }

        numberInput.setOnEditorActionListener((v, actionId, event) -> {
            String input = numberInput.getText().toString();
            if (!input.isEmpty()) {
                int number = Integer.parseInt(input);
                resultView.setText(checkNumberProperties(number));
            }
            return false;
        });
    }

    private void startCameraSource() {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();
        if (!textRecognizer.isOperational()) {
            resultView.setText("Text recognizer is not operational.");
            return;
        }

        cameraSource = new CameraSource.Builder(this, textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(640, 480)
                .setAutoFocusEnabled(true)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
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

        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {}

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {
                // Get detected text as a SparseArray of TextBlocks
                SparseArray<TextBlock> detectedText = detections.getDetectedItems();

                if (detectedText.size() > 0) {
                    StringBuilder detectedTextString = new StringBuilder();

                    // Loop through the detected text blocks and extract text
                    for (int i = 0; i < detectedText.size(); i++) {
                        TextBlock textBlock = detectedText.valueAt(i);  // Get each TextBlock
                        detectedTextString.append(textBlock.getValue()).append("\n"); // Append text value
                    }

                    // Update the UI with the recognized text
                    runOnUiThread(() -> numberInput.setText(detectedTextString.toString()));
                }
            }
        });
    }

    private String checkNumberProperties(int number) {
        StringBuilder properties = new StringBuilder();

        if (isArmstrong(number)) properties.append("Armstrong Number\n");
        if (isHappy(number)) properties.append("Happy Number\n");
        // Add other checks here

        return properties.toString();
    }

    private boolean isArmstrong(int number) {
        int originalNumber = number, result = 0, n = 0;
        while (originalNumber != 0) {
            originalNumber /= 10;
            n++;
        }
        originalNumber = number;
        while (originalNumber != 0) {
            int remainder = originalNumber % 10;
            result += Math.pow(remainder, n);
            originalNumber /= 10;
        }
        return result == number;
    }

    private boolean isHappy(int number) {
        int slow, fast;
        slow = fast = number;
        do {
            slow = numSquareSum(slow);
            fast = numSquareSum(numSquareSum(fast));
        } while (slow != fast);
        return slow == 1;
    }

    private int numSquareSum(int n) {
        int squareSum = 0;
        while (n != 0) {
            squareSum += (n % 10) * (n % 10);
            n /= 10;
        }
        return squareSum;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.special_numbers_info) {
            showNumberInfo();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showNumberInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Special Numbers");
        builder.setMessage("List of special numbers and their meanings here.");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }
}
