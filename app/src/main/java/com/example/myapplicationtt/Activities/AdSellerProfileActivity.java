package com.example.myapplicationtt.Activities;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.myapplicationtt.R;
import com.example.myapplicationtt.Utils;
import com.example.myapplicationtt.adapters.AdapterAd;
import com.example.myapplicationtt.databinding.ActivityAdSellerProfileBinding;
import com.example.myapplicationtt.models.ModelAd;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdSellerProfileActivity extends AppCompatActivity {

    private ActivityAdSellerProfileBinding binding;
    private static final String TAG = "AD_SELLER_PROFILE_TAG";
    private String sellerUid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAdSellerProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sellerUid = getIntent().getStringExtra("sellerUid");
        Log.d(TAG, "onCreate: sellerUid: " + sellerUid);

        loadSellerDetails();
        loadAds();

        binding.toolbarBackBtn.setOnClickListener(view -> onBackPressed());
    }

    private void loadSellerDetails() {
        Log.d(TAG, "loadSellerDetails: Fetching seller details from Firebase");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(sellerUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = "" + snapshot.child("name").getValue();
                String profileImageUrl = "" + snapshot.child("profileImageUrl").getValue();
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                if (timestamp != null) {
                    String formattedDate = Utils.formatTimestampDate(timestamp);
                    binding.sellerMemberSinceTv.setText(formattedDate);
                } else {
                    Log.e(TAG, "onDataChange: timestamp is null");
                }

                binding.sellerNameTv.setText(name);

                Glide.with(AdSellerProfileActivity.this)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.ic_person_white)
                        .into(binding.sellerProfileIv);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadSellerDetails: onCancelled", error.toException());
            }
        });
    }

    private void loadAds() {
        Log.d(TAG, "loadAds: Loading ads for seller");

        ArrayList<ModelAd> adArrayList = new ArrayList<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Ads");
        ref.orderByChild("uid").equalTo(sellerUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                adArrayList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelAd modelAd = ds.getValue(ModelAd.class);
                    adArrayList.add(modelAd);
                }

                AdapterAd adapterAd = new AdapterAd(AdSellerProfileActivity.this, adArrayList);
                binding.adsRv.setAdapter(adapterAd);
                binding.publishedAdsCountTv.setText(String.valueOf(adArrayList.size()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadAds: onCancelled", error.toException());
            }
        });
    }
}
