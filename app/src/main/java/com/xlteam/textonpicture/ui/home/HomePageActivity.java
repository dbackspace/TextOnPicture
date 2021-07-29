package com.xlteam.textonpicture.ui.home;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.xlteam.textonpicture.R;
import com.xlteam.textonpicture.external.utility.animation.ViManager;
import com.xlteam.textonpicture.external.utility.utils.Constant;
import com.xlteam.textonpicture.external.utility.utils.FileUtils;
import com.xlteam.textonpicture.external.utility.utils.Utility;
import com.xlteam.textonpicture.ui.edit.EditPictureActivity;
import com.xlteam.textonpicture.ui.home.created.PictureCreatedDialogFragment;
import com.xlteam.textonpicture.ui.home.firebase.PictureFirebaseDialogFragment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

import static com.xlteam.textonpicture.external.utility.utils.Constant.FILE_PROVIDER_PATH;
import static com.xlteam.textonpicture.external.utility.utils.Constant.REQUEST_CODE_TAKE_PHOTO;
import static com.xlteam.textonpicture.external.utility.utils.Constant.SAVE_DATE_TIME_FORMAT;

public class HomePageActivity extends AppCompatActivity implements DialogInterface.OnDismissListener {
    RecyclerView rvFirebase, rvCreated;
    TextView tvViewMoreFirebase, tvViewMoreCreated;
    TextView tvEmptyCreated;
    LinearLayout layoutTakePhoto, layoutGallery;
    ImageView imgSettings;
    PictureFirebaseDialogFragment pictureFirebaseDialogFragment;
    PictureCreatedDialogFragment pictureCreatedDialogFragment;
    private PictureHomeAdapter createdAdapter;

    private boolean needCheckPermission = false;
    private Uri tempUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        rvFirebase = findViewById(R.id.rv_picture_firebase);
        rvCreated = findViewById(R.id.rv_picture_created);
        tvViewMoreFirebase = findViewById(R.id.view_more_picture_firebase);
        tvViewMoreCreated = findViewById(R.id.view_more_picture_created);
        tvEmptyCreated = findViewById(R.id.tv_empty_picture_created);
        layoutTakePhoto = findViewById(R.id.layout_take_photo);
        layoutGallery = findViewById(R.id.layout_gallery);
        imgSettings = findViewById(R.id.image_settings);

        tvViewMoreCreated.setVisibility(View.GONE);

