package com.example.nirmal.stripeapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.CircularProgressDrawable;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CardInfo;
import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardInputWidget;

import java.util.Arrays;


public class HomePageActivity extends Activity {
    View stripe_payment_card,gpay_payment_card;
    ImageView stripe_image,gpay_image;
    TextView stripe_name_text,stripe_desc_text, gpay_name_text, gpay_desc_text;
    int finalAmount;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initViews();
    }


    public void initViews(){
        setupStripeView();
        setupGPayView();
        setupClickListeners();
    }

    public void setupClickListeners(){
        View.OnClickListener paymentListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view.getId() == R.id.stripe_integ){
                    initiatePayment(StripeConstants.STRIPE_PAYMENT_CODE);
                }else if(view.getId() == R.id.gpay_integ){
                    initiatePayment(StripeConstants.GPAY_PAYMENT_CODE);
                }
            }
        };
        stripe_payment_card.setOnClickListener(paymentListener);
        gpay_payment_card.setOnClickListener(paymentListener);
    }

    public void setupStripeView(){
        stripe_payment_card = findViewById(R.id.stripe_integ);
        stripe_image = stripe_payment_card.findViewById(R.id.card_image);
        stripe_name_text = stripe_payment_card.findViewById(R.id.payment_method_name);
        stripe_desc_text = stripe_payment_card.findViewById(R.id.payment_method_description);
        stripe_image.setImageResource(R.drawable.stripe_image);
        stripe_name_text.setText(R.string.stripe_name);
        stripe_desc_text.setText(R.string.stripe_description);
    }

    public void setupGPayView(){
        gpay_payment_card = findViewById(R.id.gpay_integ);
        gpay_image = gpay_payment_card.findViewById(R.id.card_image);
        gpay_name_text = gpay_payment_card.findViewById(R.id.payment_method_name);
        gpay_desc_text = gpay_payment_card.findViewById(R.id.payment_method_description);
        gpay_image.setImageResource(R.drawable.googleg_standard_color_18);
        gpay_name_text.setText(R.string.gpay_name);
        gpay_desc_text.setText(R.string.gpay_description);
    }

    public void initiatePayment(final int paymentCode){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter the amount to pay");

        final EditText input = new EditText(HomePageActivity.this);
//        final EditText
        input.setInputType(InputType.TYPE_CLASS_NUMBER );
        input.setHint("Currency in USD");
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String amountStr = input.getText().toString();
                Log.i(StripeConstants.LOGGER_CONSTANT,amountStr);
                int amount = Integer.valueOf(amountStr);
                if (amount > 0) {
                    handlePayIntent(amount,paymentCode);
                } else {
                    Toast.makeText(getApplicationContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show();
                }

            }
        });
        builder.show();
    }

    public void handlePayIntent(int amount, int paymentCode){
        Log.i(StripeConstants.LOGGER_CONSTANT, "Going to pay amount "+String.valueOf(amount)+" with option "+String.valueOf(paymentCode));
        if(paymentCode == StripeConstants.STRIPE_PAYMENT_CODE){
            startStripePayment(amount);
        }else if(paymentCode == StripeConstants.GPAY_PAYMENT_CODE){
            startGPayPayment(amount);
        }
    }

    public void startStripePayment(final int amount){
        AlertDialog.Builder stripeCardDetails = new AlertDialog.Builder(HomePageActivity.this);
        stripeCardDetails.setTitle("Enter card details");
        final com.stripe.android.view.CardInputWidget cardInputWidget = new CardInputWidget(HomePageActivity.this);
        stripeCardDetails.setView(cardInputWidget);
        stripeCardDetails.setPositiveButton("Pay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Card cardToSave = cardInputWidget.getCard();
                int isCardValid = StripeUtil.getInstance().isCardValid(cardToSave);
                if(isCardValid == 0) {
                    Log.i(StripeConstants.LOGGER_CONSTANT,"Incomplete card data");
                }else if(isCardValid == 1){
                    Log.i(StripeConstants.LOGGER_CONSTANT,"Valid card data");
                    ProgressDialog progressDialog = StripeUtil.getInstance().getSpinner(HomePageActivity.this);
                    progressDialog.show();
                    generateToken(getApplicationContext(),amount,cardToSave,progressDialog);
                }else if(isCardValid == 2){
                    Log.i(StripeConstants.LOGGER_CONSTANT,"Card validation fails, Card details are invalid");
                }
            }
        });
        stripeCardDetails.show();
    }

    public void generateToken(final Context context, final int amount , Card card, final ProgressDialog dialog){
        Log.i(StripeConstants.LOGGER_CONSTANT,"Got the generate token callback");
        Stripe stripe = new Stripe(context, StripeConstants.PUBLISHABLE_KEY);
        stripe.createToken(card, new TokenCallback() {
            public void onSuccess(Token token) {
                Log.i(StripeConstants.LOGGER_CONSTANT,"Token generated "+ token.getId());
                try {
                    StripeUtil.getInstance().chargeUser(context, amount, token.getId(), dialog);
                }catch (Exception e){
                    Log.i(StripeConstants.LOGGER_CONSTANT,"Exception happened ",e);
                    dialog.dismiss();
                }
            }
            public void onError(Exception error) {
                Log.i(StripeConstants.LOGGER_CONSTANT,"Error occured while fetching the token");
            }
        });
    }

    public void startGPayPayment(final int amount){
        finalAmount = amount;
        final PaymentsClient paymentsClient = Wallet.getPaymentsClient(getApplicationContext(),new Wallet.WalletOptions.Builder().setEnvironment(WalletConstants.ENVIRONMENT_TEST).build());
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
                                Log.i(StripeConstants.LOGGER_CONSTANT,"Can integrate G-pay");
                                PaymentDataRequest request = StripeUtil.getInstance().createPaymentDataRequest(String.valueOf(amount), "usd");
                                if (request != null) {
                                    AutoResolveHelper.resolveTask(paymentsClient.loadPaymentData(request),HomePageActivity.this,StripeConstants.GOOGLE_PAY_REQUEST_CODE);
                                }
                            } else {
                                Log.i(StripeConstants.LOGGER_CONSTANT,"Cannot integrate G-pay");
                                Toast.makeText(HomePageActivity.this, "G-Pay app not found, install the app and try again",Toast.LENGTH_LONG).show();
                            }
                        } catch (ApiException exception) {
                            Log.e(StripeConstants.LOGGER_CONSTANT,"Exception while fetching gpay compatibilty ",exception);
                        }
                    }
                });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case StripeConstants.GOOGLE_PAY_REQUEST_CODE:
                if(resultCode == Activity.RESULT_OK){
                    PaymentData paymentData = PaymentData.getFromIntent(data);
                    // You can get some data on the user's card, such as the brand and last 4 digits
                    CardInfo info = paymentData.getCardInfo();
                    // You can also pull the user address from the PaymentData object.
                    UserAddress address = paymentData.getShippingAddress();
                    // This is the raw JSON string version of your Stripe token.
                    String rawToken = paymentData.getPaymentMethodToken().getToken();
                    // Now that you have a Stripe token object, charge that by using the id
                    Token stripeToken = Token.fromString(rawToken);
                    if (stripeToken != null) {
                        // This chargeToken function is a call to your own server, which should then connect
                        // to Stripe's API to finish the charge.
                        Log.i(StripeConstants.LOGGER_CONSTANT,stripeToken.getId());
                        ProgressDialog dialog = StripeUtil.getInstance().getSpinner(HomePageActivity.this);
                        try {
                            StripeUtil.getInstance().chargeUser(HomePageActivity.this, finalAmount, stripeToken.getId(), dialog);
                        }catch (Exception e){
                            Log.e(StripeConstants.LOGGER_CONSTANT,"Exception occured ",e);
                            dialog.dismiss();
                        }
                    }
                }
        }
    }

}
