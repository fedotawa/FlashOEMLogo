package l2.albitron.huaweiflash;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
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
import java.io.OutputStream;

public class FlashActivity extends ActionBarActivity  {

    protected static final int REQ_CODE_PICK_IMAGE = 1;
    protected static final int REQ_CODE_DOWNSAMPLING = 2;

    public static final String TAG_DOWNSAMPLING = "downsampling";
    public static final int DOWNSAMPLE_FFMPEG = 0x1;
    public static final int DOWNSAMPLE_444 = DOWNSAMPLE_FFMPEG | 0x2;
    public static final int DOWNSAMPLE_454 = 0x4;
    public static final int DOWNSAMPLE_555 = 0x8;
    public static final int DOWNSAMPLE_565 = 0x10;
    public static final int DOWNSAMPLE_565_TABLE = 0x20;
    public static final int DOWNSAMPLE_555_FFMPEG = DOWNSAMPLE_FFMPEG | 0x40;
    public static final int DOWNSAMPLE_565_FFMPEG = DOWNSAMPLE_FFMPEG | 0x80;

    public static final int NO_DOWNSAMPLING = 0;

    public static final float FACTOR_4 = 15.0f;
    public static final float FACTOR_5 = 31.0f;
    public static final float FACTOR_6 = 63.0f;

    // RGB 565 -> 888 conversion tables
    private static short [] table5 = new short []
            {0, 8, 16, 25, 33, 41, 49, 58, 66, 74, 82, 90, 99, 107, 115, 123, 132,
            140, 148, 156, 165, 173, 181, 189, 197, 206, 214, 222, 230, 239, 247, 255, 0};

    private static short [] table6 = new short []
            {0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 45, 49, 53, 57, 61, 65, 69,
            73, 77, 81, 85, 89, 93, 97, 101, 105, 109, 113, 117, 121, 125, 130, 134, 138,
            142, 146, 150, 154, 158, 162, 166, 170, 174, 178, 182, 186, 190, 194, 198,
            202, 206, 210, 215, 219, 223, 227, 231, 235, 239, 243, 247, 251, 255, 0};

    static final String OEMLOGO_PATH = "/cust/media/oemlogo.mbn";
    // these are not actually libraries, just renamed binaries to trick the IDE
    private static final String FLASHER_BINARY = "libflash_oemlogo.so";
    private static final String FFMPEG_BINARY = "libffmpeg.so";

    protected int mTargetWidth = 720;
    protected int mTargetHeight = 1280;

    private Bitmap mBitmap = null;
    private File mBitmapFile = null;
    private int mDownsampling = DOWNSAMPLE_565_FFMPEG;