        updateOrRequestPermissions();
        rvFirebase.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvFirebase.setAdapter(new PictureHomeAdapter(this, Constant.TYPE_PICTURE_FIREBASE, Utility.getUrlPictureHome()));

//        rvCreated.setDrawingCacheEnabled(true);
//        rvCreated.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
//        createdAdapter = new PictureHomeAdapter(HomePageActivity.this, Constant.TYPE_PICTURE_CREATED, FileUtils.getListPathsIfFolderExist());
        rvCreated.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        tvViewMoreFirebase.setOnClickListener(view -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            ViManager.getInstance().setFragmentDefaultAnimation(HomePageActivity.this, fragmentTransaction);
            pictureFirebaseDialogFragment = new PictureFirebaseDialogFragment();
            pictureFirebaseDialogFragment.show(fragmentTransaction, "dialog_firebase");
        });

        tvViewMoreCreated.setOnClickListener(view -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            ViManager.getInstance().setFragmentDefaultAnimation(HomePageActivity.this, fragmentTransaction);
            pictureCreatedDialogFragment = new PictureCreatedDialogFragment();
            pictureCreatedDialogFragment.show(fragmentTransaction, "dialog_created");
        });
        layoutTakePhoto.setOnClickListener(v -> {
            SimpleDateFormat sdf = new SimpleDateFormat(SAVE_DATE_TIME_FORMAT, Locale.getDefault());
            File file = new File(getExternalCacheDir().getPath(), sdf.format(new Date(Utility.now())) + "tempImage.JPEG");
            tempUri = FileProvider.getUriForFile(this, FILE_PROVIDER_PATH, file);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, tempUri);
            startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
        });

        layoutGallery.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), Constant.REQUEST_CODE_PICK_PHOTO_GALLERY);
        });

        imgSettings.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            ViManager.getInstance().setFragmentDefaultAnimation(HomePageActivity.this, fragmentTransaction);
            SettingsDialogFragment settingsDialogFragment = new SettingsDialogFragment();
            settingsDialogFragment.show(fragmentTransaction, "dialog_settings");
        });
    }

    private void updateOrRequestPermissions() {
        List<String> permissionsToRequest = FileUtils.listPermissionStorage(this);
        if (!permissionsToRequest.isEmpty()) {
            Dexter.withContext(this)
                    .withPermissions(
                            permissionsToRequest)
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                            if (multiplePermissionsReport.areAllPermissionsGranted()) {
                                needCheckPermission = false;
                                Timber.e("bug");
                                List<String> listImagePaths = FileUtils.getListPathsIfFolderExist();
                                if (listImagePaths.isEmpty()) {
                                    showRvCreated(false);
                                    showAndSetTextViewEmpty(true, getString(R.string.no_picture));
                                } else {
                                    showRvCreated(true);
                                    showAndSetTextViewEmpty(false, null);
                                }
                            } else {
                                showRvCreated(false);

                                Utility.showDialogRequestPermission(HomePageActivity.this);
                                needCheckPermission = true;
                                if (!isHasAllPermission()) {
                                    String noPermission = getString(R.string.no_permission);
                                    tvEmptyCreated.setVisibility(View.VISIBLE);
                                    tvEmptyCreated.setMovementMethod(LinkMovementMethod.getInstance());
                                    tvEmptyCreated.setText(noPermission, TextView.BufferType.SPANNABLE);
                                    Spannable mySpannable = (Spannable) tvEmptyCreated.getText();
                                    ClickableSpan myClickableSpan = new ClickableSpan() {
                                        @Override
                                        public void onClick(View view) {
                                            updateOrRequestPermissions();
                                        }
                                    };
                                    mySpannable.setSpan(myClickableSpan, 0, noPermission.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                } else {
                                    needCheckPermission = false;
                                }
                            }
                            createdAdapter = new PictureHomeAdapter(HomePageActivity.this, Constant.TYPE_PICTURE_CREATED, FileUtils.getListPathsIfFolderExist());
                            rvCreated.setAdapter(createdAdapter);
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                            permissionToken.continuePermissionRequest();
                        }
                    }).check();
        }
    }

    private boolean isHasAllPermission() {
        return checkReadExternalPermission()
                && checkWriteExternalPermission();
    }

    private boolean checkReadExternalPermission() {
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    private boolean checkWriteExternalPermission() {
        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.REQUEST_CODE_PICK_PHOTO_GALLERY) {
            if (resultCode == RESULT_OK) {
                if (data == null) {
                    Toast.makeText(this, R.string.not_selected_picture, Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(this, EditPictureActivity.class);
                    intent.putExtra(Constant.EXTRA_PICK_PHOTO_URL, data.getData());
                    intent.putExtra(Constant.EXTRA_TYPE_PICTURE, Constant.TYPE_PICK_PHOTO);
                    startActivityForResult(intent, Constant.REQUEST_CODE_PHOTO_FROM_HOME);
                }
            }
        } else if (requestCode == Constant.REQUEST_CODE_TAKE_PHOTO) {
//            Bitmap photo = null;
            if (tempUri != null && resultCode == RESULT_OK) {
                Intent intent = new Intent(this, EditPictureActivity.class);
                intent.putExtra(Constant.EXTRA_PICK_PHOTO_URL, tempUri);
                intent.putExtra(Constant.EXTRA_TYPE_PICTURE, Constant.TYPE_TAKE_PHOTO);
                startActivity(intent);
                tempUri = null;
            } else {
                Timber.e("Xảy ra lỗi khi chụp ảnh");
            }
        } else if (requestCode == Constant.REQUEST_CODE_PHOTO_FROM_HOME) {
            if (resultCode == Activity.RESULT_OK) {
                if (pictureCreatedDialogFragment != null) {
                    pictureCreatedDialogFragment.dismiss();
                }
                if (pictureFirebaseDialogFragment != null) {
                    pictureFirebaseDialogFragment.dismiss();
                }
                updateDataToRecyclerCreated(FileUtils.getListPathsIfFolderExist());
            }
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        updateDataToRecyclerCreated(FileUtils.getListPathsIfFolderExist());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // update rvCreated if has change about permission
        if (needCheckPermission && isHasAllPermission()) {
            List<String> listImagePaths = FileUtils.getListPathsIfFolderExist();
            if (listImagePaths.isEmpty()) {
                showRvCreated(false);
                showAndSetTextViewEmpty(true, getString(R.string.no_picture));
            } else {
                showRvCreated(true);
                showAndSetTextViewEmpty(false, null);
                updateDataToRecyclerCreated(listImagePaths);
            }
        }
    }

    private void showRvCreated(boolean isShowed) {
        rvCreated.setVisibility(isShowed ? View.VISIBLE : View.GONE);
        tvViewMoreCreated.setVisibility(isShowed ? View.VISIBLE : View.GONE);
    }

    private void showAndSetTextViewEmpty(boolean isShowed, String textContent) {
        tvEmptyCreated.setVisibility(isShowed ? View.VISIBLE : View.GONE);
        if (isShowed) {
            tvEmptyCreated.setText(textContent);
        }
    }

    private void updateDataToRecyclerCreated(List<String> listPathsIfFolderExist) {
        if (listPathsIfFolderExist.size() > 0) {
            showRvCreated(true);
            showAndSetTextViewEmpty(false, null);
            createdAdapter.updateList(listPathsIfFolderExist);
            createdAdapter.notifyDataSetChanged();
        } else {
            showRvCreated(false);
            showAndSetTextViewEmpty(true, getString(R.string.no_picture));
        }
    }

    //    @Override
//    protected void onResume() {
//        super.onResume();
//        if (mAdView != null) {
//            mAdView.resume();
//        }
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        if (mAdView != null) {
//            mAdView.pause();
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        AsyncLayoutInflateManager.getInstance(this).onDestroy();
//        if (mAdView != null) {
//            mAdView.destroy();
//        }
//    }
}