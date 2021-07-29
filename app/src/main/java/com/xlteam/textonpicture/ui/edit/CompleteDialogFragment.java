package com.xlteam.textonpicture.ui.edit;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.xlteam.textonpicture.R;
import com.xlteam.textonpicture.external.utility.utils.FileUtils;
import com.xlteam.textonpicture.ui.commondialog.DialogSaveChangesBuilder;
import com.xlteam.textonpicture.ui.home.HomePageActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.UUID;

public class CompleteDialogFragment extends DialogFragment {

    private Context mContext;
    private Bitmap mBitmap;
    private Dialog mDialogBack;
    private ImageView imgSavedPicture, imgBack, imgShare;
    private AdView mAdView;
    private TextView tvSavedPath, tvSaveSuccess;
    private LinearLayout layoutSuccess;
    private ProgressBar loading;

    public CompleteDialogFragment() {

    }

    public static CompleteDialogFragment newInstance(Bitmap bitmap) {
        CompleteDialogFragment completeDialogFragment = new CompleteDialogFragment();
        Bundle args = new Bundle();
        if (bitmap != null) args.putParcelable("bitmap_picture", bitmap);
        completeDialogFragment.setArguments(args);
        return completeDialogFragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_TextOnPicture);
        if (getArguments() != null) {
            mBitmap = getArguments().getParcelable("bitmap_picture");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(mContext, getTheme()) {
            @Override
            public void onBackPressed() {
                showDialogBack(mContext);
            }
        };
        Objects.requireNonNull(dialog.getWindow()).requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.dialog_complete, container, false);
        MobileAds.initialize(mContext, initializationStatus -> {
        });
        imgSavedPicture = root.findViewById(R.id.image_saved_picture);
        imgBack = root.findViewById(R.id.image_back);
        imgShare = root.findViewById(R.id.image_share);
        mAdView = root.findViewById(R.id.adView);
        tvSavedPath = root.findViewById(R.id.tvSavedPath);
        layoutSuccess = root.findViewById(R.id.layout_success);
        tvSaveSuccess = root.findViewById(R.id.tvSaveSuccess);
        loading = root.findViewById(R.id.loading);

        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        imgSavedPicture.setImageBitmap(mBitmap);
        imgBack.setOnClickListener(view -> showDialogBack(mContext));

        imgShare.setOnClickListener(view -> {
            //share ảnh
        });

        new Thread(() -> {
            String nameImage = UUID.randomUUID().toString();
            Result result = savePhotoToExternalStorage(mContext, nameImage, mBitmap);
            if (result.result) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    tvSavedPath.setText(mContext.getString(R.string.file_path, result.path + File.separator + nameImage + ".jpg"));
                    layoutSuccess.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.GONE);
                });
            } else {
                new Handler(Looper.getMainLooper()).post(() -> {
                    layoutSuccess.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.GONE);
                    tvSaveSuccess.setText(R.string.save_fail);
                    tvSavedPath.setText(R.string.try_again);
                });
            }
        }).start();
        return root;
    }

    private void showDialogBack(Context context) {
        if (mDialogBack == null)
            mDialogBack = DialogSaveChangesBuilder.create(context).setTitleMessage(getString(R.string.do_you_want_to_back))
                    .setFirstButton(view -> mDialogBack.dismiss(), getString(R.string.cancel))
                    .setSecondButton(view -> dismiss(), getString(R.string.back))
                    .setThirdButton(view -> {
                        Intent intent = new Intent(mContext, HomePageActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                        ((Activity) mContext).finish();
                    }, getString(R.string.home)).build();
        mDialogBack.show();
    }

    private Result savePhotoToExternalStorage(Context context, String imageName, Bitmap bmp) {
        OutputStream fos;
        Uri imageCollection;
        ContentValues contentValues = new ContentValues();
        Result result = new Result();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + "TextOnPicture");
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, imageName + ".jpg");
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.Images.Media.WIDTH, bmp.getWidth());
            contentValues.put(MediaStore.Images.Media.HEIGHT, bmp.getHeight());

            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = contentResolver.insert(imageCollection, contentValues);
            try {
                fos = contentResolver.openOutputStream(Objects.requireNonNull(uri));
                result.result = bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                result.path = contentValues.getAsString(MediaStore.Images.Media.RELATIVE_PATH);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            File saveFolder = FileUtils.findFolderSaveImage();
            String savedPath = saveFolder.getAbsolutePath() + File.separator + imageName + ".jpg";
            result.path = saveFolder.getAbsolutePath();
            File savedFile = new File(savedPath);
            try {
                fos = new FileOutputStream(savedFile);
                result.result = bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private static class Result {
        boolean result;
        String path;

        public Result() {
            result = false;
            path = "";
        }
    }
}
