package com.example.myapplicationtt.adapters;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.util.Util;
import com.example.myapplicationtt.R;
import com.example.myapplicationtt.Utils;
import com.example.myapplicationtt.databinding.RowImagesPickedBinding;
import com.example.myapplicationtt.models.ModelImagePicked;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class AdapterImagesPicked extends RecyclerView.Adapter<AdapterImagesPicked.HolderImagesPicked>{
    private RowImagesPickedBinding binding;
    private static final String TAG ="IMAGES_TAG";

    private Context context;
    private ArrayList<ModelImagePicked> imagePickedArrayList;

    private String adId;

    public AdapterImagesPicked(Context context,ArrayList<ModelImagePicked> imagePickedArrayList,String adId){
        this.context = context;
        this.imagePickedArrayList = imagePickedArrayList;
        this.adId = adId;
    }

    @NonNull
    @Override
    public HolderImagesPicked onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        binding = RowImagesPickedBinding.inflate(LayoutInflater.from(context),parent,false);

        return new HolderImagesPicked(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderImagesPicked holder, int position) {
        ModelImagePicked  model = imagePickedArrayList.get(position);

        if(model.getFromInternet()){
            String imageUrl = model.getImageUrl();
            Log.d(TAG, "onBindViewHolder: imageUrl: "+imageUrl);

            try{
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_image_gray)
                        .into(holder.imageIv);
            }
            catch(Exception e){
                Log.e(TAG, "onBindViewHolder: ", e);
            }
        }
        else{

            Uri imageUri = model.getImageUri();

            Log.d(TAG, "onBindViewHolder: imageUri: "+imageUri);

            try{

                Glide.with(context)
                        .load(imageUri)
                        .placeholder(R.drawable.ic_image_gray)
                        .into(holder.imageIv);

            }catch (Exception e){
                Log.e(TAG, "onBindViewHolder: ", e);
            }
        }


        holder.closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(model.getFromInternet()){

                    deleteImageFirebase(model,holder,position);

                }else{
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        imagePickedArrayList.remove(adapterPosition);
                        notifyItemRemoved(adapterPosition);
                    }
                }

            }
        });
    }

    private void deleteImageFirebase(ModelImagePicked model, HolderImagesPicked holder, int position) {

        String imageId = model.getId();
        Log.d(TAG, "deleteImageFirebase: adId: "+adId);
        Log.d(TAG, "deleteImageFirebase: imageId: "+imageId);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Ads");
        ref.child(adId).child("Images").child(imageId)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                        Log.d(TAG, "onSuccess: Deleted");
                        Utils.toast(context,"Image Deleted.");

                        try{
                            imagePickedArrayList.remove(model);
                            notifyItemRemoved(position);

                        }catch (Exception e){
                            Log.e(TAG, "onSuccess: ", e);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        Utils.toast(context,"Failed to delete image due to "+e.getMessage());
                    }
                });

    }

    @Override
    public int getItemCount() {
        return imagePickedArrayList.size();
    }

    class HolderImagesPicked extends RecyclerView.ViewHolder {

        ImageView imageIv;
        ImageButton closeBtn;

        public HolderImagesPicked(@NonNull View itemView) {
            super(itemView);

            imageIv = binding.imageIv;
            closeBtn = binding.closeBtn;


        }
    }
}
