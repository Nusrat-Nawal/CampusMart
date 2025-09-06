package com.campusmart.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.campusmart.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;
import java.util.UUID;

public class PaymentActivity extends AppCompatActivity {

    private TextView textViewPaymentInstruction, textViewItemPriceInfo, textViewFeeInfo, textViewTotalToPay;
    private MaterialButton buttonBkash, buttonNogod, buttonBankCard, buttonConfirmPosting;
    private LinearLayout layoutPaymentOptions;
    private CardView cardViewPaymentDetails, cardViewReceipt;
    private TextView textViewReceiptTitle, textViewReceiptTransactionId, textViewReceiptPaymentMethod, textViewReceiptAmountPaid;
    private TextView textViewReceiptUserName, textViewReceiptUserEmail; // Added for user details

    private double itemPrice;
    private double feeToPay;
    private final double FEE_PERCENTAGE = 0.05;

    private FirebaseUser currentUser;

    public static final String EXTRA_ITEM_PRICE = "com.campusmart.app.EXTRA_ITEM_PRICE";
    public static final String EXTRA_PAYMENT_CONFIRMED = "com.campusmart.app.EXTRA_PAYMENT_CONFIRMED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        textViewPaymentInstruction = findViewById(R.id.textViewPaymentInstruction);
        textViewItemPriceInfo = findViewById(R.id.textViewItemPriceInfo);
        textViewFeeInfo = findViewById(R.id.textViewFeeInfo);
        textViewTotalToPay = findViewById(R.id.textViewTotalToPay);
        buttonBkash = findViewById(R.id.buttonBkash);
        buttonNogod = findViewById(R.id.buttonNogod);
        buttonBankCard = findViewById(R.id.buttonBankCard);
        buttonConfirmPosting = findViewById(R.id.buttonConfirmPosting);
        layoutPaymentOptions = findViewById(R.id.layoutPaymentOptions);
        cardViewPaymentDetails = findViewById(R.id.cardViewPaymentDetails);

        cardViewReceipt = findViewById(R.id.cardViewReceipt);
        textViewReceiptTitle = findViewById(R.id.textViewReceiptTitle);
        textViewReceiptUserName = findViewById(R.id.textViewReceiptUserName); // Initialize
        textViewReceiptUserEmail = findViewById(R.id.textViewReceiptUserEmail); // Initialize
        textViewReceiptTransactionId = findViewById(R.id.textViewReceiptTransactionId);
        textViewReceiptPaymentMethod = findViewById(R.id.textViewReceiptPaymentMethod);
        textViewReceiptAmountPaid = findViewById(R.id.textViewReceiptAmountPaid);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_ITEM_PRICE)) {
            String priceString = intent.getStringExtra(EXTRA_ITEM_PRICE);
            try {
                itemPrice = Double.parseDouble(priceString);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Error: Invalid item price received.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        } else {
            Toast.makeText(this, "Error: Item price not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_LONG).show();
            // Optionally finish activity or redirect to login
            // For now, we'll allow proceeding but receipt info will be blank for user details
        }

        calculateAndDisplayFees();

        buttonBkash.setOnClickListener(v -> handlePaymentOptionClick("Bkash"));
        buttonNogod.setOnClickListener(v -> handlePaymentOptionClick("Nogod"));
        buttonBankCard.setOnClickListener(v -> handlePaymentOptionClick("Bank Card"));

        buttonConfirmPosting.setOnClickListener(v -> confirmAndProceedWithPosting());
    }

    private void calculateAndDisplayFees() {
        feeToPay = itemPrice * FEE_PERCENTAGE;
        textViewItemPriceInfo.setText(String.format(Locale.getDefault(), "Item Price: %.2f Tk", itemPrice));
        textViewFeeInfo.setText(String.format(Locale.getDefault(), "Posting Fee (%.0f%%): %.2f Tk", FEE_PERCENTAGE * 100, feeToPay));
        textViewTotalToPay.setText(String.format(Locale.getDefault(), "Total to Pay: %.2f Tk", feeToPay));
    }

    private void handlePaymentOptionClick(String paymentMethod) {
        layoutPaymentOptions.setVisibility(View.GONE);
        textViewPaymentInstruction.setVisibility(View.GONE);

        // Populate User Info on Receipt
        if (currentUser != null) {
            String userName = currentUser.getDisplayName();
            String userEmail = currentUser.getEmail();
            textViewReceiptUserName.setText("User: " + (userName != null && !userName.isEmpty() ? userName : "N/A"));
            textViewReceiptUserEmail.setText("Email: " + (userEmail != null && !userEmail.isEmpty() ? userEmail : "N/A"));
        } else {
            textViewReceiptUserName.setText("User: Not available");
            textViewReceiptUserEmail.setText("Email: Not available");
        }

        String fakeTransactionId = "CM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        textViewReceiptTransactionId.setText("Transaction ID: " + fakeTransactionId);
        textViewReceiptPaymentMethod.setText("Paid via: " + paymentMethod);
        textViewReceiptAmountPaid.setText(String.format(Locale.getDefault(), "Amount Paid: %.2f Tk", feeToPay));
        textViewReceiptTitle.setText("Payment Successful!");

        cardViewReceipt.setVisibility(View.VISIBLE);
        buttonConfirmPosting.setVisibility(View.VISIBLE);

        Toast.makeText(this, paymentMethod + " selected. Payment processed (simulation).", Toast.LENGTH_SHORT).show();
    }

    private void confirmAndProceedWithPosting() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_PAYMENT_CONFIRMED, true);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}
