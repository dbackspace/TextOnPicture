package com.xlteam.textonpicture.ui.edit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.xlteam.textonpicture.R;
import com.xlteam.textonpicture.external.datasource.ColorDataSource;
import com.xlteam.textonpicture.external.utility.colorpicker.ColorPickerDialog;
import com.xlteam.textonpicture.external.utility.customview.ClipArt;
import com.xlteam.textonpicture.external.utility.logger.Log;
import com.xlteam.textonpicture.external.utility.utils.Constant;
import com.xlteam.textonpicture.external.utility.utils.FileUtils;
import com.xlteam.textonpicture.external.utility.utils.Utility;

import java.util.List;

import timber.log.Timber;

public class EditPictureActivity extends AppCompatActivity
        implements
        DialogAddTextBuilder.Callback,
        ClipArt.CallbackListener {
    private ImageView imgBack, imgCancelText, imgDoneText;
    private TextView tvDone;
    private ImageView mImgBackground;
    private RelativeLayout layoutText;

    private AdView mAdView;
    public static final int PICK_IMAGE = 1;

    private Context mContext;

    // relative background
    private RelativeLayout relativeBackground;

    // Text editor
    private RecyclerView rvFont;
    private FontAdapter mFontAdapter;

    //color
    private RelativeLayout layoutOpacityColor;
    private RecyclerView rvColor;
    private SeekBar sbOpacity;
    private TextView tvValueOpacity;
    private RelativeLayout layoutShadow;
    private SeekBar sbSaturationShadow, sbOpacityShadow;
    private TextView tvValueSaturationShadow, tvValueOpacityShadow;
    private ImageView imgShadowLeft, imgShadowRight, imgShadowTop, imgShadowBottom, imgShadowCenter;
    private ColorAdapter mColorAdapter;

    //align
    private RelativeLayout layoutAlign;
    private ImageView imgAlignCenter;
    private ImageView imgAlignLeft;
    private ImageView imgAlignRight;

    // text current
    private ClipArt currentClipArt;
    private RecyclerView rvToolText;
    private ToolTextAdapter mToolTextAdapter;
    // border for text
//    private View currentViewOfText;
    private ClipArt previousViewClipArt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // init view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_picture);
        mContext = getBaseContext();
        MobileAds.initialize(this);
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        findViewById();
        relativeBackground.setOnClickListener(v -> {
            if (layoutText.getVisibility() == View.VISIBLE) {
                showTextMode(false);
            }
        });

        loadImageIntoImageBackground();

        initToolText();
        tvDone.setOnClickListener(v -> {
            if (Utility.isValidClick(v.getId()))
                saveImageCreatedToSdcard(relativeBackground);
        });
        imgBack.setOnClickListener(v -> {
            Intent intent = getIntent();
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
        imgCancelText.setOnClickListener(view -> showTextMode(false));
        imgDoneText.setOnClickListener(view -> showTextMode(false));
    }

    private void loadImageIntoImageBackground() {
        Intent intent = getIntent();
        int type = intent.getIntExtra(Constant.EXTRA_TYPE_PICTURE, -1);
        if (type == Constant.TYPE_PICK_PHOTO) {
            Uri imageUri = intent.getParcelableExtra(Constant.EXTRA_PICK_PHOTO_URL);
            if (imageUri != null) {
                loadImageFromUri(imageUri, mImgBackground);
            }
        } else if (type == Constant.TYPE_TAKE_PHOTO) {
            Uri photo = intent.getParcelableExtra(Constant.EXTRA_TAKE_PHOTO_BITMAP);

            if (photo != null) {
                loadImageFromUri(photo, mImgBackground);
            }
        } else {
            String url = intent.getStringExtra(Constant.EXTRA_URL_PICTURE);
            if (type == Constant.TYPE_PICTURE_FIREBASE) {
                loadImageFromString(url, mImgBackground);
            } else if (type == Constant.TYPE_PICTURE_CREATED) {
                loadImageFromString("file://" + url, mImgBackground);
            }
        }
    }

    private void loadImageFromString(String url, ImageView imageView) {
        Glide.with(this)
                .load(url)
                .fitCenter()
                .into(imageView);
    }

    private void loadImageFromUri(Uri photo, ImageView imageView) {
        Glide.with(this)
                .load(photo)
                .fitCenter()
                .into(imageView);
    }

    private void findViewById() {
        imgBack = findViewById(R.id.btn_edit_back_and_cancel);
        tvDone = findViewById(R.id.tv_edit_save);
        mImgBackground = findViewById(R.id.img_edit_background);

        relativeBackground = findViewById(R.id.relative_background_save_img);

        //text editor
        rvToolText = findViewById(R.id.rv_tool_text);
        layoutText = findViewById(R.id.layout_text);
        imgCancelText = findViewById(R.id.btn_cancel_edit_text);
        imgDoneText = findViewById(R.id.image_save_text);

        rvFont = findViewById(R.id.rvFont);
        //color
        rvColor = findViewById(R.id.rvColor);
        layoutOpacityColor = findViewById(R.id.layout_opacity_color);
        sbOpacity = findViewById(R.id.sb_opacity);
        tvValueOpacity = findViewById(R.id.tv_value_opacity);
        layoutShadow = findViewById(R.id.layout_shadow);
        sbSaturationShadow = findViewById(R.id.sb_saturation_shadow);
        sbOpacityShadow = findViewById(R.id.sb_opacity_shadow);
        tvValueSaturationShadow = findViewById(R.id.tv_value_saturation_shadow);
        tvValueOpacityShadow = findViewById(R.id.tv_value_opacity_shadow);
        imgShadowTop = findViewById(R.id.image_shadow_top);
        imgShadowBottom = findViewById(R.id.image_shadow_bottom);
        imgShadowLeft = findViewById(R.id.image_shadow_left);
        imgShadowRight = findViewById(R.id.image_shadow_right);
        imgShadowCenter = findViewById(R.id.image_shadow_center);

        //align
        layoutAlign = findViewById(R.id.layout_align);
        imgAlignLeft = findViewById(R.id.image_align_left);
        imgAlignCenter = findViewById(R.id.image_align_center);
        imgAlignRight = findViewById(R.id.image_align_right);

    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        switch (reqCode) {
            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    Uri resultUri = result.getUri();
                    mImgBackground.setImageURI(resultUri);
                }
                break;
            case PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    if (data == null) {
                        Toast.makeText(this, R.string.not_selected_picture, Toast.LENGTH_SHORT).show();
                    } else {
                        if (data.getData() != null) {
                            final Uri imageUri = data.getData();
                            loadImageFromUri(imageUri, mImgBackground);
                        } else {
                            Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_LONG).show();
                        }
                    }
                }
                break;
        }
    }

    public void onOpenTextEditorClicked(View view) {
        Dialog addTextDialog = new DialogAddTextBuilder(this, this, null, Utility.getBitmapFromView(relativeBackground)).build();
        addTextDialog.show();
    }

    public void onAddTextClicked() {
        Dialog addTextDialog = new DialogAddTextBuilder(this, this, null, Utility.getBitmapFromView(relativeBackground)).build();
        addTextDialog.show();
    }

    public void onAddImageClicked(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), PICK_IMAGE);
    }

    public void onEditPictureClicked(View view) {
        // start cropping activity for pre-acquired image saved on the device
        CropImage.activity(Utility.getImageUri(getApplicationContext(), getBitmapFromImageView(mImgBackground)))
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }

    public Bitmap getBitmapFromImageView(ImageView imageView) {
        imageView.invalidate();
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        return drawable.getBitmap();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAdView != null) {
            mAdView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdView != null) {
            mAdView.destroy();
        }
    }

    // save image
    private void saveImageCreatedToSdcard(View view) {
        List<String> permissionsToRequest = FileUtils.listPermissionStorage(this);
        if (permissionsToRequest.isEmpty()) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            CompleteDialogFragment completeDialogFragment = CompleteDialogFragment.newInstance(Utility.getBitmapFromView(view));
            completeDialogFragment.show(fragmentTransaction, "dialog_complete");

        }else{
            Dexter.withContext(this)
                    .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                            FragmentManager fragmentManager = getSupportFragmentManager();
                            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                            CompleteDialogFragment completeDialogFragment = CompleteDialogFragment.newInstance(Utility.getBitmapFromView(view));
                            completeDialogFragment.show(fragmentTransaction, "dialog_complete");
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                            Toast.makeText(mContext, getString(R.string.notify_not_enough_permission), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                            permissionToken.continuePermissionRequest();
                        }
                    }).check();
        }
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onSaveClicked(String text, boolean isEditOldText) {
        /*
          Không tạo empty text.
          TODO: Xử lý trong textChange ở DialogAddText, có cho enable nút DONE hay không
         */
        if (TextUtils.isEmpty(text.trim())) return;

        if (isEditOldText) {
            currentClipArt.setText(text);
            return;
        }
        if (currentClipArt != null) {
            previousViewClipArt = currentClipArt;
        }
        currentClipArt = new ClipArt(this, text);

        showTextMode(true);
        addViewToParent(currentClipArt);
    }

    private void addViewToParent(ClipArt viewOfText) {
//        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
//        viewOfText.setLayoutParams(params);
        relativeBackground.addView(viewOfText);
    }

    @Override
    public void onBackPressed() {
        if (layoutText.getVisibility() == View.VISIBLE) {
            //hỏi xem có lưu thay đổi
            showTextMode(false);
        } else {
            Intent intent = getIntent();
            setResult(Activity.RESULT_OK, intent);
            super.onBackPressed();
        }
    }

    private void isChangedListener() {
    }

    private void showTextMode(boolean isShow) {
        if (isShow) {
            layoutText.setVisibility(View.VISIBLE);
            if (currentClipArt != null) {
                showToolAndBorderOfText(true);
                currentClipArt.visibleAll();
            }
        } else {
            layoutText.setVisibility(View.GONE);
            previousViewClipArt = null;
            if (currentClipArt != null) {
                showToolAndBorderOfText(false);
                currentClipArt.disableAll();
                currentClipArt = null;
            }
        }
    }

    private void showToolAndBorderOfText(boolean isShow) {
        if (isShow && previousViewClipArt != null && previousViewClipArt != currentClipArt) {
            previousViewClipArt.disableAll();
        }

        if (isShow && currentClipArt != null) {
            currentClipArt.visibleAll();
            //update current status of tool
            updateCurrentStatusForTool();
        } else {
            if (currentClipArt != null) {
                currentClipArt.disableAll();
            }
        }
    }

    private void setColorForImageView(ImageView img, int colorId) {
        ImageViewCompat.setImageTintList(img, ColorStateList.valueOf(ContextCompat.getColor(this, colorId)));
    }

    private void initToolText() {
        rvToolText.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        mToolTextAdapter = new ToolTextAdapter(this, number -> {
            switch (number) {
                case 0:
                    onAddTextClicked();
                    break;
                case 1:
                    onTextColorClicked();
                    break;
                case 2:
                    onTextBgColorClicked();
                    break;
                case 3:
                    onTextShadowClicked();
                    break;
                case 4:
                    onTextFontClicked();
                    break;
                case 5:
                    onTextAlignClicked();
                    break;

            }
        });
        rvToolText.setAdapter(mToolTextAdapter);
    }

    public void onTextColorClicked() {
        if (mColorAdapter == null) initColorTool();
        layoutOpacityColor.setVisibility(View.VISIBLE);
        rvColor.setVisibility(View.VISIBLE);
        mColorAdapter.setNoColor(false);
        rvFont.setVisibility(View.GONE);
        layoutAlign.setVisibility(View.GONE);
        layoutShadow.setVisibility(View.GONE);
        if (currentClipArt != null) {
            sbOpacity.setProgress(currentClipArt.getOpacityText());
        }
    }

    public void onTextBgColorClicked() {
        if (mColorAdapter == null) initColorTool();
        layoutOpacityColor.setVisibility(View.VISIBLE);
        rvColor.setVisibility(View.VISIBLE);
        mColorAdapter.setNoColor(true);
        rvFont.setVisibility(View.GONE);
        layoutShadow.setVisibility(View.GONE);
        layoutAlign.setVisibility(View.GONE);
        if (currentClipArt != null) {
            sbOpacity.setProgress(currentClipArt.getOpacityBackground());
        }
    }

    public void onTextShadowClicked() {
        if (mColorAdapter == null) initColorTool();
        if (!isInitShadowTool) {
            initShadowTool();
            isInitShadowTool = true;
        }
        rvColor.setVisibility(View.VISIBLE);
        mColorAdapter.setNoColor(true);
        layoutShadow.setVisibility(View.VISIBLE);
        rvFont.setVisibility(View.GONE);
        layoutAlign.setVisibility(View.GONE);
        layoutOpacityColor.setVisibility(View.GONE);
        if (currentClipArt != null) {
            sbOpacityShadow.setProgress(currentClipArt.getOpacityShadow());
            sbSaturationShadow.setProgress(currentClipArt.getSaturationShadow());
        }
    }

    public void onTextFontClicked() {
        if (mFontAdapter == null) initFontTool();
        rvFont.setVisibility(View.VISIBLE);
        layoutOpacityColor.setVisibility(View.GONE);
        rvColor.setVisibility(View.GONE);
        layoutShadow.setVisibility(View.GONE);
        layoutAlign.setVisibility(View.GONE);
        int fontClipArt = currentClipArt.getFont();
        Timber.e(mFontAdapter.getNumberFont() + " " + fontClipArt);
        if (currentClipArt != null && mFontAdapter.getNumberFont() != fontClipArt) {
            mFontAdapter.setNumberFont(fontClipArt);
            mFontAdapter.notifyDataSetChanged();
        }
        rvFont.smoothScrollToPosition(fontClipArt);
    }

    public void onTextAlignClicked() {
        if (!isInitAlignTool) {
            initAlignTool();
            isInitAlignTool = true;
        }
        layoutAlign.setVisibility(View.VISIBLE);
        rvFont.setVisibility(View.GONE);
        layoutOpacityColor.setVisibility(View.GONE);
        layoutShadow.setVisibility(View.GONE);
        rvColor.setVisibility(View.GONE);
        if (currentClipArt != null) setIconGravity(currentClipArt.getGravity());
    }

    interface SimpleSeekBarChangeListener extends SeekBar.OnSeekBarChangeListener {
        @Override
        default void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        @Override
        default void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        default void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    public void initColorTool() {
        rvColor.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        mColorAdapter = new ColorAdapter(new ColorAdapter.ColorSelectCallback() {
            @Override
            public void selectColor(int color) {
                String colorCSS = ColorDataSource.getInstance().getAllData().get(color);
                switch (mToolTextAdapter.getCurrentNumberTool()) {
                    case 1:
                        int opacityText = currentClipArt.getOpacityText();
                        currentClipArt.setColorText(colorCSS);
                        if (opacityText == 0) sbOpacity.setProgress(100);
                        // nếu màu trong suốt thì trả lại 100% màu
                        break;
                    case 2:
                        int opacityBackground = currentClipArt.getOpacityBackground();
                        currentClipArt.setColorBackground(colorCSS);
                        if (opacityBackground == 0) sbOpacity.setProgress(20);
                        break;
                    case 3:
                        int opacityShadow = currentClipArt.getOpacityShadow();
                        currentClipArt.setColorShadow(colorCSS);
                        if (opacityShadow == 0) sbOpacityShadow.setProgress(100);
                        break;
                }
            }

            @Override
            public void setNoColor() {
                switch (mToolTextAdapter.getCurrentNumberTool()) {
                    case 2:
                        sbOpacity.setProgress(0);
                        break;
                    case 3:
                        sbOpacityShadow.setProgress(0);
                        break;
                }
            }

            @Override
            public void pickColor() {
                String color = "#000000";
                int position = mToolTextAdapter.getCurrentNumberTool();
                if (currentClipArt != null) {
                    switch (position) {
                        case 1:
                            color = "#" + currentClipArt.getColorText();
                            break;
                        case 2:
                            color = "#" + currentClipArt.getColorBackground();
                            break;
                        case 3:
                            color = "#" + currentClipArt.getColorShadow();
                            break;
                    }
                }

                ColorPickerDialog dialog = new ColorPickerDialog(EditPictureActivity.this, Color.parseColor(color), false, new ColorPickerDialog.OnColorPickerListener() {
                    @Override
                    public void onCancel(ColorPickerDialog dialog) {
                    }

                    @Override
                    public void onApply(ColorPickerDialog dialog, String color) {
                        Log.d("binh.ngk ", " color = " + color);
                        switch (position) {
                            case 1:
                                currentClipArt.setColorText(color);
                                if (currentClipArt.getOpacityText() == 0)
                                    sbOpacity.setProgress(100);
                                // nếu màu trong suốt thì trả lại 100% màu
                                break;
                            case 2:
                                currentClipArt.setColorBackground(color);
                                if (currentClipArt.getOpacityBackground() == 0)
                                    sbOpacity.setProgress(20);
                                break;
                            case 3:
                                currentClipArt.setColorShadow(color);
                                if (currentClipArt.getOpacityShadow() == 0)
                                    sbOpacityShadow.setProgress(100);
                                break;
                        }
                    }
                });

                dialog.show();
            }
        });
        rvColor.setAdapter(mColorAdapter);
        sbOpacity.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvValueOpacity.setText(progress + "%");
                switch (mToolTextAdapter.getCurrentNumberTool()) {
                    case 1:
                        currentClipArt.setOpacityText(progress);
                        break;
                    case 2:
                        currentClipArt.setOpacityBackground(progress);
                        break;
                }
            }
        });
    }

    public void initFontTool() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        int DEFAULT_FONT_NUMBER = 8;
        linearLayoutManager.scrollToPositionWithOffset(DEFAULT_FONT_NUMBER, 20);
        rvFont.setLayoutManager(linearLayoutManager);
        mFontAdapter = new FontAdapter(this, numberFont -> currentClipArt.setFont(numberFont));
        mFontAdapter.setNumberFont(DEFAULT_FONT_NUMBER);
        rvFont.setAdapter(mFontAdapter);
    }

    boolean isInitShadowTool = false;

    private void initShadowTool() {
        sbSaturationShadow.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvValueSaturationShadow.setText(progress + "%");
                currentClipArt.setSaturationShadow(progress);
            }
        });

        sbOpacityShadow.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvValueOpacityShadow.setText(progress + "%");
                currentClipArt.setOpacityShadow(progress);
            }
        });

        View.OnClickListener shadowArrowClick = v -> {
            if (currentClipArt == null) return;
            float dx = currentClipArt.getDxShadow(), dy = currentClipArt.getDyShadow();
            switch (v.getId()) {
                case R.id.image_shadow_left:
                    if (dx >= -10) currentClipArt.setDxShadow(dx - 1f);
                    else return;
                    break;
                case R.id.image_shadow_right:
                    if (dx <= 10) currentClipArt.setDxShadow(dx + 1f);
                    else return;
                    break;
                case R.id.image_shadow_top:
                    if (dy >= -10) currentClipArt.setDyShadow(dy - 1f);
                    else return;
                    break;
                case R.id.image_shadow_bottom:
                    if (dy <= 10) currentClipArt.setDyShadow(dy + 1f);
                    else return;
                    break;
                case R.id.image_shadow_center:
                    currentClipArt.setDxShadow(0f);
                    currentClipArt.setDyShadow(0f);
                    break;

            }
        };
        imgShadowLeft.setOnClickListener(shadowArrowClick);
        imgShadowRight.setOnClickListener(shadowArrowClick);
        imgShadowTop.setOnClickListener(shadowArrowClick);
        imgShadowBottom.setOnClickListener(shadowArrowClick);
        imgShadowCenter.setOnClickListener(shadowArrowClick);
    }

    boolean isInitAlignTool = false;

    private void initAlignTool() {
        imgAlignRight.setOnClickListener(v -> {
            currentClipArt.setGravity(Gravity.END);
            setIconGravity(Gravity.END);
        });

        imgAlignLeft.setOnClickListener(v -> {
            currentClipArt.setGravity(Gravity.START);
            setIconGravity(Gravity.START);
        });

        imgAlignCenter.setOnClickListener(v -> {
            currentClipArt.setGravity(Gravity.CENTER);
            setIconGravity(Gravity.CENTER);
        });
    }

    private void setIconGravity(int gravity) {
        switch (gravity) {
            case Gravity.START:
                setColorForImageView(imgAlignLeft, R.color.color_3cc2f5_legend);
                setColorForImageView(imgAlignCenter, R.color.white);
                setColorForImageView(imgAlignRight, R.color.white);
                break;
            case Gravity.CENTER:
                setColorForImageView(imgAlignLeft, R.color.white);
                setColorForImageView(imgAlignCenter, R.color.color_3cc2f5_legend);
                setColorForImageView(imgAlignRight, R.color.white);
                break;
            case Gravity.END:
                setColorForImageView(imgAlignLeft, R.color.white);
                setColorForImageView(imgAlignCenter, R.color.white);
                setColorForImageView(imgAlignRight, R.color.color_3cc2f5_legend);
                break;
        }
    }

    @Override
    public void onClipArtTouched(ClipArt currentView) {
        previousViewClipArt = currentClipArt;
        currentClipArt = currentView;
        showTextMode(true);

        updateCurrentStatusForTool();
    }


    //update current status of tool
    private void updateCurrentStatusForTool() {
        switch (mToolTextAdapter.getCurrentNumberTool()) {
            case 1:
                sbOpacity.setProgress(currentClipArt.getOpacityText());
                break;
            case 2:
                sbOpacity.setProgress(currentClipArt.getOpacityBackground());
                break;
            case 3:
                sbSaturationShadow.setProgress(currentClipArt.getSaturationShadow());
                sbOpacityShadow.setProgress(currentClipArt.getOpacityShadow());
                break;
            case 4:
                rvFont.smoothScrollToPosition(currentClipArt.getFont());
                mFontAdapter.setNumberFont(currentClipArt.getFont());
                mFontAdapter.notifyDataSetChanged();
//                Timber.e(currentClipArt.getFont() + "");
                break;
            case 5:
                setIconGravity(currentClipArt.getGravity());
                break;
        }
    }

    @Override
    public void onClipArtDoubleTapped(ClipArt clipArt) {
        Dialog addTextDialog = new DialogAddTextBuilder(this, this, clipArt.getText(), Utility.getBitmapFromView(relativeBackground)).build();
        addTextDialog.show();
    }
}