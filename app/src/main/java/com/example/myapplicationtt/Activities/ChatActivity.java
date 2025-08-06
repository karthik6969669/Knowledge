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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplicationtt.R;
import com.example.myapplicationtt.Utils;
import com.example.myapplicationtt.adapters.AdapterChat;
import com.example.myapplicationtt.databinding.ActivityChatBinding;
import com.example.myapplicationtt.models.ModelChat;
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

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private static final String TAG = "CHAT_TAG";
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;

    private String receiptUid = "";
    private String myUid = "";
    private String chatPath = "";
    private Uri imageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        receiptUid = getIntent().getStringExtra("receiptUid");
        myUid = firebaseAuth.getUid();

        chatPath = Utils.chatPath(receiptUid, myUid);
        Log.d(TAG, "onCreate: receiptUid: " + receiptUid);
        Log.d(TAG, "onCreate: myUid: " + myUid);
        Log.d(TAG, "onCreate: chatPath: " + chatPath);

        loadReceiptDetails();
        loadMessages();

        binding.toolbarBackBtn.setOnClickListener(view -> finish());

        binding.attachFab.setOnClickListener(view -> imagePickDialog());

        // Add listener to detect "Send" action on mobile keyboard
        binding.messageEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                validateData();  // Calls the function to send the message
                return true;
            }
            return false;
        });

        binding.sendBtn.setOnClickListener(view -> validateData());
    }

    private void loadReceiptDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(receiptUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("name").getValue(String.class);
                        String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                        binding.toolbarTitleTv.setText(name);
                        Glide.with(ChatActivity.this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_person_gray)
                                .error(R.drawable.ic_image_broken_gray)
                                .into(binding.toolbarProfileIv);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void loadMessages() {
        ArrayList<ModelChat> chatArrayList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Chats");
        ref.child(chatPath)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        chatArrayList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ModelChat modelChat = ds.getValue(ModelChat.class);
                            chatArrayList.add(modelChat);
                        }
                        AdapterChat adapterChat = new AdapterChat(ChatActivity.this, chatArrayList);
                        binding.chatRv.setAdapter(adapterChat);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void imagePickDialog() {
        PopupMenu popupMenu = new PopupMenu(this, binding.attachFab);
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Gallery");

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == 1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA});
                } else {
                    requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
                }
            } else if (itemId == 2) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pickImageGallery();
                } else {
                    requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
            return true;
        });

        popupMenu.show();
    }

    private ActivityResultLauncher<String[]> requestCameraPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean areAllGranted = true;
                for (Boolean isGranted : result.values()) {
                    if (!isGranted) {
                        areAllGranted = false;
                        break;
                    }
                }

                if (areAllGranted) {
                    pickImageCamera();
                } else {
                    Utils.toast(ChatActivity.this, "Camera or Storage permissions denied!");
                }
            }
    );

    private ActivityResultLauncher<String> requestStoragePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    pickImageGallery();
                } else {
                    Utils.toast(ChatActivity.this, "Storage permission denied!");
                }
            }
    );

    private void pickImageCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "CHAT_IMAGE_TEMP");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "CHAT_IMAGE_TEMP_DESCRIPTION");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    uploadToFirebaseStorage();
                } else {
                    Utils.toast(ChatActivity.this, "Cancelled.");
                }
            }
    );

    private void pickImageGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    imageUri = result.getData().getData();
                    uploadToFirebaseStorage();
                } else {
                    Utils.toast(ChatActivity.this, "Cancelled.");
                }
            }
    );

    private void uploadToFirebaseStorage() {
        progressDialog.setMessage("Uploading image...");
        progressDialog.show();

        long timestamp = Utils.getTimestamp();
        String filePathAndName = "ChatImages/" + timestamp;

        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
        storageReference.putFile(imageUri)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressDialog.setMessage("Uploading image..Progress: " + (int) progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful()) ;
                    String imageUrl = uriTask.getResult().toString();
                    sendMessage(Utils.MESSAGE_TYPE_IMAGE, imageUrl, timestamp);
                })
                .addOnFailureListener(e -> {
                    Utils.toast(ChatActivity.this, "Failed to upload: " + e.getMessage());
                    progressDialog.dismiss();
                });
    }

    private void validateData() {
        String message = binding.messageEt.getText().toString().trim();
        if (message.isEmpty()) {
            Utils.toast(this, "Type Message to send...");
        } else {
            sendMessage(Utils.MESSAGE_TYPE_TEXT, message, Utils.getTimestamp());
        }
    }

    private void sendMessage(String messageType, String message, long timestamp) {
        progressDialog.setMessage("Sending...");
        progressDialog.show();

        DatabaseReference refChat = FirebaseDatabase.getInstance().getReference("Chats");
        String keyId = refChat.push().getKey();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("messageId", keyId);
        hashMap.put("messageType", messageType);
        hashMap.put("message", message);
        hashMap.put("fromUid", myUid);
        hashMap.put("toUid", receiptUid);
        hashMap.put("timestamp", timestamp);

        refChat.child(chatPath).child(keyId)
                .setValue(hashMap)
                .addOnSuccessListener(unused -> {
                    binding.messageEt.setText("");
                    progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Utils.toast(ChatActivity.this, "Failed to send: " + e.getMessage());
                    progressDialog.dismiss();
                });
    }
}
