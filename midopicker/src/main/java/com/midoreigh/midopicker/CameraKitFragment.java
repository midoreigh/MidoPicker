package com.midoreigh.midopicker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageButton;

import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;
import com.midoreigh.midopicker.util.BitmapUtil;
import com.midoreigh.midopicker.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by midoreigh on 28/4/17.
 */

public class CameraKitFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = CameraKitFragment.class.getSimpleName();

    private static final Interpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final Interpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();

    private static Config mConfig;
    private CameraView cameraView;
    private ImageButton btn_take_picture;
    private View vShutter;

    /**
     * @param config
     */
    public static void setConfig(@Nullable Config config) {
        mConfig = config;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.picker_fragment_camerakit, null, false);

        cameraView = (CameraView) view.findViewById(R.id.camera_kit_view);
        btn_take_picture = (ImageButton) view.findViewById(R.id.btn_take_picture);
        btn_take_picture.setOnClickListener(this);
        btn_take_picture.setImageResource(mConfig.getCameraBtnImage());
        btn_take_picture.setBackgroundResource(mConfig.getCameraBtnBackground());
        vShutter = view.findViewById(R.id.vShutter);

        ViewGroup.LayoutParams cameraViewLayoutParams = cameraView.getLayoutParams();
        cameraViewLayoutParams.height = (int) getResources().getDimension(mConfig.getCameraHeight());
        cameraViewLayoutParams.width = (int) getResources().getDimension(mConfig.getCameraWidth());

        cameraView.setLayoutParams(cameraViewLayoutParams);

        cameraView.setCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] picture) {
                super.onPictureTaken(picture);

                // Create a bitmap
                Bitmap resultImage = BitmapFactory.decodeByteArray(picture, 0, picture.length);
                saveImage(resultImage);
            }
        });

        return view;
    }
    private File getPhotoDirectory() {
        return new File(Environment.getExternalStorageDirectory() + "/"+getResources().getString(mConfig.getSavedDirectoryName())+"/");
    }

    private String getPhotoFilename() {
        String ts = (new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)).format(new Date());
        return "Photo_" + ts + ".jpg";
    }

    protected File getPhotoPath() {
        File dir = this.getPhotoDirectory();
        dir.mkdirs();
        return new File(dir, this.getPhotoFilename());
    }

    private void saveImage(Bitmap image) {

        try {

            final File photo = getPhotoPath();

            if (photo.exists()) {
                photo.delete();
            }

            FileOutputStream fos;

            fos = new FileOutputStream(photo);

            image.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            fos.flush();
            fos.close();

            MediaScannerConnection.scanFile(getActivity(), new String[]{photo.getPath()}, new String[]{"image/jpeg"}, new MediaScannerConnection.MediaScannerConnectionClient() {
                @Override
                public void onMediaScannerConnected() {

                }

                @Override
                public void onScanCompleted(String path, Uri uri) {

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showTakenPicture(Uri.parse(photo.toString()));
                        }
                    });

                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    public void onPause() {
        cameraView.stop();
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        // if activity is closed suddenly,
        // dismiss the progress dialog.
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }



    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setMessage(getString(R.string.progress_title));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
    }

    @Override
    public void onClick(View v) {
        if (v == btn_take_picture) {
            onTakePicture(v);
        }
    }

    private void onTakePicture(View v) {
        cameraView.captureImage();
        btn_take_picture.setEnabled(false);
        animateShutter();
    }

    private void animateShutter() {
        vShutter.setVisibility(View.VISIBLE);
        vShutter.setAlpha(0.f);

        ObjectAnimator alphaInAnim = ObjectAnimator.ofFloat(vShutter, "alpha", 0f, 0.8f);
        alphaInAnim.setDuration(100);
        alphaInAnim.setStartDelay(100);
        alphaInAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

        ObjectAnimator alphaOutAnim = ObjectAnimator.ofFloat(vShutter, "alpha", 0.8f, 0f);
        alphaOutAnim.setDuration(200);
        alphaOutAnim.setInterpolator(DECELERATE_INTERPOLATOR);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(alphaInAnim, alphaOutAnim);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                vShutter.setVisibility(View.GONE);
                mProgressDialog.show();
            }
        });
        animatorSet.start();
    }

    public void showTakenPicture(Uri uri) {

        ImagePickerActivity mImagePickerActivity = ((ImagePickerActivity) getActivity());

        mImagePickerActivity.addImage(uri);

        GalleryFragment mGalleryFragment = mImagePickerActivity.getGalleryFragment();

        if (mGalleryFragment != null) {
            mGalleryFragment.refreshGallery(mImagePickerActivity);
        }

        btn_take_picture.setEnabled(true);

        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
    }
}
