package com.example.nirmal.stripeapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.model.Card;
import com.stripe.android.view.CardInputWidget;


public class MainActivity extends AppCompatActivity {
    CardInputWidget cardInputWidget;
    Button submitButton,chargeUserButton, googlePayButton;
    EditText amountToCharge;
    PaymentsClient paymentsClient;
    ProgressDialog progressDialog;
    PaymentsClient client;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setListeners();
        checkForGPayIntegration();
    }

    private void setListeners(){
        submitButton.setOnClickListener(cardListener);
        chargeUserButton.setOnClickListener(chargeListener);
        googlePayButton.setOnClickListener(gPayListener);
    }

    private void initView(){
        cardInputWidget = findViewById(R.id.card_input_widget);
        submitButton = findViewById(R.id.card_details_submit);
        chargeUserButton = findViewById(R.id.charge_user);
        amountToCharge = findViewById(R.id.amount);
        googlePayButton = findViewById(R.id.google_pay_button);
        chargeUserButton.setVisibility(View.INVISIBLE);
        amountToCharge.setVisibility(View.INVISIBLE);
        googlePayButton.setVisibility(View.INVISIBLE);
    }

    android.view.View.OnClickListener cardListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Card cardToSave = cardInputWidget.getCard();
            int isCardValid = StripeUtil.getInstance().isCardValid(cardToSave);
            if(isCardValid == 0) {
                Log.i(StripeConstants.LOGGER_CONSTANT,"Incomplete card data");
            }else if(isCardValid == 1){
                Log.i(StripeConstants.LOGGER_CONSTANT,"Valid card data");
                StripeUtil.getInstance().generateToken(getApplicationContext(),cardToSave);
                chargeUserButton.setVisibility(View.VISIBLE);
                amountToCharge.setVisibility(View.VISIBLE);
            }else if(isCardValid == 2){
                Log.i(StripeConstants.LOGGER_CONSTANT,"Card validation fails, Card details are invalid");
            }

        }
    };

    View.OnClickListener gPayListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            PaymentDataRequest request = StripeUtil.getInstance().createPaymentDataRequest("1.00", "INR");
            if (request != null) {
                AutoResolveHelper.resolveTask(
                        client.loadPaymentData(request),
                        MainActivity.this,
                        StripeConstants.GOOGLE_PAY_REQUEST_CODE);

            }
        }
    };

    View.OnClickListener chargeListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
//            chargeUser();
        }
    };

//    private void chargeUser(){
//        try {
//            StripeUtil.getInstance().chargeUser(getApplicationContext(),Integer.valueOf(amountToCharge.getText().toString()), "usd", "test-currency");
//        }catch (Exception e){
//            Log.e(StripeConstants.LOGGER_CONSTANT,e.toString());
//        }
//    }

    public void checkForGPayIntegration(){
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Checking for Google pay");
        progressDialog.setMessage("Please wait");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();
        paymentsClient = Wallet.getPaymentsClient(getApplicationContext(),new Wallet.WalletOptions.Builder().setEnvironment(WalletConstants.ENVIRONMENT_TEST).build());
        IsReadyToPayRequest request = IsReadyToPayRequest.newBuilder()
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                .build();
        Task<Boolean> task = paymentsClient.isReadyToPay(request);
        task.addOnCompleteListener(
                new OnCompleteListener<Boolean>() {
                    public void onComplete(Task<Boolean> task) {
                        try {
                            boolean result = task.getResult(ApiException.class);
                            if(result) {
                                Toast.makeText(MainActivity.this,"Google pay can be integrated",Toast.LENGTH_SHORT).show();
                                Log.i(StripeConstants.LOGGER_CONSTANT,"Can integrate G-pay");
                                googlePayButton.setVisibility(View.VISIBLE);
                            } else {
                                Toast.makeText(MainActivity.this,"Cannot show google pay",Toast.LENGTH_SHORT).show();
                                Log.i(StripeConstants.LOGGER_CONSTANT,"Cannot integrate G-pay");
                            }
                        } catch (ApiException exception) {
                            Log.e(StripeConstants.LOGGER_CONSTANT,"Exception while fetching gpay compatibilty ",exception);
                        }
                        progressDialog.dismiss();
                    }
                });

    }
}
