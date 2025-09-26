package com.campusmart.app.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.IOException;

public class MlKitOcrService {

    private final TextRecognizer recognizer;

    public MlKitOcrService() {
        // Initialize the TextRecognizer
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    // Method to recognize text from a Bitmap
    public Task<Text> recognizeText(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        return recognizer.process(image);
    }

    // Overloaded method to recognize text from a Uri (e.g., from gallery or camera)
    public Task<Text> recognizeText(Context context, Uri imageUri) throws IOException {
        InputImage image = InputImage.fromFilePath(context, imageUri);
        return recognizer.process(image);
    }

    // You would then call this method and add OnSuccessListener and OnFailureListener
    // to get the results.
    // For example:
    //
    // service.recognizeText(bitmap)
    //         .addOnSuccessListener(new OnSuccessListener<Text>() {
    //             @Override
    //             public void onSuccess(Text visionText) {
    //                 // Task completed successfully
    //                 String resultText = visionText.getText();
    //                 // Process resultText
    //             }
    //         })
    //         .addOnFailureListener(
    //                 new OnFailureListener() {
    //                     @Override
    //                     public void onFailure(@NonNull Exception e) {
    //                         // Task failed with an exception
    //                         // ...
    //                     }
    //                 });
}
