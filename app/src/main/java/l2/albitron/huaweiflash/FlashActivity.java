package l2.albitron.huaweiflash;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FlashActivity extends ActionBarActivity  {

    protected static final int TARGET_WIDTH = 720;
    protected static final int TARGET_HEIGHT = 1280;

    protected static final int REQ_CODE_PICK_IMAGE = 1;
    protected static final int REQ_CODE_DOWNSAMPLING = 2;

    public static final String TAG_DOWNSAMPLING = "downsampling";
    public static final int DOWNSAMPLE_444 = 444;
    public static final int DOWNSAMPLE_454 = 454;
    public static final int DOWNSAMPLE_555 = 555;
    public static final int DOWNSAMPLE_565 = 565;
    public static final int NO_DOWNSAMPLING = 0;

    public static final float FACTOR_4 = 15.0f;
    public static final float FACTOR_5 = 31.0f;
    public static final float FACTOR_6 = 63.0f;

    // RGB 565 -> 888 conversion tables
//    private static short [] table5 = new short []
//            {0, 8, 16, 25, 33, 41, 49, 58, 66, 74, 82, 90, 99, 107, 115, 123, 132,
//            140, 148, 156, 165, 173, 181, 189, 197, 206, 214, 222, 230, 239, 247, 255};
//
//    private static short [] table6 = new short []
//            {0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 45, 49, 53, 57, 61, 65, 69,
//            73, 77, 81, 85, 89, 93, 97, 101, 105, 109, 113, 117, 121, 125, 130, 134, 138,
//            142, 146, 150, 154, 158, 162, 166, 170, 174, 178, 182, 186, 190, 194, 198,
//            202, 206, 210, 215, 219, 223, 227, 231, 235, 239, 243, 247, 251, 255};

    public static final String OEMLOGO_PATH = "/cust/media/oemlogo.mbn";

    private Bitmap mBitmap = null;
    private File mBitmapFile = null;
    private int mDownsampling = DOWNSAMPLE_565;

        View.OnClickListener onFlashClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mBitmap == null)
                return;


            final Bitmap scaled =
                    (mBitmap.getWidth() != TARGET_WIDTH || mBitmap.getHeight() != TARGET_HEIGHT)
                            ? mBitmap.createScaledBitmap(mBitmap, TARGET_WIDTH, TARGET_HEIGHT, true)
                            : mBitmap;

            final Bitmap converted = scaled; //scaled.copy(Bitmap.Config.RGB_565, false);

            try {
                final File pixels =
                        File.createTempFile(String.valueOf(System.currentTimeMillis()),
                            "", getFilesDir());

                Button buttonFlash = (Button) findViewById(R.id.buttonFlash);
                buttonFlash.setText(getResources().getString(R.string.textPleaseWait));
                buttonFlash.setEnabled(false);

                Button buttonColorSettings = (Button)findViewById(R.id.buttonColorSettings);
                buttonColorSettings.setEnabled(false);

                ImageView imageToFlash = (ImageView) findViewById(R.id.imageView);
                imageToFlash.setEnabled(false);


                new AsyncTask<Void, Void, Void>() {
                    protected Void doInBackground(Void... params) {
                        writePixelsToFile(pixels, converted);
                        flashPixels(pixels);
                        pixels.delete();

                        return null;
                    }

                    protected void onProgressUpdate(Void... progress) {
                    }

                    protected void onPostExecute(Void param) {
                        onFlashComplete();
                    }
                }.execute();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    };

    public float getGFactor(int depth)
    {
        switch (depth)
        {
            case DOWNSAMPLE_444:
                return FACTOR_4;
            case DOWNSAMPLE_454:
                return FACTOR_5;
            case DOWNSAMPLE_555:
                return FACTOR_5;
            case DOWNSAMPLE_565:
                return FACTOR_6;
        }

        return 1f;
    }

    public float getRBFactor(int depth)
    {
        switch (depth)
        {
            case DOWNSAMPLE_444:
                return FACTOR_4;
            case DOWNSAMPLE_454:
                return FACTOR_4;
            case DOWNSAMPLE_555:
                return FACTOR_5;
            case DOWNSAMPLE_565:
                return FACTOR_5;
        }

        return 1f;
    }

    private void writePixelsToFile(File file, Bitmap image)
    {
        try {
            FileOutputStream outputStream = new FileOutputStream(file);

            byte [] output = new byte [image.getWidth() * image.getHeight() * 3];

            try
            {
                int i = 0;
                byte b, g, r;
                float g_factor = getGFactor(mDownsampling);
                float rb_factor = getRBFactor(mDownsampling);

                for (int y = 0; y < image.getHeight(); ++y)
                    for (int x = 0; x < image.getWidth(); ++x)
                    {
                        int px = image.getPixel(x, y);

                        // 888 -> 565
//                        byte b = (byte)Math.round(((float) Color.blue(px) / 255.0f) * 31.0f);
//                        byte g = (byte)Math.round(((float) Color.green(px) / 255.0f) * 63.0f);
//                        byte r = (byte)Math.round(((float) Color.red(px) / 255.0f) * 31.0f);

                        // 565 -> 888
//                        output[i++] = (byte) table5[b];
//                        output[i++] = (byte) table5[g];
//                        output[i++] = (byte) table5[r];

                        if (mDownsampling == NO_DOWNSAMPLING)
                        {
                            b = (byte)Color.blue(px);
                            g = (byte)Color.green(px);
                            r = (byte)Color.red(px);
                        }
                        else
                        {
                            b = (byte)Math.round((Color.blue(px) / 255.0f) * rb_factor);
                            g = (byte)Math.round((Color.green(px) / 255.0f) * g_factor);
                            r = (byte)Math.round((Color.red(px) / 255.0f) * rb_factor);

                            b = (byte)Math.round((b / rb_factor ) * 255.0f);
                            g = (byte)Math.round((g / g_factor) * 255.0f);
                            r = (byte)Math.round((r / rb_factor) * 255.0f);
                        }

                        output[i++] = b;
                        output[i++] = g;
                        output[i++] = r;
                    }

                outputStream.write(output);
            }
            finally {
                outputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void flashPixels(File file)
    {
        try{
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            String libdir = getApplicationInfo().nativeLibraryDir;

            outputStream.writeBytes("rm " + OEMLOGO_PATH + "\n");
            outputStream.flush();

            outputStream.writeBytes("cp -f  " + file.getAbsolutePath() + " " + OEMLOGO_PATH + "\n");
            outputStream.flush();

            outputStream.writeBytes("export LD_LIBRARY_PATH=" + libdir + "\n");
            outputStream.flush();

            // this isn't actually a library, just a renamed binary to trick the IDE
            outputStream.writeBytes(libdir + "/libflash_oemlogo.so\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");

            su.waitFor();
        }catch(IOException e){
            e.printStackTrace();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    private void onFlashComplete()
    {
        Button buttonFlash = (Button) findViewById(R.id.buttonFlash);
        buttonFlash.setText(getResources().getString(R.string.textFlash));
        buttonFlash.setEnabled(false);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ImageView imageToFlash = (ImageView)findViewById(R.id.imageView);

        imageToFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                photoPickerIntent.setPackage("com.android.gallery3d");

                try {
                    mBitmapFile = File.createTempFile(String.valueOf(System.currentTimeMillis()),
                            ".png", getExternalCacheDir());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (mBitmapFile != null) {
                    Uri tempUri = Uri.fromFile(mBitmapFile);
                    photoPickerIntent.putExtra("output", tempUri);
                }

                photoPickerIntent.setType("image/*");
                photoPickerIntent.putExtra("aspectX", 9);
                photoPickerIntent.putExtra("aspectY", 16);
                photoPickerIntent.putExtra("crop", "true");
                photoPickerIntent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
                startActivityForResult(photoPickerIntent, REQ_CODE_PICK_IMAGE);
            }
        });

        Button buttonFlash = (Button)findViewById(R.id.buttonFlash);
        buttonFlash.setOnClickListener(onFlashClicked);

        Button buttonColorSettings = (Button)findViewById(R.id.buttonColorSettings);
        buttonColorSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent colorSettingsIntent =
                        new Intent(FlashActivity.this, ColorSettingsActivity.class);

                colorSettingsIntent.putExtra(TAG_DOWNSAMPLING, mDownsampling);
                startActivityForResult(colorSettingsIntent, REQ_CODE_DOWNSAMPLING);
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {

        super.onActivityResult(requestCode, resultCode, resultIntent);

        switch (requestCode) {
            case REQ_CODE_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    if (resultIntent != null) {
                        try {
                            InputStream inputStream = getContentResolver()
                                    .openInputStream(resultIntent.getData());
                            mBitmap = BitmapFactory.decodeStream(inputStream);

                            ImageView imageToFlash = (ImageView) findViewById(R.id.imageView);
                            imageToFlash.setImageBitmap(mBitmap);

                            Button buttonFlash = (Button) findViewById(R.id.buttonFlash);
                            buttonFlash.setEnabled(true);

                            TextView textSelectImage = (TextView)findViewById(R.id.textSelectImage);
                            textSelectImage.setText("");
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else
                {
                    mBitmapFile.delete();
                    mBitmap = null;
                }
                break;
            case REQ_CODE_DOWNSAMPLING:
                if (resultCode == RESULT_OK) {
                    mDownsampling = resultIntent.getIntExtra(TAG_DOWNSAMPLING, DOWNSAMPLE_565);
                }
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
