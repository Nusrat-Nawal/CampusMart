package com.campusmart.app.api;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import com.campusmart.app.R;
import com.campusmart.app.utils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class GeminiApi {
    private static final String TAG = "GeminiApi";
    private final OkHttpClient client;
    private final String apiKey;
    private final Context context;

    public GeminiApi(Context context) {
        this.context = context;
        this.apiKey = context.getString(R.string.gemini_api_key);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void extractTextFromImage(Uri imageUri, GeminiResponseCallback callback) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                callback.onFailure("Failed to open image file.");
                return;
            }

            byte[] imageData = getBytesFromInputStream(inputStream);
            String base64Image = Base64.encodeToString(imageData, Base64.NO_WRAP);

            String requestUrl = Constants.GEMINI_BASE_URL + apiKey;
            Log.d(TAG, "Request URL: " + requestUrl);

            // Constructing the JSON request body
            JsonObject imagePart = new JsonObject();
            imagePart.addProperty("mimeType", "image/jpeg");
            imagePart.addProperty("data", base64Image);

            JsonObject inlineDataPart = new JsonObject();
            inlineDataPart.add("inlineData", imagePart);

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", "Extract the full name, student ID number, and university name from this image. Provide the extracted data in a single-line JSON format with keys: \"name\", \"studentId\", and \"university\". Do not include any other text or explanation.");

            JsonArray partsArray = new JsonArray();
            partsArray.add(textPart);
            partsArray.add(inlineDataPart);

            JsonObject contentsObject = new JsonObject();
            contentsObject.add("parts", partsArray);

            JsonArray contentsArray = new JsonArray();
            contentsArray.add(contentsObject);

            JsonObject requestBodyJson = new JsonObject();
            requestBodyJson.add("contents", contentsArray);

            RequestBody requestBody = RequestBody.create(requestBodyJson.toString(), MediaType.parse("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(requestUrl)
                    .post(requestBody)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Gemini API call failed", e);
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        if (response.isSuccessful()) {
                            String extractedText = parseGeminiResponse(responseBody);
                            if (extractedText != null) {
                                callback.onSuccess(extractedText);
                            } else {
                                callback.onFailure("Could not parse response from Gemini API.");
                            }
                        } else {
                            Log.e(TAG, "Gemini API failed with HTTP " + response.code() + ": " + responseBody);
                            callback.onFailure("API Error: " + responseBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse Gemini API response", e);
                        callback.onFailure("Failed to parse Gemini API response: " + e.getMessage());
                    } finally {
                        response.body().close();
                    }
                }
            });

        } catch (IOException e) {
            callback.onFailure("Failed to process image: " + e.getMessage());
        }
    }


    private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }


    public static String parseGeminiResponse(String jsonResponse) throws JSONException {
        JSONObject root = new JSONObject(jsonResponse);
        if (root.has("candidates")) {
            JSONObject candidate = root.getJSONArray("candidates").getJSONObject(0);
            if (candidate.has("content")) {
                JSONObject content = candidate.getJSONObject("content");
                if (content.has("parts")) {
                    JSONObject part = content.getJSONArray("parts").getJSONObject(0);
                    if (part.has("text")) {
                        return part.getString("text");
                    }
                }
            }
        }
        return null;
    }

    public interface GeminiResponseCallback {
        void onSuccess(String jsonResult);
        void onFailure(String errorMessage);
    }
}