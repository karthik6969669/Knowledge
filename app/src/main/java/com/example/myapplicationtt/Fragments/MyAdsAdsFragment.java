package com.example.myapplicationtt.Fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.myapplicationtt.adapters.AdapterAd;
import com.example.myapplicationtt.databinding.FragmentMyAdsAdsBinding;
import com.example.myapplicationtt.models.ModelAd;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class MyAdsAdsFragment extends Fragment {


    private FragmentMyAdsAdsBinding binding;

    private static  final String TAG ="MY_ADS_TAG";

    private Context mContext;

    private FirebaseAuth firebaseAuth;

    private ArrayList<ModelAd> adArrayList;

    private AdapterAd adapterAd;

    public MyAdsAdsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {

        mContext = context;
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        binding = FragmentMyAdsAdsBinding.inflate(inflater , container , false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth =FirebaseAuth.getInstance();

        loadAds();

        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                try{

                    String query = charSequence.toString();
                    adapterAd.getFilter().filter(query);

                }catch (Exception e){
                    Log.e(TAG, "onTextChanged: ", e);
                }


            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void loadAds(){
        Log.d(TAG, "loadAds: ");

        adArrayList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Ads");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        adArrayList.clear();

                        for(DataSnapshot ds: snapshot.getChildren()){

                            try{

                                ModelAd modelAd = ds.getValue(ModelAd.class);

                                adArrayList.add(modelAd);

                            }catch (Exception e){
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }
                        adapterAd = new AdapterAd(mContext,adArrayList);
                        binding.adsRv.setAdapter(adapterAd);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
}