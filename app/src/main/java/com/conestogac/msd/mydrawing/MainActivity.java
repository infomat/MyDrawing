package com.conestogac.msd.mydrawing;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
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
import java.util.List;

public class MainActivity extends Activity implements OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_EXTERNAL_STORAGE =1;
    private static String[] PERMISSIONS_EXT_STORAGE = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private DrawingView drawView;
    private float smallBrush, mediumBrush, largeBrush;
    private ImageButton currPaint, cameraBtn, drawBtn, textBtn, eraseBtn, newBtn, saveBtn;
    //final EditText et = (EditText) findViewById(R.id.ed_text);
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int ACTION_TAKE_PHOTO_B = 1;
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

    @Override
    public void onClick(View view){
//respond to clicks
        switch (view.getId())
        {
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
            case R.id.new_btn:
                Log.d(TAG, "newbtn");
                ProcessNew();
                break;
            case R.id.camera_btn:
                Log.d(TAG, "camerabtn");
                takePhoto(view);
                break;
            case R.id.save_btn:
                Log.d(TAG, "savebtn");
                ProcessSave();
                break;
            default:
                break;
        }
    }

    public void takePhoto(View view)
    {
        isStoragePermissionGranted();
    }

    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
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
        Log.d(TAG, "createImageFile: "+imageFileName);

		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, getAlbumDir());
        mCurrentPhotoPath = imageF.getAbsolutePath();
		return imageF;
	}

   private boolean setPic() {
		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size``` of the ImageView */
		int targetW = drawView.getWidth();
		int targetH = drawView.getHeight();

		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

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
		Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
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
            File f;
            try {
                    f = createImageFile();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                    Log.d(TAG, "dispatchTakePictureIntent");
            } catch (IOException e) {
                e.printStackTrace();
                f = null;
                mCurrentPhotoPath = null;
            }
			break;

            default:
                break;
		} // switch

		startActivityForResult(takePictureIntent, actionCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d(TAG, "onActivityResult");
        Log.d(TAG, "requestCode: " + requestCode + "resultCode: " + resultCode);
        if(requestCode == ACTION_TAKE_PHOTO_B) {
            handleBigCameraPhoto();
        }
    }
    
    private void handleSmallCameraPhoto(Intent intent) {
		Bundle extras = intent.getExtras();
		mImageBitmap = (Bitmap) extras.get("data");
		mImageView.setImageBitmap(mImageBitmap);
		mImageView.setVisibility(View.VISIBLE);
	}

	private void handleBigCameraPhoto() {
		if (mCurrentPhotoPath != null) {
            Log.d(TAG, "handleBigCameraPhoto");
            if (setPic() == true) {
                galleryAddPic();
            }
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
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG,"Now, REQUEST_EXTERNAL_STORAGE is granted ");
                dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
            } else {
                Snackbar.make(curLayout, R.string.permission_extsto_nok,
                        Snackbar.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
/**
	 * Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
	 *
	 * @param context The application's environment.
	 * @param action The Intent action to check for availability.
	 *
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
			packageManager.queryIntentActivities(intent,
					PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
    private void ProcessSave() {
        //save drawing
        AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
        saveDialog.setTitle("Save drawing");
        saveDialog.setMessage("Save drawing to device Gallery?");
        saveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //save drawing
                FileOutputStream fos=null;
                try {
                    drawView.setDrawingCacheEnabled(true);
                    fos = new FileOutputStream(new File(mCurrentPhotoPath));
                    Bitmap bitmap = drawView.getDrawingCache();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.flush();
                    fos.close();
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }


                if (fos != null) {
                    Toast savedToast = Toast.makeText(getApplicationContext(),
                            "Drawing saved to Gallery!", Toast.LENGTH_SHORT);
                    savedToast.show();
                } else {
                    Toast unsavedToast = Toast.makeText(getApplicationContext(),
                            "Oops! Image could not be saved.", Toast.LENGTH_SHORT);
                    unsavedToast.show();
                }
                drawView.destroyDrawingCache();
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        saveDialog.show();
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
            public void onClick(DialogInterface dialog, int which){
                dialog.cancel();
            }
        });
        newDialog.show();
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

    private void ProcessText() {
        //Show Dialog to get size of text
        // drawView.setTextMode();
        // et.setVisibility(View.INVISIBLE);
        // et.setFocusable(true);
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
        largeBtn.setOnClickListener(new OnClickListener(){
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

    public  void isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is already granted");
                dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
            } else {
                Log.v(TAG, "Show Rationale");
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.i(TAG, "Displaying external storage permission rationale to provide additional context.");
                    ActivityCompat.requestPermissions(this,PERMISSIONS_EXT_STORAGE , REQUEST_EXTERNAL_STORAGE);
                } else {
                    Log.v(TAG, "Permission is revoked");
                    ActivityCompat.requestPermissions(this,PERMISSIONS_EXT_STORAGE , REQUEST_EXTERNAL_STORAGE);
                }
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted due to SDK");
        }
    }
}
