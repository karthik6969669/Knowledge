package com.example.myapplicationtt.Activities;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplicationtt.R;
import com.example.myapplicationtt.Utils;
import com.example.myapplicationtt.adapters.AdapterImagesPicked;
import com.example.myapplicationtt.databinding.ActivityAdCreateBinding;
import com.example.myapplicationtt.models.ModelImagePicked;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AdCreateActivity extends AppCompatActivity {
    private ActivityAdCreateBinding binding;
    private static final String TAG = "AD_CREATE_TAG";
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;
    private Uri imageUri = null;

    private ArrayList<ModelImagePicked> imagePickedArrayList;
    private AdapterImagesPicked adapterImagesPicked;
    private boolean isEditMode = false;
    private String adIdForEditing = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAdCreateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseAuth = FirebaseAuth.getInstance();

        ArrayAdapter<String> adapterCategories = new ArrayAdapter<>(this, R.layout.row_category_act, Utils.categories);
        binding.categoryAct.setAdapter(adapterCategories);

        Intent intent = getIntent();
        isEditMode = intent.getBooleanExtra("isEditMode",false);
        Log.d(TAG, "onCreate: isEditMode: "+isEditMode);

        if(isEditMode){

            adIdForEditing = intent.getStringExtra("adId");
            loadAdDetails();
            binding.toolbarTitleTv.setText("Update Ad");
            binding.postAdBtn.setText("Updated Ad");
        }else{

            binding.toolbarTitleTv.setText("Create Ad");
            binding.postAdBtn.setText("Post Ad");
        }


        imagePickedArrayList = new ArrayList<>();
        loadImage();

        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        binding.toolbarAddImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImagePickOptions();
            }
        });

        binding.locationAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdCreateActivity.this,LocationPickerActivity.class);
                locationPickerActivityLauncher.launch(intent);
            }
        });

        binding.postAdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
    }

    private ActivityResultLauncher<Intent> locationPickerActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    Log.d(TAG, "onActivityResult: ");

                    if (result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();


                        if(data != null){


                            address = data.getStringExtra("address");


                            Log.d(TAG, "onActivityResult: address: "+address);

                            binding.locationAct.setText(address);
                        }

                    }else{

                        Log.d(TAG, "onActivityResult: cancelled");
                        Utils.toast(AdCreateActivity.this,"Cancelled");
                    }


                }
            }
    );


    private void loadImage() {
        adapterImagesPicked = new AdapterImagesPicked(this, imagePickedArrayList,adIdForEditing);
        binding.imagesRv.setAdapter(adapterImagesPicked);
    }

    private void showImagePickOptions() {
        PopupMenu popupMenu = new PopupMenu(this, binding.toolbarAddImageBtn);
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Gallery");
        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 1) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA});
                    } else {
                        requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
                    }
                } else if (item.getItemId() == 2) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pickImageGallery();
                    } else {
                        requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                }
                return true;
            }
        });


    }

    private ActivityResultLauncher<String> requestStoragePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Log.d(TAG, "onActivityResult: isGranted: "+isGranted);
                    if (isGranted) {
                        pickImageGallery();
                    } else {
                        Utils.toast(AdCreateActivity.this, "Storage Permission denied...");
                    }
                }
            }
    );

    private ActivityResultLauncher<String[]> requestCameraPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    Log.d(TAG, "onActivityResult: ");
                    Log.d(TAG, "onActivityResult: "+result.toString());
                    boolean areAllGranted = true;
                    for (Boolean isGranted : result.values()) {
                        areAllGranted = areAllGranted && isGranted;
                    }
                    if (areAllGranted) {
                        pickImageCamera();
                    } else {
                        Utils.toast(AdCreateActivity.this, "Camera or Storage or both permissions denied...");
                    }
                }
            }
    );

    private void pickImageGallery() {
        Log.d(TAG, "pickImageGallery: ");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private void pickImageCamera() {
        Log.d(TAG, "pickImageCamera: ");
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "TEMPORARY_IMAGE");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "TEMPORARY_IMAGE_DESCRIPTION");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: ");
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: imageUre: "+imageUri);
                        String timestamp = "" + Utils.getTimestamp();
                        ModelImagePicked modelImagePicked = new ModelImagePicked(timestamp, imageUri, null, false);
                        imagePickedArrayList.add(modelImagePicked);
                        loadImage();
                    } else {
                        Utils.toast(AdCreateActivity.this, "Cancelled...");
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: ");
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "onActivityResult: imageUri: "+imageUri);
                        String timestamp = "" + Utils.getTimestamp();
                        ModelImagePicked modelImagePicked = new ModelImagePicked(timestamp, imageUri, null, false);
                        imagePickedArrayList.add(modelImagePicked);
                        loadImage();
                    } else {
                        Utils.toast(AdCreateActivity.this, "Cancelled...");
                    }
                }
            }
    );

    private String bookname = "";
    private String category = "";
    private String address = "";
    private String price = "";
    private String title= "";
    private String description = "";


    private void validateData() {
        bookname = binding.bookEt.getText().toString().trim();
        category = binding.categoryAct.getText().toString().trim();
        address = binding.locationAct.getText().toString().trim();
        price = binding.priceEt.getText().toString().trim();
        title = binding.titleEt.getText().toString().trim();
        description = binding.descriptionEt.getText().toString().trim();

        if (bookname.isEmpty()) {
            binding.bookEt.setError("Enter Book Name");
            binding.bookEt.requestFocus();
        }
        else if (category.isEmpty()) {
            binding.categoryAct.setError("Choose Category");
            binding.categoryAct.requestFocus();
        }

        else if (address.isEmpty()) {
            binding.locationAct.setError("Choose Location");
            binding.locationAct.requestFocus();
        }

        else if (title.isEmpty()) {
            binding.titleEt.setError("Enter Title");
            binding.titleEt.requestFocus();
        }

        else if (description.isEmpty()) {
            binding.descriptionEt.setError("Enter Description");
            binding.descriptionEt.requestFocus();
        }

        else if (imagePickedArrayList.isEmpty()) {
            Utils.toast(this, "Pick at least one image");
        }
        else {
            if(isEditMode){
                updateAd();
            }
            else {
                postAd();
            }
        }
    }

    private void postAd() {
        progressDialog.setMessage("Publishing Ad");
        progressDialog.show();

        long timestamp = Utils.getTimestamp();
        DatabaseReference refAds = FirebaseDatabase.getInstance().getReference("Ads");
        String keyId = refAds.push().getKey();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", ""+keyId);
        hashMap.put("uid", ""+ firebaseAuth.getUid());
        hashMap.put("bookname", ""+ bookname);
        hashMap.put("category", ""+ category);
        hashMap.put("address", ""+ address);
        hashMap.put("price",  ""+ price);
        hashMap.put("title", ""+ title);
        hashMap.put("description", ""+ description);
        hashMap.put("status", ""+ Utils.AD_STATUS_AVAILABLE);
        hashMap.put("timestamp",  timestamp);


        refAds.child(keyId)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Ad published...");
                        uploadImageStorage(keyId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        Utils.toast(AdCreateActivity.this, "Failed to publish Ad due to " + e.getMessage());
                    }
                });


    }

    private  void updateAd(){
        Log.d(TAG, "updateAd: ");
        progressDialog.setMessage("Updating Ad...");
        progressDialog.show();

        HashMap<String, Object> hashMap = new HashMap<>();


        hashMap.put("bookname", ""+ bookname);
        hashMap.put("category", ""+ category);
        hashMap.put("address", ""+ address);
        hashMap.put("price",  ""+ price);
        hashMap.put("title", ""+ title);
        hashMap.put("description", ""+ description);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Ads");
        ref.child(adIdForEditing)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: ");
                        progressDialog.dismiss();
                        uploadImageStorage(adIdForEditing);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        Utils.toast(AdCreateActivity.this,"Failed to update Ad due to "+e.getMessage());
                    }
                });

    }

    private void uploadImageStorage(String adId) {
        for (int i = 0; i < imagePickedArrayList.size(); i++) {
            ModelImagePicked modelImagePicked = imagePickedArrayList.get(i);

            if(!modelImagePicked.getFromInternet()){
                String imageName = modelImagePicked.getId();
                String filePathAndName = "Ads/" + imageName;

                StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);

                int finalI = i;
                storageReference.putFile(modelImagePicked.getImageUri())
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                                String message = "Uploading image " + finalI + " of " + imagePickedArrayList.size() + "...\nProgress " + (int) progress + "%";
                                Log.d(TAG, "onProgress: message "+message);
                                progressDialog.setMessage(message);
                                progressDialog.show();
                            }
                        })
                        .addOnSuccessListener(taskSnapshot -> {
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            uriTask.addOnSuccessListener(uploadedImageUrl -> {
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("id", modelImagePicked.getId());
                                hashMap.put("imageUrl", uploadedImageUrl.toString());

                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Ads");
                                ref.child(adId).child("Images").child(imageName).updateChildren(hashMap)
                                        .addOnCompleteListener(task -> {
                                            // Check if this was the last image
                                            if (finalI == imagePickedArrayList.size() - 1) {
                                                progressDialog.dismiss();
                                                Utils.toast(AdCreateActivity.this, "Ad published successfully!");
                                                Log.d(TAG, "All images uploaded and ad published.");
                                            }
                                        });
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "onFailure: ", e);
                            progressDialog.dismiss();
                            Utils.toast(AdCreateActivity.this, "Failed to upload image: " + e.getMessage());
                        });
            }
        }
    }
    private void loadAdDetails(){
        Log.d(TAG, "loadAdDetails: ");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Ads");
        ref.child(adIdForEditing)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        String bookname = ""+snapshot.child("bookname").getValue();
                        String category = ""+snapshot.child("category").getValue();
                        String description = ""+snapshot.child("description").getValue();
                        String price = ""+snapshot.child("price").getValue();
                        String address = ""+snapshot.child("address").getValue();
                        String title = ""+snapshot.child("title").getValue();

                        binding.bookEt.setText(bookname);
                        binding.categoryAct.setText(category);
                        binding.descriptionEt.setText(description);
                        binding.priceEt.setText(price);
                        binding.titleEt.setText(title);
                        binding.locationAct.setText(address);

                        DatabaseReference refImages = snapshot.child("Images").getRef();
                        refImages.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                for (DataSnapshot ds:snapshot.getChildren()){
                                    String id = ""+ds.child("id").getValue();
                                    String imageUrl = ""+ds.child("imageUrl").getValue();

                                    ModelImagePicked modelImagePicked = new ModelImagePicked(id,null,imageUrl,true);
                                    imagePickedArrayList.add(modelImagePicked);

                                }
                                loadImage();

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}
