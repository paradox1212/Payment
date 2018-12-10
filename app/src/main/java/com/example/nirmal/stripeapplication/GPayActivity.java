package com.example.nirmal.stripeapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.stripe.android.model.Token;

import java.util.Arrays;

public class GPayActivity extends Activity {
    Button getGPayToken;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.google_pay);
        setListeners(startPaymentActivity());
    }

    private  PaymentsClient startPaymentActivity(){
        PaymentsClient paymentsClient = Wallet.getPaymentsClient(getApplicationContext(),new Wallet.WalletOptions.Builder().setEnvironment(WalletConstants.ENVIRONMENT_TEST).build());
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
                                Toast.makeText(GPayActivity.this,"Google pay can be integrated",Toast.LENGTH_SHORT).show();
                                Log.i(StripeConstants.LOGGER_CONSTANT,"Can integrate G-pay");
                            } else {
                                Toast.makeText(GPayActivity.this,"Cannot show google pay",Toast.LENGTH_SHORT).show();
                                Log.i(StripeConstants.LOGGER_CONSTANT,"Cannot integrate G-pay");
                            }
                        } catch (ApiException exception) {
                            Log.e(StripeConstants.LOGGER_CONSTANT,"Exception while fetching gpay compatibilty ",exception);
                        }
                    }
                });
        return paymentsClient;
    }

    private PaymentMethodTokenizationParameters createTokenizationParameters() {
        return PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                .addParameter("gateway", "stripe")
                .addParameter("stripe:publishableKey", StripeConstants.PUBLISHABLE_KEY)
                .addParameter("stripe:version", "2018-11-08")
                .build();
    }

    private PaymentDataRequest createPaymentDataRequest(String amount, String currency) {
        Log.i(StripeConstants.LOGGER_CONSTANT,"Creating payment request");
        PaymentDataRequest.Builder request =PaymentDataRequest.newBuilder()
                        .setTransactionInfo(
                                TransactionInfo.newBuilder()
                                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                                        .setTotalPrice(amount)
                                        .setCurrencyCode(currency)
                                        .build())
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                        .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                        .setCardRequirements(
                                CardRequirements.newBuilder()
                                        .addAllowedCardNetworks(Arrays.asList(
                                                WalletConstants.CARD_NETWORK_AMEX,
                                                WalletConstants.CARD_NETWORK_DISCOVER,
                                                WalletConstants.CARD_NETWORK_VISA,
                                                WalletConstants.CARD_NETWORK_MASTERCARD))
                                        .build());

        request.setPaymentMethodTokenizationParameters(createTokenizationParameters());
        return request.build();
    }

    private void setListeners(final PaymentsClient client) {
        getGPayToken = findViewById(R.id.gpay_pay_button);
        getGPayToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PaymentDataRequest request = createPaymentDataRequest("1.00", "INR");
                if (request != null) {
                    AutoResolveHelper.resolveTask(
                            client.loadPaymentData(request),
                            GPayActivity.this,
                            StripeConstants.GOOGLE_PAY_REQUEST_CODE);

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
                    }
                }
        }
    }
}
