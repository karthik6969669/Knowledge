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
import android.widget.PopupMenu;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.myapplicationtt.R;
import com.example.myapplicationtt.Utils;
import com.example.myapplicationtt.databinding.ActivityProfileEditBinding;
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

import java.util.HashMap;
import java.util.Map;

public class profileEditActivity extends AppCompatActivity {

    private ActivityProfileEditBinding binding;

    private static  final String TAG ="PROFILE_EDIT_TAG";
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private Uri imageUri = null;
    private String myUserType = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding=ActivityProfileEditBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());



        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait...");
        progressDialog.setCanceledOnTouchOutside(false);
        firebaseAuth=FirebaseAuth.getInstance();
        loadMyInfo();

        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        binding.profileImagePickFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                imagePickerDialog();
                
            }
        });

        binding.updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ValidateData();
            }
        });

    }
    private String name = "";
    private  String dob = "";
    private String email= "";


    private void ValidateData(){
        name = binding.nameEt.getText().toString().trim();
        dob = binding.dobEt.getText().toString().trim();
        email = binding.emailEt.getText().toString().trim();

        if (imageUri == null){
            updateProfileDb(null);
        }
        else{
            uploadProfileImageStorage();
        }

    }
    private void uploadProfileImageStorage(){
        Log.d(TAG, "uploadProfileImageStorage: ");
        progressDialog.setMessage("Uploading user profile Image...");
        progressDialog.show();

        String filePathAndName = "UserImage/"+"profile_"+firebaseAuth.getUid();

        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
        ref.putFile(imageUri)
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {

                        double progress = (100.0 * snapshot.getBytesTransferred())/ snapshot.getTotalByteCount();
                        Log.d(TAG, "onProgress: Progress "+progress);
                        progressDialog.setMessage("Uploading profile image.progress: " +(int) progress + "%");

                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG, "onSuccess: Uploaded");

                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String uploadedImageUrl = uriTask.getResult().toString();
                        if(uriTask.isSuccessful()){

                            updateProfileDb(uploadedImageUrl);
                        }


                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        Utils.toast(profileEditActivity.this,"Failed to update profile image due to "+ e.getMessage());

                    }
                });
    }
    private void updateProfileDb(String imageUrl){

        progressDialog.setMessage("Updating User Info...");
        progressDialog.show();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("name", "" + name);
        hashMap.put("dob", "" +dob);

        if (imageUrl != null) {

            hashMap.put("profileImageUrl","" +imageUrl);
        }
        if (!myUserType.equalsIgnoreCase("Email") && !myUserType.equalsIgnoreCase("Google")){
            hashMap.put("email", "" +email);
        }

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid())
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Info Updated.");
                        progressDialog.dismiss();
                        Utils.toast(profileEditActivity.this,"Profile Updated...");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        Utils.toast(profileEditActivity.this,"Failed to update due to "+e.getMessage());
                    }
                });

    }
    private void loadMyInfo(){

        Log.d(TAG, "loadMyInfo: ");
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String dob = ""+snapshot.child("dob").getValue();
                        String email = ""+snapshot.child("email").getValue();
                        String name = ""+snapshot.child("name").getValue();
                        String profileImageUrl = ""+snapshot.child("profileImageUrl").getValue();
                        String timestamp = ""+snapshot.child("timestamp").getValue();
                         myUserType = ""+snapshot.child("userType").getValue();

                         if (myUserType.equalsIgnoreCase("Email")|| myUserType.equalsIgnoreCase("Google")){
                             binding.emailTil.setEnabled(false);
                             binding.emailEt.setEnabled(false);
                         }
                         else{

                         }
                         binding.emailEt.setText(email);
                         binding.dobEt.setText(dob);
                         binding.nameEt.setText(name);

                         try{
                             Glide.with(profileEditActivity.this)
                                     .load(profileImageUrl)
                                     .placeholder(R.drawable.ic_person_white)
                                     .into(binding.profileIv);
                         }
                         catch (Exception e){
                             Log.e(TAG, "onDataChange: ", e);
                         }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    private void imagePickerDialog(){
        PopupMenu popupMenu = new PopupMenu(this,binding.profileImagePickFab);
        popupMenu.getMenu().add(Menu.NONE, 1,1,"Camera");
        popupMenu.getMenu().add(Menu.NONE,2,2,"Gallery");
        popupMenu.show();
        
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                
                int itemId = menuItem.getItemId();
                if (itemId == 1){

                    Log.d(TAG, "onMenuItemClick: Camera Clicked, check if camera permission(s) granted or not");
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

                        requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA});

                    }
                    else{
                        requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE});

                    }
                }
                else if(itemId == 2){
                    Log.d(TAG, "onMenuItemClick: Check if Storage permission is granted or not");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

                        pickImageGallery();

                    }
                    else{
                        requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                }
                return false;
            }
        });
    }

    private ActivityResultLauncher<String[]> requestCameraPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    Log.d(TAG, "onActivityResult: "+result.toString());
                    boolean areAllGranted = true;
                    for(boolean isGranted:result.values()){

                        areAllGranted = areAllGranted && isGranted;
                    }
                    if(areAllGranted){
                        Log.d(TAG, "onActivityResult: All Granted e.g. Camera, Storage");
                        pickImageCamera();

                    }
                    else{
                        Log.d(TAG, "onActivityResult: All or either one is denied");
                        Utils.toast(profileEditActivity.this,"Camera or Storage or both permissions denied...");
                    }

                }
            }
    );

    private ActivityResultLauncher<String> requestStoragePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {

                    Log.d(TAG, "onActivityResult: isGranted: "+isGranted);
                    if(isGranted){
                        pickImageGallery();

                    }else{
                        Utils.toast(profileEditActivity.this,"Storage permission denied...!");

                    }
                }
            }

    );

    private void  pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ");

        ContentValues  contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE,"TEMP_TITLE");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"TEMP_DESCRIPTION");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        cameraActivityResultLauncher.launch(intent);


    }
    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Log.d(TAG, "onActivityResult: Image Captured: "+imageUri);

                        try{
                            Glide.with(profileEditActivity.this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.ic_person_white)
                                    .into(binding.profileIv);
                        }catch (Exception e){
                            Log.d(TAG, "onActivityResult: ",e);
                        }
                    }
                    else{
                        Utils.toast(profileEditActivity.this,"Cancelled...");
                    }

                }
            }
    );

    private void  pickImageGallery(){
        Log.d(TAG, "pickImageGallery: ");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);

    }
    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        imageUri = data.getData();

                        Log.d(TAG, "onActivityResult: Image Picked From Gallery: "+imageUri);

                        try{
                            Glide.with(profileEditActivity.this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.ic_person_white)
                                    .into(binding.profileIv);
                        }
                        catch (Exception e){
                            Log.d(TAG, "onActivityResult: ",e);
                        }


                    }else{
                        Utils.toast(profileEditActivity.this,"Cancelled....");
                    }

                }
            }
    );

}