package com.scattergram;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    private Button btnSelect;
    private ImageView ivImage;
    private ImageView ivScatteredImage;
    private String userChoosenTask;
    private Button btnScatterImage;
    private ProgressBar progressBar;
    private AsyncTask mMyTask;
    private Button btnShareImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSelect = (Button) findViewById(R.id.btnSelectPhoto);
        btnScatterImage = (Button) findViewById(R.id.btnScatterImage);
        btnShareImage = (Button) findViewById(R.id.btnShareImage);
        ivImage = (ImageView) findViewById(R.id.ivImage);
        ivScatteredImage = (ImageView) findViewById(R.id.ivScatteredImage);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        btnShareImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                shareImage();
            }
        });
        btnScatterImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMyTask = new ScatterImage().execute();
            }
        });
        btnSelect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        btnScatterImage.setEnabled(false);
        progressBar.setVisibility(View.GONE);
        btnShareImage.setEnabled(false);
    }

    //intent to share image with other apps
    private void shareImage() {
        View content = findViewById(R.id.ivScatteredImage);
        Bitmap bitmap = getBitmapFromView(content);
        try {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());

            File file = new File(this.getExternalCacheDir(), "myimage.png");
            FileOutputStream fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            file.setReadable(true, false);
            final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            intent.setType("image/png");
            startActivity(Intent.createChooser(intent, "Share image via"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap getBitmapFromView(View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            bgDrawable.draw(canvas);
        } else {
            canvas.drawColor(Color.WHITE);
        }
        view.draw(canvas);
        return returnedBitmap;
    }

    //Asynctask to scatter the image
    private class ScatterImage extends AsyncTask<String, Integer, Bitmap> {

        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            btnScatterImage.setEnabled(false);
            ivScatteredImage.setVisibility(View.GONE);
            btnShareImage.setEnabled(false);
            btnSelect.setEnabled(false);
        }

        protected Bitmap doInBackground(String... tasks) {

            Bitmap selectedImage = ((BitmapDrawable) ivImage.getDrawable()).getBitmap();
            int w = selectedImage.getWidth();
            int h = selectedImage.getHeight();

            Bitmap scatteredImage = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

            int[][] colors = new int[w][h];

            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int color1 = selectedImage.getPixel(x, y);
                    colors[x][y] = color1;
                }
            }

            shuffle(colors);

            for (int i = colors.length - 1; i > 0; i--) {
                for (int j = colors[i].length - 1; j > 0; j--) {
                    scatteredImage.setPixel(i, j, colors[i][j]);
                }
            }

            return scatteredImage;
        }

        protected void onPostExecute(Bitmap scatteredImage) {
            ivScatteredImage.setImageBitmap(scatteredImage);
            progressBar.setVisibility(View.GONE);
            btnScatterImage.setEnabled(false);
            ivScatteredImage.setVisibility(View.VISIBLE);
            btnShareImage.setEnabled(true);
            btnSelect.setEnabled(true);
        }
    }

    //Fisher–Yates algorithm
    void shuffle(int[][] a) {
        Random random = new Random();

        for (int i = a.length - 1; i > 0; i--) {
            for (int j = a[i].length - 1; j > 0; j--) {
                int m = random.nextInt(i + 1);
                int n = random.nextInt(j + 1);

                int temp = a[i][j];
                a[i][j] = a[m][n];
                a[m][n] = temp;
            }
        }
    }

    private void selectImage() {
        final CharSequence[] items = {"Take Photo", "Choose from Library",
                "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Image");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result = Utility.checkPermission(MainActivity.this);
                if (items[item].equals("Take Photo")) {
                    userChoosenTask = "Take Photo";
                    if (result)
                        cameraIntent();
                } else if (items[item].equals("Choose from Library")) {
                    userChoosenTask = "Choose from Library";
                    if (result)
                        galleryIntent();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void cameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void galleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (userChoosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if (userChoosenTask.equals("Choose from Library"))
                        galleryIntent();
                } else {
                    //code for deny
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data);
        }
    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {
        Bitmap bm = null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(),
                        data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        btnScatterImage.setEnabled(true);
        ivImage.setImageBitmap(bm);
        btnShareImage.setEnabled(false);
        ivScatteredImage.setVisibility(View.GONE);
    }

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File destination = new File(Environment.getExternalStorageDirectory(),
                System.currentTimeMillis() + ".jpg");
        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        btnScatterImage.setEnabled(true);
        ivImage.setImageBitmap(thumbnail);
        btnShareImage.setEnabled(false);
        ivScatteredImage.setVisibility(View.GONE);
    }
}
