package com.oxoo.spagreen;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.oxoo.spagreen.network.RetrofitClient;
import com.oxoo.spagreen.network.apis.PaymentApi;
import com.oxoo.spagreen.network.apis.SubscriptionApi;
import com.oxoo.spagreen.network.model.ActiveStatus;
import com.oxoo.spagreen.utils.ApiResources;
import com.oxoo.spagreen.utils.ToastMsg;
import com.oxoo.spagreen.utils.Tools;
import com.oxoo.spagreen.network.model.Package;

import co.paystack.android.Paystack;
import co.paystack.android.PaystackSdk;
import co.paystack.android.Transaction;
import co.paystack.android.model.Card;
import co.paystack.android.model.Charge;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class PayStackActivity extends AppCompatActivity {


    private TextInputLayout cardNoTil, cardValidityTil, cardCvvTil;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private TextView cardNo, cardCVV, cardExpire;

//    String cardNumber = "4084084084084081";
//    int expiryMonth = 11; //any month in the future
//    int expiryYear = 22; // any year in the future. '2018' would work also!
//    String cvv = "408";  // cvv of the test card

    private Button processBt;

    private Card card;
    private Package packageItem;

    private boolean isDark;
    private Menu menu;
    private String currencySymbol;
    private String userId;
    private String email;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences("push", MODE_PRIVATE);
        isDark = sharedPreferences.getBoolean("dark", false);

        if (isDark) {
            setTheme(R.style.AppThemeDark);
        } else {
            setTheme(R.style.AppThemeLight);
        }

        setContentView(R.layout.activity_pay_stack);

        PaystackSdk.setPublicKey(ApiResources.PAY_STACK_PUBLIC_KEY);

        packageItem = (Package) getIntent().getSerializableExtra("package");
        currencySymbol = getIntent().getStringExtra("currency");


        SharedPreferences ferences = getSharedPreferences("user", MODE_PRIVATE);
        userId = ferences.getString("id", null);
        email = ferences.getString("email", null);

        initView();


        // change toolbar color as theme color
        if (isDark) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.black_window_light));
            processBt.setBackground(getResources().getDrawable(R.drawable.btn_rounded_dark));
        }

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Payment for "+ packageItem.getName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        cardNoTil.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int count) {
                if (charSequence.toString().trim().length() == 0) {
                    cardNo.setText("**** **** **** ****");
                } else {
                    String number = Tools.insertPeriodically(charSequence.toString().trim(), " ", 4);
                    cardNo.setText(number);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        cardValidityTil.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int count) {
                if (charSequence.toString().trim().length() == 0) {
                    cardExpire.setText("MM/YY");
                } else {
                    String exp = Tools.insertPeriodically(charSequence.toString().trim(), "", 2);
                    cardExpire.setText(exp);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        cardCvvTil.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int count) {
                if (charSequence.toString().trim().length() == 0) {
                    cardCVV.setText("***");
                } else {
                    cardCVV.setText(charSequence.toString().trim());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


    }


    @Override
    protected void onStart() {
        super.onStart();

        processBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String cardNo = cardNoTil.getEditText().getText().toString();
                String cvv = cardCvvTil.getEditText().getText().toString();
                String date = cardValidityTil.getEditText().getText().toString();
                String year = date.substring(date.lastIndexOf("/") + 1);

                if (cardNo.isEmpty() || cardNo.length() != 16) {
                    cardCvvTil.setError("Card Number is not valid.");
                    return;
                } else if (cvv.isEmpty() || cvv.length() != 3) {
                    cardCvvTil.setError("CVV Number is not valid.");
                    return;
                } else if (!Tools.isValidFormat(date)) {
                    cardValidityTil.setError("Format is not Correct");
                    return;
                }
                String month = date.substring(0,2);
                card = new Card(cardNo, Integer.valueOf(month), Integer.valueOf(year), cvv);
                Log.d("data:", month+" "+year);

                if (card.isValid()) {
                    performCharge();
                } else {
                    new ToastMsg(PayStackActivity.this).toastIconError("Invalid card.");
                }
            }
        });

    }

    public void performCharge(){

        Double amount = Double.valueOf(packageItem.getPrice()) * ApiResources.POUND_TO_NGN_EXCHANGE_RATE;
        amount = Double.parseDouble(String.format("%.2f", amount));
        amount = (amount * 100);
        Log.d("amount:", amount+"");
        //create a Charge object
        Charge charge = new Charge();
        charge.setAmount(amount.intValue());
        charge.setEmail(email);
        charge.setCard(card); //sets the card to charge
        charge.setCurrency("NGN");


        progressBar.setVisibility(View.VISIBLE);
        processBt.setVisibility(View.GONE);
        PaystackSdk.chargeCard(this, charge, new Paystack.TransactionCallback() {
            @Override
            public void onSuccess(Transaction transaction) {
                //Toast.makeText(PayStackActivity.this, transaction.getReference()+"", Toast.LENGTH_SHORT).show();
                sendDataToServer(transaction.getReference());
            }

            @Override
            public void beforeValidate(Transaction transaction) {
                Log.d("before ref:", transaction.getReference());
                // This is called only before requesting OTP.
                // Save reference so you may send to server. If
                // error occurs with OTP, you should still verify on server.
            }

            @Override
            public void onError(Throwable error, Transaction transaction) {
                error.printStackTrace();
                Toast.makeText(PayStackActivity.this, transaction.getReference()+"", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void initView() {

        processBt = findViewById(R.id.process_bt);
        cardCvvTil = findViewById(R.id.card_cvv_til);
        cardNoTil = findViewById(R.id.card_no_til);
        cardValidityTil = findViewById(R.id.card_validity_til);
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        cardNo = findViewById(R.id.card_number);
        cardCVV = findViewById(R.id.card_cvv);
        cardExpire = findViewById(R.id.card_expire);

    }

    private void sendDataToServer(String ref) {



        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        PaymentApi paymentApi = retrofit.create(PaymentApi.class);
        Call<ResponseBody> call = paymentApi.savePayment(Config.API_KEY, packageItem.getPlanId(), userId, packageItem.getPrice(),
                ref, "Paystack");

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    new ToastMsg(PayStackActivity.this).toastIconSuccess(getResources().getString(R.string.payment_success));
                    updateActiveStatus(userId);

                } else {
                    new ToastMsg(PayStackActivity.this).toastIconError("Something went wrong.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                new ToastMsg(PayStackActivity.this).toastIconError("Something went wrong."+t.getMessage());
                t.printStackTrace();
            }

        });

    }


    private void updateActiveStatus(String userId) {

        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        SubscriptionApi subscriptionApi = retrofit.create(SubscriptionApi.class);

        Call<ActiveStatus> call = subscriptionApi.getActiveStatus(com.oxoo.spagreen.Config.API_KEY, userId);
        call.enqueue(new Callback<ActiveStatus>() {
            @Override
            public void onResponse(Call<ActiveStatus> call, Response<ActiveStatus> response) {
                if (response.code() == 200) {
                    ActiveStatus activeStatus = response.body();
                    saveActiveStatus(activeStatus);
                    finish();
                } else {
                    new ToastMsg(PayStackActivity.this).toastIconError("Payment info not save to the own server. something went wrong.");
                }
            }

            @Override
            public void onFailure(Call<ActiveStatus> call, Throwable t) {
                t.printStackTrace();
                new ToastMsg(PayStackActivity.this).toastIconError(t.getMessage());
            }
        });

    }

    private void saveActiveStatus(ActiveStatus activeStatus) {
        SharedPreferences.Editor editor = getSharedPreferences("activeStatus", MODE_PRIVATE).edit();
        editor.putString("activeStatus", activeStatus.getStatus());
        editor.apply();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.rave_menu, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem amountItem = menu.findItem(R.id.amount_menu);
        amountItem.setTitle(currencySymbol +" "+packageItem.getPrice());

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
