package com.example.myapplicationtt.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplicationtt.Activities.AdDetailsActivity;
import com.example.myapplicationtt.FilterAd;
import com.example.myapplicationtt.R;
import com.example.myapplicationtt.Utils;
import com.example.myapplicationtt.databinding.RowAdBinding;
import com.example.myapplicationtt.models.ModelAd;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdapterAd extends RecyclerView.Adapter<AdapterAd.HolderAd> implements Filterable {

    private RowAdBinding binding;
    private static final String TAG = "ADAPTER_AD_TAG";

    private FirebaseAuth firebaseAuth;
    private Context context;
    public ArrayList<ModelAd> adArrayList;
    private ArrayList<ModelAd> filterList;
    private FilterAd filter;

    public AdapterAd(Context context, ArrayList<ModelAd> adArrayList) {
        this.context = context;
        this.adArrayList = adArrayList;
        this.filterList = new ArrayList<>(adArrayList); // Create a copy for filtering
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderAd onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        binding = RowAdBinding.inflate(LayoutInflater.from(context), parent, false);
        return new HolderAd(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderAd holder, int position) {
        ModelAd modelAd = adArrayList.get(position);
        holder.titleTv.setText(modelAd.getTitle());
        holder.descriptionTv.setText(modelAd.getDescription());
        holder.addressTv.setText(modelAd.getAddress());
        holder.priceTv.setText(modelAd.getPrice());
        holder.dateTv.setText(Utils.formatTimestampDate(modelAd.getTimestamp()));


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =new Intent(context, AdDetailsActivity.class);
                intent.putExtra("adId",modelAd.getId());
                context.startActivity(intent);
            }
        });

        loadAdFirstImage(modelAd, holder);
        if (firebaseAuth.getCurrentUser() != null) {
            checkIsFavorite(modelAd, holder);
        }

        holder.favBtn.setOnClickListener(view -> {
            boolean favorite = modelAd.isFavorite();
            if (favorite) {
                Utils.removeFromFavorite(context, modelAd.getId());
            } else {
                Utils.addToFavorite(context, modelAd.getId());
            }
        });
    }

    private void checkIsFavorite(ModelAd modelAd, HolderAd holder) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Favorites").child(modelAd.getId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean favorite = snapshot.exists();
                        modelAd.setFavorite(favorite);
                        holder.favBtn.setImageResource(favorite ? R.drawable.ic_fav_yes : R.drawable.ic_fav_no);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database error: ", error.toException());
                    }
                });
    }

    private void loadAdFirstImage(ModelAd modelAd, HolderAd holder) {
        String adId = modelAd.getId();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Ads");
        reference.child(adId).child("Images").limitToFirst(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String imageUrl = ds.child("imageUrl").getValue(String.class);
                            if (imageUrl != null) {
                                Glide.with(context)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.ic_image_gray)
                                        .into(holder.imageIv);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database error: ", error.toException());
                    }
                });
    }

    @Override
    public int getItemCount() {
        return adArrayList.size();
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new FilterAd(this, filterList);
        }
        return filter;
    }

    class HolderAd extends RecyclerView.ViewHolder {
        ShapeableImageView imageIv;
        TextView titleTv, descriptionTv, addressTv, priceTv, dateTv;
        ImageButton favBtn;

        public HolderAd(@NonNull View itemView) {
            super(itemView);
            imageIv = binding.imageIv;
            titleTv = binding.titleTv;
            descriptionTv = binding.descriptionTv;
            addressTv = binding.addressTv;
            priceTv = binding.priceTv;
            dateTv = binding.dateTv;
            favBtn = binding.favBtn;
        }
    }
}
