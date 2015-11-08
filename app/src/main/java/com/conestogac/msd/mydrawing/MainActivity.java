package com.conestogac.msd.mydrawing;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity implements OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_EXTERNAL_STORAGE_CAM = 0;
    private static final int REQUEST_EXTERNAL_STORAGE_IMG = 1;
    private static final int REQUEST_EXTERNAL_STORAGE_GAL = 2;
    private static String[] PERMISSIONS_EXT_STORAGE = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private DrawingView drawView;
    private float smallBrush, mediumBrush, largeBrush;
    private ImageButton currPaint, cameraBtn, drawBtn, textBtn, eraseBtn, newBtn, saveBtn;
    //final EditText et = (EditText) findViewById(R.id.ed_text);
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    private static final int ACTION_TAKE_PHOTO_B = 1;
    private static final int ACTION_SELECT_IMAGE = 2;

    private static final String BITMAP_STORAGE_KEY = "viewbitmap";
    private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
    private ImageView mImageView;
    private Bitmap mImageBitmap;
    private String mCurrentPhotoPath;
    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
    private String getAlbumName() {
        return getString(R.string.album_name);
    }
    private View curLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        curLayout = findViewById(R.id.main_layout);
        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);
        drawView = (DrawingView)findViewById(R.id.drawing);
        drawView.setBrushSize(mediumBrush);
        mImageBitmap = null;
        mImageView = (ImageView) findViewById(R.id.imageView);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}
