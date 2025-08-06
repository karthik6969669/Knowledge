package com.example.myapplicationtt;

import android.content.Context;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class Utils {

    public static final String AD_STATUS_AVAILABLE = "AVAILABLE";
    public static final String AD_STATUS_SOLD = "SOLD";
    public static final String MESSAGE_TYPE_TEXT="TEXT";
    public static final String MESSAGE_TYPE_IMAGE="IMAGE";

    public static final String[] categories = {
            "Mystery",
            "Science",
            "History",
            "Health and fitness",
            "Horror",
            "Thriller",
            "Biography",
            "Mystery and suspense"
    };

    public static final int[] categoryIcons = {
            R.drawable.ic_category_book,
            R.drawable.ic_category_science,
            R.drawable.ic_category_histroy,
            R.drawable.ic_catogory_fitness,
            R.drawable.ic_category_medication,
            R.drawable.ic_category_book,
            R.drawable.ic_category_book,
            R.drawable.ic_category_book
    };

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static long getTimestamp() {
        return System.currentTimeMillis();
    }

    public static String formatTimestampDate(Long timestamp) {
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(timestamp);
        return DateFormat.format("dd/MM/yyyy", calendar).toString();
    }

    public static String formatTimestampDateTime(Long timestamp) {
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(timestamp);
        return DateFormat.format("dd/MM/yyyy hh:mm:a", calendar).toString();
    }


    public static void addToFavorite(Context context, String adId) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            Utils.toast(context, "You're not logged in ...");
        } else {
            long timestamp = Utils.getTimestamp();

            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("adId", adId);
            hashMap.put("timestamp", timestamp);

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorites").child(adId)
                    .setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Utils.toast(context, "Added to favorites.");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Utils.toast(context, "Failed to add to favorites due to " + e.getMessage());
                        }
                    });
        }
    }

    public static String chatPath(String receiptUid,String yourUid){
        String[] arrayUids = new String[]{receiptUid,yourUid};

        Arrays.sort(arrayUids);
        String chatPath = arrayUids[0]+ "_" +arrayUids[1];

        return chatPath;
    }

    public static void removeFromFavorite(Context context, String adId) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            Utils.toast(context, "You're not logged in...");
        } else {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorites").child(adId)
                    .removeValue()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Utils.toast(context, "Removed from favorites.");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Utils.toast(context, "Failed to remove due to " + e.getMessage());
                        }
                    });
        }
    }

    

    }

