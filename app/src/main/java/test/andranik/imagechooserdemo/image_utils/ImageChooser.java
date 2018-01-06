package test.andranik.imagechooserdemo.image_utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import test.andranik.imagechooserdemo.R;

import static android.os.Build.VERSION_CODES.N;

/**
 * Created by andranik on 8/10/16.
 */
public class ImageChooser {

    public interface AfterImageLoadedCallback {
        void onImageLoaded(Bitmap loadedBitmap);
    }

    private final static int GET_IMAGE_REQUEST = 1;
    private final static int PERMISSIONS_REQUEST = 2;
    private final static int CROP_REQUEST = 3;

    private static final String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
    private static final String CAMERA = "android.permission.CAMERA";

    private boolean resize = false;
    private boolean fromCamera;

    private int resizeW = 300; // default size
    private int resizeH = 300; // default size

    private int aspectX = 1; // default aspect ratio
    private int aspectY = 1; // default aspect ratio

    private Activity activity;
    private Fragment fragment;

    private ImageView imageView;
    private String capturedImagePath;
    private String tempFile;
    private AfterImageLoadedCallback afterLoadedCallback;
    private Intent intentData;

    private ImageChooser(Fragment fragment, Activity activity, ImageView imageView,
                         boolean resize, int resizeH, int resizeW, int aspectX, int aspectY,
                         AfterImageLoadedCallback afterLoadedCallback) {

        this.activity = activity;
        this.fragment = fragment;
        this.imageView = imageView;
        this.resize = resize;
        this.resizeH = resizeH;
        this.resizeW = resizeW;
        this.aspectX = aspectX;
        this.aspectY = aspectY;
        this.afterLoadedCallback = afterLoadedCallback;
    }

    public void choose() {
        if (isPermissionsGranted()) {
            chooseImage();
        }
    }

    public void choose(ImageView imageView) {
        this.imageView = imageView;
        choose();
    }

    private Context getContext(){
        return activity == null ? fragment.getContext().getApplicationContext() : activity.getApplicationContext();
    }

    private void chooseImage() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setType("image/*");

        String filename = Math.random() + ".png";

        boolean cacheDirAvailable = getContext().getExternalCacheDir() != null;
        String dir = cacheDirAvailable ? getContext().getExternalCacheDir().getAbsolutePath() : null;

        capturedImagePath = dir + "/" + filename;
        File file = new File(capturedImagePath);

        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        Uri outPutUri = FileProvider.getUriForFile(getContext(),
                getContext().getApplicationContext().getPackageName() + ".provider", file);