//        et.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
//                drawView.writeText(et.getText().toString());
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//            }
//        });

        LinearLayout paintLayout = (LinearLayout)findViewById(R.id.paint_colors);
        currPaint = (ImageButton)paintLayout.getChildAt(0);
        currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
        cameraBtn = (ImageButton)findViewById(R.id.camera_btn);
        cameraBtn.setOnClickListener(this);
        drawBtn = (ImageButton)findViewById(R.id.draw_btn);
        drawBtn.setOnClickListener(this);
        textBtn = (ImageButton)findViewById(R.id.text_btn);
        textBtn.setOnClickListener(this);
        eraseBtn = (ImageButton)findViewById(R.id.erase_btn);
        eraseBtn.setOnClickListener(this);
        newBtn = (ImageButton)findViewById(R.id.new_btn);
        newBtn.setOnClickListener(this);
        saveBtn = (ImageButton)findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(this);
     }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d(TAG, "onActivityResult");
        Log.d(TAG, "requestCode: " + requestCode + "resultCode: " + resultCode);
        if(requestCode == ACTION_TAKE_PHOTO_B) {
            handleBigCameraPhoto();
        } else if (requestCode == ACTION_SELECT_IMAGE) {
            Log.d(TAG, "From Gallery");
            handleSelectedImage(data);
        }
    }


    // Some lifecycle callbacks so that the image can survive orientation change
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
        outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
        mImageView.setImageBitmap(mImageBitmap);
        mImageView.setVisibility(
                savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ?
                        ImageView.VISIBLE : ImageView.INVISIBLE
        );
    }
    //Form Marshmallow, External Storage Permission si not possible to use at manifest file
    //So, runtime requesting user grant for permission will be done, and then this callback will be called
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_EXTERNAL_STORAGE_CAM) {
                dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
            } else if (requestCode == REQUEST_EXTERNAL_STORAGE_IMG){
                SaveImage();
            } else if (requestCode == REQUEST_EXTERNAL_STORAGE_GAL) {
                setIntentGallery();
            }
        } else {
            Snackbar.make(curLayout, R.string.permission_extsto_nok,
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view){
//respond to clicks
        switch (view.getId())
        {
            case R.id.new_btn:
                Log.d(TAG, "newbtn");
                ProcessNew();
                break;
            case R.id.camera_btn:
                Log.d(TAG, "camerabtn");
                takePhoto(view);
                break;
            case R.id.gallery_btn:
                Log.d(TAG, "gallerybtn");
                gotoGallery(view);
                break;
            case R.id.draw_btn:
                Log.d(TAG, "drawbtn");
                ProcessDraw();
                break;
            case R.id.text_btn:
                Log.d(TAG, "textbtn");
                ProcessText();
                break;
            case R.id.erase_btn:
                Log.d(TAG, "erasebtn");
                ProcessErase();
                break;
            case R.id.save_btn:
                Log.d(TAG, "savebtn");
                ProcessSave();
                break;
            default:
                break;
        }
    }

    private void ProcessNew() {
        //new button
        AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
        newDialog.setTitle("New drawing");
        newDialog.setMessage("Start new drawing (you will lose the current drawing)?");
        newDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
                drawView.startNew();
                dialog.dismiss();
            }
        });
        newDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        newDialog.show();
    }

    public void takePhoto(View view)
    {
        isStoragePermissionGranted(REQUEST_EXTERNAL_STORAGE_CAM);
    }

    public void gotoGallery(View v) {
        isStoragePermissionGranted(REQUEST_EXTERNAL_STORAGE_GAL);
    }
    private void setIntentGallery(){
        Intent goGalleryIntent = new Intent(Intent.ACTION_PICK);
        goGalleryIntent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        goGalleryIntent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(goGalleryIntent, "Select File"), ACTION_SELECT_IMAGE);
    }
    private void ProcessDraw() {
        //draw button clicked
        final Dialog brushDialog = new Dialog(this);
        brushDialog.setTitle("Brush size:");
        brushDialog.setContentView(R.layout.brush_chooser);
        ImageButton smallBtn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
        smallBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.setBrushSize(smallBrush);
                drawView.setLastBrushSize(smallBrush);
                drawView.setErase(false);
                brushDialog.dismiss();
            }
        });

        ImageButton mediumBtn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
        mediumBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.setBrushSize(mediumBrush);
                drawView.setLastBrushSize(mediumBrush);
                drawView.setErase(false);
                brushDialog.dismiss();
            }
        });

        ImageButton largeBtn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
        largeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.setBrushSize(largeBrush);
                drawView.setLastBrushSize(largeBrush);
                drawView.setErase(false);
                brushDialog.dismiss();
            }
        });
        brushDialog.show();
    }

    private void ProcessText() {
        //Show Dialog to get size of text
        // drawView.setTextMode();
        // et.setVisibility(View.INVISIBLE);
        // et.setFocusable(true);
    }

    private void ProcessErase() {
        //switch to erase - choose size
        final Dialog brushDialog = new Dialog(this);
        brushDialog.setTitle("Eraser size:");
        brushDialog.setContentView(R.layout.brush_chooser);
        ImageButton smallBtn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
        smallBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.setErase(true);
                drawView.setBrushSize(smallBrush);
                brushDialog.dismiss();
            }
        });
        ImageButton mediumBtn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
        mediumBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.setErase(true);
                drawView.setBrushSize(mediumBrush);
                brushDialog.dismiss();
            }
        });
        ImageButton largeBtn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
        largeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.setErase(true);
                drawView.setBrushSize(largeBrush);
                brushDialog.dismiss();
            }
        });
        brushDialog.show();
    }

    private void ProcessSave() {
        //save drawing
        AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
        saveDialog.setTitle("Save drawing");
        saveDialog.setMessage("Save drawing to device Gallery?");
        saveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //In case of no camera image exists -> obtain permission & make new file
                if (mCurrentPhotoPath == null) {
                    isStoragePermissionGranted(REQUEST_EXTERNAL_STORAGE_IMG);
                } else {
                    SaveImage();
                }
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        saveDialog.show();
    }

    private void handleSelectedImage(Intent data){
        Uri selectedImageUri = data.getData();
        String[] projection = { MediaStore.MediaColumns.DATA };
        Cursor cursor = managedQuery(selectedImageUri, projection, null, null,
                null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        cursor.moveToFirst();

        String selectedImagePath = cursor.getString(column_index);
        setPic(selectedImagePath);
    }

    private void handleBigCameraPhoto() {
        if (mCurrentPhotoPath != null) {
            Log.d(TAG, "handleBigCameraPhoto");
            if (setPic(mCurrentPhotoPath) == true) {
                galleryAddPic();
            }
        }
    }

    private void SaveImage() {
        FileOutputStream fos=null;
        Log.d(TAG, "SaveImage()- mCurrentPhotoPath: " + mCurrentPhotoPath);
        //in case of with out camera taken, no file is created
        if (mCurrentPhotoPath == null) {
            File f;
            try {
                f = createImageFile();
                Log.d(TAG, "SaveImage() new file is created"+mCurrentPhotoPath);
            } catch (IOException e) {
                e.printStackTrace();
                f = null;
                mCurrentPhotoPath = null;
                Log.e(TAG, "SaveImage() something wrong");
            }
        }

        try {
            drawView.setDrawingCacheEnabled(true);
            fos = new FileOutputStream(new File(mCurrentPhotoPath));
            Bitmap bitmap = drawView.getDrawingCache();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            Toast savedToast = Toast.makeText(getApplicationContext(),
                    "Drawing saved to Gallery!", Toast.LENGTH_SHORT);
            savedToast.show();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            Toast unsavedToast = Toast.makeText(getApplicationContext(),
                    "Oops! Image could not be saved.", Toast.LENGTH_SHORT);
            unsavedToast.show();
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Toast unsavedToast = Toast.makeText(getApplicationContext(),
                    "Oops! Image could not be saved.", Toast.LENGTH_SHORT);
            unsavedToast.show();
            e.printStackTrace();
        }
        drawView.destroyDrawingCache();
    }

    public  void isStoragePermissionGranted(int REQ_CODE) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is already granted");
                if (REQ_CODE == REQUEST_EXTERNAL_STORAGE_CAM) {
                    dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
                } else if (REQ_CODE == REQUEST_EXTERNAL_STORAGE_IMG){
                    SaveImage();
                } else if (REQ_CODE == REQUEST_EXTERNAL_STORAGE_GAL){
                    setIntentGallery();
                }
            } else {
                Log.v(TAG, "Show Rationale");
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.i(TAG, "Displaying external storage permission rationale to provide additional context.");
                    ActivityCompat.requestPermissions(this,PERMISSIONS_EXT_STORAGE , REQ_CODE);
                } else {
                    Log.v(TAG, "Permission is revoked");
                    ActivityCompat.requestPermissions(this,PERMISSIONS_EXT_STORAGE , REQ_CODE);
                }
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted due to SDK");
        }
    }
    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());
            Log.d(TAG, "getAlbumDir: "+storageDir);
            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        Log.d(TAG, "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.d(TAG, "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        Log.d(TAG, "createImageFile: " + imageFileName);

        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, getAlbumDir());
        mCurrentPhotoPath = imageF.getAbsolutePath();
        return imageF;
    }

    //get path of image and set image at drawview as bitmap
    private boolean setPic(String imgPath) {
		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */
		/* Get the size``` of the ImageView */
        if (imgPath == null) return false;

        int targetW = drawView.getWidth();
        int targetH = drawView.getHeight();

		/* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath, bmOptions);

        //in case of clear without taking photo
        if (bmOptions.outMimeType == null) return false;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        }
        Log.d(TAG, "Resizing targetW: "+targetW+ "photoW: "+photoW+"targetH"+targetH+ "photoH: "+photoH  );
		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
        Bitmap bitmap = BitmapFactory.decodeFile(imgPath, bmOptions);
        drawView.setCanvasBitmap(bitmap);
        return true;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        Log.d(TAG, "galleryAddPic: " + mCurrentPhotoPath);
    }

    private void dispatchTakePictureIntent(int actionCode) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        switch(actionCode) {
            case ACTION_TAKE_PHOTO_B:

                Log.i(TAG, "Now, REQUEST_EXTERNAL_STORAGE is granted ");
                if (mCurrentPhotoPath == null) {
                    File f;
                    try {
                        f = createImageFile();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                        Log.d(TAG, "dispatchTakePictureIntent");
                    } catch (IOException e) {
                        e.printStackTrace();
                        f = null;
                        mCurrentPhotoPath = null;
                        Log.e(TAG, "dispatchTakePictureIntent something wrong!!");
                    }
                }

                break;

            default:
                break;
        } // switch

        startActivityForResult(takePictureIntent, actionCode);
    }

    public void paintClicked(View view){
        //use chosen color
        if(view!=currPaint){
            //update color
            ImageButton imgView = (ImageButton)view;
            String color = view.getTag().toString();
            drawView.setColor(color);
            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            currPaint=(ImageButton)view;
            drawView.setErase(false);
            drawView.setBrushSize(drawView.getLastBrushSize());
        }
    }
}