    View.OnClickListener onFlashClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mBitmap == null)
                return;


            Bitmap scaled =
                    (mBitmap.getWidth() != mTargetWidth || mBitmap.getHeight() != mTargetHeight)
                            ? Bitmap.createScaledBitmap(mBitmap, mTargetWidth, mTargetHeight, true)
                            : mBitmap;

            final Bitmap converted = scaled; //scaled.copy(Bitmap.Config.RGB_565, false);

            try {
                final File pixels =
                        File.createTempFile(String.valueOf(System.currentTimeMillis()),
                                ".png", getFilesDir());

                Button buttonFlash = (Button) findViewById(R.id.buttonFlash);
                buttonFlash.setText(getResources().getString(R.string.textPleaseWait));
                buttonFlash.setEnabled(false);

                Button buttonColorSettings = (Button) findViewById(R.id.buttonColorSettings);
                buttonColorSettings.setEnabled(false);

                ImageView imageToFlash = (ImageView) findViewById(R.id.imageView);
                imageToFlash.setEnabled(false);


                new AsyncTask<Void, Void, Void>() {
                    protected Void doInBackground(Void... params) {
                        try {
                            if ((mDownsampling & DOWNSAMPLE_FFMPEG) != 0)
                                convertPixelsFFMpeg(pixels, converted);
                            else
                                writePixelsToFile(pixels, converted);

                            flashPixels(pixels);
                        }
                        finally {
                            pixels.delete();
                        }

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

    public String getPixelFormat(int depth)
    {
        switch (depth)
        {
            case DOWNSAMPLE_444:
                return "rgb444";
            case DOWNSAMPLE_555_FFMPEG:
                return "rgb555";
            case DOWNSAMPLE_565_FFMPEG:
                return "rgb565";
        }

        return "rgb565";
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

                        if (mDownsampling == NO_DOWNSAMPLING)
                        {
                            b = (byte)Color.blue(px);
                            g = (byte)Color.green(px);
                            r = (byte)Color.red(px);
                        }
                        else if (mDownsampling == DOWNSAMPLE_565_TABLE)
                        {
                            // 888 -> 565
                            b = (byte)Math.round((Color.blue(px) / 255.0f) * 31.0f);
                            g = (byte)Math.round((Color.green(px) / 255.0f) * 63.0f);
                            r = (byte)Math.round((Color.red(px) / 255.0f) * 31.0f);

                            // 565 -> 888
                            b = (byte) table5[b];
                            g = (byte) table6[g];
                            r = (byte) table5[r];
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

    private void convertPixelsFFMpeg(File file, Bitmap image)
    {
        String libdir = getApplicationInfo().nativeLibraryDir;
        File raw = new File(file.getAbsolutePath() + ".raw");

        try {
            OutputStream os = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.close();

            String ffmpeg = libdir + "/" + FFMPEG_BINARY;
            String pixel_format = getPixelFormat(mDownsampling);
            String resolution = mTargetWidth + "x" + mTargetHeight;

            String [] reducedColor = new String []
                    { ffmpeg, "-y", "-vcodec", "png", "-i", file.getAbsolutePath(),
                            "-vcodec", "rawvideo", "-f", "rawvideo",
                            "-pix_fmt", pixel_format, raw.getAbsolutePath()
                    };

            Process p = Runtime.getRuntime().exec(reducedColor);
            p.waitFor();

            String [] fullColor = new String []
                    { ffmpeg, "-y", "-vcodec", "rawvideo",  "-f", "rawvideo", "-pix_fmt",
                            pixel_format, "-s", resolution, "-i", raw.getAbsolutePath(),
                            "-vcodec", "rawvideo", "-f", "rawvideo",
                            "-pix_fmt", "bgr24", file.getAbsolutePath()
                    };

            p = Runtime.getRuntime().exec(fullColor);
            p.waitFor();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally
        {
            raw.delete();
        }

    }

    private void flashPixels(File file)
    {
        File oemlogo = new File(OEMLOGO_PATH);

        try {
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            String libdir = getApplicationInfo().nativeLibraryDir;

            if (!oemlogo.getParentFile().exists()) {
                outputStream.writeBytes("mkdir " + oemlogo.getParentFile() + "\n");
                outputStream.flush();
            }

            if (oemlogo.exists()) {
                outputStream.writeBytes("rm " + OEMLOGO_PATH + "\n");
                outputStream.flush();
            }

            outputStream.writeBytes("cp -f  " + file.getAbsolutePath() + " " + OEMLOGO_PATH + "\n");
            outputStream.flush();

            outputStream.writeBytes("export LD_LIBRARY_PATH=" + libdir + "\n");
            outputStream.flush();

            outputStream.writeBytes(libdir + "/" + FLASHER_BINARY + "\n");
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

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mTargetWidth = size.x;
        mTargetHeight = size.y;

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

                int gcd = mTargetWidth;
                int gcd1 = mTargetHeight;

                // get aspect ratio
                while (gcd != 0 && gcd1 != 0){
                    if(gcd > gcd1)
                        gcd = gcd % gcd1;
                    else
                        gcd1 = gcd1 % gcd;
                }

                gcd = gcd1 == 0? gcd: gcd1;

                photoPickerIntent.setType("image/*");
                photoPickerIntent.putExtra("aspectX", mTargetWidth / gcd);
                photoPickerIntent.putExtra("aspectY", mTargetHeight / gcd);
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
                    mDownsampling = resultIntent.getIntExtra(TAG_DOWNSAMPLING, DOWNSAMPLE_565_FFMPEG);
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