        UriPermission.grantUriPermission(pickIntent, getContext(), outPutUri);

        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);

        UriPermission.grantUriPermission(pickIntent, getContext(), outPutUri);

        tempFile = generateTempFile("png").getAbsolutePath();

        Uri tempFileUri = FileProvider.getUriForFile(getContext(),
                getContext().getPackageName() + ".provider", new File(tempFile));

        UriPermission.grantUriPermission(pickIntent,  getContext(), tempFileUri);


        UriPermission.grantUriPermission(captureIntent, getContext(), outPutUri);

        Intent chooserIntent = Intent.createChooser(pickIntent,
                getContext().getString(R.string.take_choose_photo));

        if (cacheDirAvailable) {
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{captureIntent});
        }

        if (activity != null) {
            activity.startActivityForResult(chooserIntent, GET_IMAGE_REQUEST);
        } else {
            fragment.startActivityForResult(chooserIntent, GET_IMAGE_REQUEST);
        }
    }

    public boolean handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CROP_REQUEST && resultCode == Activity.RESULT_OK) {
            File tempFileFile = new File(tempFile);

            //handle image loading accordingly
            if (fromCamera) {
                File file = new File(capturedImagePath);
                boolean renamed = tempFileFile.renameTo(file);
                tempFileFile.delete();
                setImageToView(file.getAbsolutePath());
            } else {
                tempFileFile.delete();
                setImageToView(capturedImagePath);
            }
            return true;
        }

        intentData = data;

        if (requestCode == GET_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data == null || data.getData() == null) { // From Camera.
                fromCamera = true;
                startCrop(capturedImagePath, tempFile);
            } else { // From Gallery.
                fromCamera = false;
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContext().getContentResolver().query(selectedImage, filePathColumn,
                        null, null, null);

                if (cursor != null) {
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String imagePath = cursor.getString(columnIndex);

                    cursor.close();

                    startCrop(imagePath, capturedImagePath);
                } else {
                    startCrop(selectedImage.getPath(), capturedImagePath);
                }
            }
            return true;
        }

        return false;
    }

    public void handleOnRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != PERMISSIONS_REQUEST) {
            return;
        }

        for (int i : grantResults) {
            if (i != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        chooseImage();
    }

    private void setImageToView(String imagePath) {
        imageView.setVisibility(View.VISIBLE);

        BitmapRequestBuilder builder = Glide.with(getContext())
                .load("file://" + imagePath)
                .asBitmap()
                .listener(new RequestListener<String, Bitmap>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
                        e.printStackTrace();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        if (afterLoadedCallback != null) {
                            afterLoadedCallback.onImageLoaded(resource);
                        }
                        return false;
                    }
                });

        if (resize) {
            builder.override(resizeW, resizeH);
        }

        builder.into(imageView);
    }

    private boolean isPermissionsGranted() {
        return checkPermission();
    }

    private boolean checkPermission() {
        boolean cameraGranted = ContextCompat
                .checkSelfPermission(getContext(), CAMERA) == PackageManager.PERMISSION_GRANTED;

        boolean readStorageGranted = ContextCompat
                .checkSelfPermission(getContext(), READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        if (readStorageGranted && cameraGranted) {
            return true;
        } else {
            List<String> perms = new ArrayList<>();

            if (!cameraGranted) {
                perms.add(CAMERA);
            }
            if (!readStorageGranted) {
                perms.add(READ_EXTERNAL_STORAGE);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (activity != null) {
                    activity.requestPermissions(perms.toArray(new String[perms.size()]), PERMISSIONS_REQUEST);
                } else {
                    fragment.requestPermissions(perms.toArray(new String[perms.size()]), PERMISSIONS_REQUEST);
                }
            }
        }
        return false;
    }

    private void startCrop(String path, String output) {
        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        cropIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Uri outPutUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", new File(output));

        Uri photoURI;

        if (Build.VERSION.SDK_INT >= N) {
            if (path == null) {
                downloadBitmapFromRemote();
                photoURI = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", new File(capturedImagePath));
            } else {
                photoURI = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", new File(path));
            }
        } else {
            if (path == null) {
                downloadBitmapFromRemote();
                photoURI = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", new File(capturedImagePath));
            } else {
                photoURI = Uri.fromFile(new File(path));
            }
        }


        UriPermission.grantUriPermission(cropIntent,
                activity, outPutUri, photoURI);

        cropIntent.setDataAndType(photoURI, "image/*");

        cropIntent.putExtra("crop", "true");

        cropIntent.putExtra("aspectX", aspectX);
        cropIntent.putExtra("aspectY", aspectY);

        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);

        if (fragment == null) {
            activity.startActivityForResult(cropIntent, CROP_REQUEST);
        } else {
            fragment.startActivityForResult(cropIntent, CROP_REQUEST);
        }
    }


    public static class Builder {

        private Activity activity;
        private Fragment fragment;

        private boolean resize = false;

        private int resizeW = 300; // default size
        private int resizeH = 300; // default size

        private int aspectX = 1; // default aspect ratio
        private int aspectY = 1; // default aspect ratio

        private ImageView imageView;

        private AfterImageLoadedCallback afterLoadedCallback;

        public static Builder with(Activity activity) {
            return new Builder(activity, null);
        }

        public static Builder with(Fragment fragment) {
            return new Builder(null, fragment);
        }

        private Builder(Activity activity, Fragment fragment) {
            this.activity = activity;
            this.fragment = fragment;
        }

        public Builder setResize(boolean resize) {
            this.resize = resize;
            return this;
        }

        public Builder setResizeW(int resizeW) {
            this.resizeW = resizeW;
            return this;
        }

        public Builder setResizeH(int resizeH) {
            this.resizeH = resizeH;
            return this;
        }

        public Builder setAspectX(int aspectX) {
            this.aspectX = aspectX;
            return this;
        }

        public Builder setAspectY(int aspectY) {
            this.aspectY = aspectY;
            return this;
        }

        public Builder setImageView(ImageView imageView) {
            this.imageView = imageView;
            return this;
        }

        public Builder setAfterLoadedCallback(AfterImageLoadedCallback afterLoadedCallback) {
            this.afterLoadedCallback = afterLoadedCallback;
            return this;
        }

        public ImageChooser build() {
            return new ImageChooser(fragment, activity, imageView, resize, resizeH, resizeW, aspectX, aspectY,
                    afterLoadedCallback);
        }
    }

    private boolean downloadBitmapFromRemote() {
        File cacheDirPath = new File(capturedImagePath);
        try {
            InputStream is = activity.getContentResolver().openInputStream(intentData.getData());
            if (is != null) {
                Bitmap pictureBitmap = BitmapFactory.decodeStream(is);

                try {
                    FileOutputStream out = new FileOutputStream(
                            cacheDirPath);
                    pictureBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

                    out.flush();
                    out.close();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return false;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private File generateTempFile(String type) {
        String filename = "TempFile." + type;

        boolean cacheDirAvailable = getContext().getExternalCacheDir() != null;
        String dir = cacheDirAvailable ? getContext().getExternalCacheDir().getAbsolutePath() : null;

        String filePath = dir + "/" + filename;
        File file = new File(filePath);
        return file;
    }
}