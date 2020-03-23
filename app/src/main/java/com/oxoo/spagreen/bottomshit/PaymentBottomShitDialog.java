package com.oxoo.spagreen.bottomshit;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.oxoo.spagreen.R;

public class PaymentBottomShitDialog extends BottomSheetDialogFragment {

    public static final String PAYPAL = "paypal";
    public static final String STRIP = "strip";
    public static final String PAYSTACK = "paystack";

    private OnBottomShitClickListener bottomShitClickListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.layout_payment_bottom_shit, container,
                false);

        ImageButton paypalBt, stripBt, payStackBt;
        paypalBt = view.findViewById(R.id.paypal_btn);
        stripBt = view.findViewById(R.id.stripe_btn);
        payStackBt = view.findViewById(R.id.pay_stack_btn);

        paypalBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomShitClickListener.onBottomShitClick(PAYPAL);
            }
        });

        stripBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomShitClickListener.onBottomShitClick(STRIP);
            }
        });

        payStackBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomShitClickListener.onBottomShitClick(PAYSTACK);
            }
        });


        return view;

    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            bottomShitClickListener = (OnBottomShitClickListener) context;
        } catch (Exception e) {
            throw new ClassCastException(context.toString() + " must be implemented");
        }

    }

    public interface OnBottomShitClickListener {
        void onBottomShitClick(String paymentMethodName);
    }


}

