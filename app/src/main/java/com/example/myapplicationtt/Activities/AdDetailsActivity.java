package com.example.myapplicationtt.Activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

import com.example.myapplicationtt.R;
import com.example.myapplicationtt.Utils;
import com.example.myapplicationtt.adapters.AdapterImageSlider;
import com.example.myapplicationtt.databinding.ActivityAdDetailsBinding;
import com.example.myapplicationtt.models.ModelAd;
import com.example.myapplicationtt.models.ModelImageSlider;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class AdDetailsActivity extends AppCompatActivity {

    private ActivityAdDetailsBinding binding;
    private static final String TAG = "AD_DETAILS_TAG";
    private FirebaseAuth firebaseAuth;
    private String adId = "";
    private String sellerUid = null;
    private boolean favorite = false;
    private ArrayList<ModelImageSlider> imageSliderArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityAdDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbarBackBtn.setVisibility(View.GONE);
        binding.toolbarDeleteBtn.setVisibility(View.GONE);
        binding.chatBtn.setVisibility(View.GONE);
        binding.slipBtn.setVisibility(View.GONE);

        adId = getIntent().getStringExtra("adId");

        // Check if adId is null
        if (adId == null || adId.isEmpty()) {
            Log.e(TAG, "onCreate: adId is null or empty");
            Utils.toast(this, "Error: Ad ID not provided");
            finish();
            return;
        }

        Log.d(TAG, "onCreate: adId: " + adId);
        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            checkIsFavorite();
        }

        loadAdDetails();
        loadAdImages();

        setupListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupListeners() {
        binding.toolbarBackBtn.setOnClickListener(view -> onBackPressed());

        binding.toolbarDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialAlertDialogBuilder(AdDetailsActivity.this)
                        .setTitle("Delete Ad")
                        .setMessage("Are you sure you want to delete this Ad?")
                        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                deleteAd();
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });


        binding.toolbarEditBtn.setOnClickListener(view -> {

            editOptions();
            // Implement edit action here
        });

        binding.toolbarFavBtn.setOnClickListener(view -> {
            if (favorite) {
                Utils.removeFromFavorite(AdDetailsActivity.this, adId);
            } else {
                Utils.addToFavorite(AdDetailsActivity.this, adId);
            }
        });
        binding.sellerProfileCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdDetailsActivity.this, AdSellerProfileActivity.class);
                intent.putExtra("sellerUid", sellerUid);
                startActivity(intent);

            }
        });
        binding.chatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(AdDetailsActivity.this, ChatActivity.class);
                intent.putExtra("receiptUid",sellerUid);
                startActivity(intent);

            }
        });


        binding.slipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generateAndOpenSlip();

            }
        });

    }
    private void generateAndOpenSlip() {
        // Create PDF slip
        File pdfFile = createPdfSlip();

        // Check if the PDF was created successfully
        if (pdfFile != null && pdfFile.exists()) {
            // Send the PDF file path to PdfViewerActivity
            Intent intent = new Intent(AdDetailsActivity.this, PdfViewerActivity.class);
            intent.putExtra("PDF_FILE_PATH", pdfFile.getAbsolutePath());
            startActivity(intent);
        } else {
            Utils.toast(AdDetailsActivity.this, "Failed to generate slip.");
        }
    }
    private File createPdfSlip() {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        // Use Canvas to draw text and other details on the slip
        Canvas canvas = page.getCanvas();
        TextPaint textPaint = new TextPaint(); // Use TextPaint here instead of Paint
        textPaint.setTextSize(16);

        // Draw a white background
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);

        // Set up the border paint style
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4);

        // Draw a decorative border around the page with some padding
        int padding = 20;
        canvas.drawRect(padding, padding, canvas.getWidth() - padding, canvas.getHeight() - padding, borderPaint);

        // Get the user ID from FirebaseAuth
        String userId = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : "Unknown User";

        // Set text color to black for visibility
        textPaint.setColor(Color.BLACK); // Use TextPaint

        // Define top margin and initial position for text
        int topMargin = 50;
        int x = padding + 20;
        int y = topMargin + 50; // Start text after top margin
        int lineHeight = 25;

        // Title and Date-Time
        String formattedDateTime = Utils.formatTimestampDateTime(Utils.getTimestamp());
        canvas.drawText("Date & Time: " + formattedDateTime, x, y, textPaint); // Draw formatted date and time
        y += lineHeight * 2;

        // Title and Details
        canvas.drawText("Bill Details", x, y, textPaint);
        y += lineHeight * 2;

        // Function to wrap text
        String text = "Title: " + binding.titleTv.getText();
        y = drawMultilineText(canvas, text, x, y, textPaint, canvas.getWidth() - 40); // Wrap text
        text = "Description: " + binding.descriptionTv.getText();
        y = drawMultilineText(canvas, text, x, y, textPaint, canvas.getWidth() - 40); // Wrap text
        text = "Price: ₹" + binding.priceTv.getText();
        y = drawMultilineText(canvas, text, x, y, textPaint, canvas.getWidth() - 40); // Wrap text
        text = "Category: " + binding.categoryTv.getText();
        y = drawMultilineText(canvas, text, x, y, textPaint, canvas.getWidth() - 40); // Wrap text
        text = "Seller: " + binding.sellerNameTv.getText();
        y = drawMultilineText(canvas, text, x, y, textPaint, canvas.getWidth() - 40); // Wrap text
        text = "Seller ID: " + sellerUid;
        y = drawMultilineText(canvas, text, x, y, textPaint, canvas.getWidth() - 40); // Wrap text
        text = "User ID: " + userId;
        y = drawMultilineText(canvas, text, x, y, textPaint, canvas.getWidth() - 40); // Wrap text

        // Draw the book image on the canvas
        try {
            Bitmap bookImage = Glide.with(this)
                    .asBitmap()
                    .load("Images") // Replace with the URL or resource ID for the book image
                    .submit()
                    .get();

            if (bookImage != null) {
                // Scale the image to fit within the page and position it
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bookImage, 200, 200, false);
                y += 20; // Space between text and image
                canvas.drawBitmap(scaledBitmap, x, y, null); // Position the image as desired on the PDF
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        pdfDocument.finishPage(page);

        // Save the PDF to a file
        File outputDir = getCacheDir(); // Using cache directory to save the PDF temporarily
        File outputFile = new File(outputDir, "slip.pdf");

        try {
            pdfDocument.writeTo(new FileOutputStream(outputFile));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            pdfDocument.close();
        }

        return outputFile;
    }
    private int drawMultilineText(Canvas canvas, String text, int x, int y, TextPaint textPaint, int maxWidth) {
        StaticLayout staticLayout = new StaticLayout(
                text,
                textPaint,
                maxWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0.0f,
                false
        );
        canvas.save();
        canvas.translate(x, y);
        staticLayout.draw(canvas);
        canvas.restore();
        return y + staticLayout.getHeight() + 10; // Move the Y position to after the wrapped text
    }




    private void editOptions(){
        Log.d(TAG, "editOptions: ");

        PopupMenu popupMenu = new PopupMenu(this,binding.toolbarEditBtn);
        popupMenu.getMenu().add(Menu.NONE, 0, 0, "Edit");
        popupMenu.getMenu().add(Menu.NONE, 1,1,"Mark As sold");
        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int itemId = menuItem.getItemId();

                if (itemId == 0 ){
                    Intent intent = new Intent(AdDetailsActivity.this, AdCreateActivity.class);
                    intent.putExtra("isEditMode",true);
                    intent.putExtra("adId",adId);
                    startActivity(intent);
                }else if(itemId == 1){

                    showMarkAsSoldDialog();
                }
                return true;
            }
        });
    }
    private void showMarkAsSoldDialog(){
        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(this);
        alertDialogBuilder.setTitle("Mark as Sold")
                .setMessage("Are you sure you want to mark this Ad as sold?")
                .setPositiveButton("SOLD", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {

                        Log.d(TAG, "onClick: Sold Clicked...");

                        HashMap<String,Object> hashMap = new HashMap<>();
                        hashMap.put("status",""+Utils.AD_STATUS_SOLD);

                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Ads");
                        ref.child(adId)
                                .updateChildren(hashMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "onSuccess: Marked as sold");
                                        Utils.toast(AdDetailsActivity.this,"Marked as sold");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "onFailure: ", e);
                                        Utils.toast(AdDetailsActivity.this,"Failed to mark as sold due to "+e.getMessage());

                                    }
                                });

                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        Log.d(TAG, "onClick: Cancel Clicked...");
                        dialog.dismiss();
                    }
                })
                .show();
    }


    private void loadAdDetails() {
        Log.d(TAG, "loadAdDetails: ");
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Ads");
        ref.child(adId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            ModelAd modelAd = snapshot.getValue(ModelAd.class);

                            // Check if modelAd is null
                            if (modelAd == null) {
                                Log.e(TAG, "onDataChange: Ad data is null");
                                Utils.toast(AdDetailsActivity.this, "Error loading ad details");
                                return;
                            }

                            sellerUid = modelAd.getUid();
                            String title = modelAd.getTitle() != null ? modelAd.getTitle() : "No title";
                            String description = modelAd.getDescription() != null ? modelAd.getDescription() : "No description";
                            String address = modelAd.getAddress() != null ? modelAd.getAddress() : "No address";
                            String price = modelAd.getPrice() != null ? modelAd.getPrice() : "No price";
                            String category = modelAd.getCategory() != null ? modelAd.getCategory() : "No Category";
                            long timestamp = modelAd.getTimestamp();

                            String formattedDate = Utils.formatTimestampDate(timestamp);

                            if (sellerUid != null && sellerUid.equals(firebaseAuth.getUid())) {
                                binding.toolbarEditBtn.setVisibility(View.VISIBLE);
                                binding.toolbarDeleteBtn.setVisibility(View.VISIBLE);
                                binding.chatBtn.setVisibility(View.GONE);
                                binding.slipBtn.setVisibility(View.GONE);
                                binding.sellerProfileLabelTv.setVisibility(View.GONE);
                                binding.sellerProfileCv.setVisibility(View.GONE);
                            } else {
                                binding.toolbarEditBtn.setVisibility(View.GONE);
                                binding.toolbarDeleteBtn.setVisibility(View.GONE);
                                binding.chatBtn.setVisibility(View.VISIBLE);
                                binding.slipBtn.setVisibility(View.VISIBLE);
                                binding.sellerProfileLabelTv.setVisibility(View.VISIBLE);
                                binding.sellerProfileCv.setVisibility(View.VISIBLE);
                            }

                            binding.titleTv.setText(title);
                            binding.descriptionTv.setText(description);
                            binding.addressTv.setText(address);
                            binding.priceTv.setText(price);
                            binding.dateTv.setText(formattedDate);
                            binding.categoryTv.setText(category);



                            loadSellerDetails();
                        } catch (Exception e) {
                            Log.e(TAG, "onDataChange: Error loading ad details", e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadAdDetails: Database error", error.toException());
                    }
                });
    }

    private void loadSellerDetails() {
        Log.d(TAG, "loadSellerDetails: ");
        if (sellerUid == null) {
            Log.e(TAG, "loadSellerDetails: sellerUid is null");
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(sellerUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            String name = snapshot.child("name").getValue(String.class);
                            String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                            Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                            // Check for null timestamp and provide a default message
                            String formattedDate;
                            if (timestamp != null && timestamp > 0) {
                                // Format the date using the Utils method or manually
                                formattedDate = Utils.formatTimestampDate(timestamp); // Ensure this returns a string in dd/MM/yyyy format
                            } else {
                                formattedDate = "Not Available"; // or a default message if timestamp is missing
                                Log.e(TAG, "Invalid or missing timestamp for seller.");
                            }

                            binding.sellerNameTv.setText(name != null ? name : "No name");
                            binding.memberSinceLabelTv.setText("Member Since: " + formattedDate); // No format specifier here
                            Glide.with(AdDetailsActivity.this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_person_white)
                                    .into(binding.sellerProfileIv);
                        } catch (Exception e) {
                            Log.e(TAG, "onDataChange: Error loading seller details", e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadSellerDetails: Database error", error.toException());
                    }
                });
    }



    private void checkIsFavorite() {
        Log.d(TAG, "checkIsFavorite: ");
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Favorites").child(adId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        favorite = snapshot.exists();
                        binding.toolbarFavBtn.setImageResource(favorite ? R.drawable.ic_fav_yes : R.drawable.ic_fav_no);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "checkIsFavorite: Database error", error.toException());
                    }
                });
    }

    private void loadAdImages() {
        Log.d(TAG, "loadAdImages: ");
        imageSliderArrayList = new ArrayList<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Ads");
        ref.child(adId).child("Images")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        imageSliderArrayList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ModelImageSlider modelImageSlider = ds.getValue(ModelImageSlider.class);
                            if (modelImageSlider != null) {
                                imageSliderArrayList.add(modelImageSlider);
                            }
                        }
                        AdapterImageSlider adapterImageSlider = new AdapterImageSlider(AdDetailsActivity.this, imageSliderArrayList);
                        binding.imageSliderVp.setAdapter(adapterImageSlider);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadAdImages: Database error", error.toException());
                    }
                });
    }

    private void deleteAd() {
        Log.d(TAG, "deleteAd: ");
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Ads");
        ref.child(adId)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "onSuccess: Ad deleted");
                    Utils.toast(AdDetailsActivity.this, "Ad deleted successfully");
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "onFailure: Error deleting ad", e);
                    Utils.toast(AdDetailsActivity.this, "Failed to delete ad: " + e.getMessage());
                });
    }

}
