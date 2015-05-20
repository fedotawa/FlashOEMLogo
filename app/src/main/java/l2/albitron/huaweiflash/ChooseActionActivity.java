package l2.albitron.huaweiflash;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;


public class ChooseActionActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_action);

        PackageInfo info = null;
        String version = "";
        try {
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView textVersion = (TextView)findViewById(R.id.textVersion);
        textVersion.setText(getResources().getString(R.string.textAppVersion) + " " + version);

        WebView view = new WebView(this);

        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        rlp.addRule(RelativeLayout.BELOW, R.id.buttonHardware);
        view.setLayoutParams(rlp);
        view.setBackgroundColor(0x00000000);
        view.setVerticalScrollBarEnabled(false);
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        WebSettings settings = view.getSettings();
        settings.setDefaultTextEncodingName("utf-8");

        ((RelativeLayout)findViewById(R.id.chooserLayout)).addView(view);

        view.loadDataWithBaseURL(null, getString(R.string.bsdClause), "text/html", "utf-8", null);

        final Button flashImage = (Button)findViewById(R.id.buttonFlashImage);
        flashImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent flashIntent = new Intent(ChooseActionActivity.this, FlashActivity.class);
                ChooseActionActivity.this.startActivity(flashIntent);
            }
        });

        final Button flashMBN = (Button)findViewById(R.id.buttonFlashMBN);
        flashMBN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flashImage.setEnabled(false);
                flashMBN.setEnabled(false);
                flashMBN.setText(getResources().getString(R.string.textPleaseWait));
                flashFromMedia();
            }
        });

        final Button buttonHardware = (Button)findViewById(R.id.buttonHardware);
        buttonHardware.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent hardwareIntent = new Intent(ChooseActionActivity.this, SupportedHardware.class);
                ChooseActionActivity.this.startActivity(hardwareIntent);
            }
        });
    }

    private void flashFromMedia()
    {


        new AsyncTask<Void, Void, Boolean>() {
            protected Boolean doInBackground(Void... params) {

                Boolean result = false;

                try {
                    Process su = Runtime.getRuntime().exec("su");
                    DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

                    String libdir = getApplicationInfo().nativeLibraryDir;

                    if (new File(FlashActivity.OEMLOGO_PATH).exists()) {
                        outputStream.writeBytes("export LD_LIBRARY_PATH=" + libdir + "\n");
                        outputStream.flush();

                        // this isn't actually a library, just a renamed binary to trick the IDE
                        outputStream.writeBytes(libdir + "/libflash_oemlogo.so\n");
                        outputStream.flush();
                        result = true;
                    }

                    outputStream.writeBytes("exit\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return result;
            }

            protected void onProgressUpdate(Void... progress) {
            }

            protected void onPostExecute(Boolean result) {
                Button flashImage = (Button)findViewById(R.id.buttonFlashImage);
                Button flashMBN = (Button)findViewById(R.id.buttonFlashMBN);

                flashImage.setEnabled(true);
                flashMBN.setEnabled(true);
                flashMBN.setText(getResources().getString(R.string.textFlashOEMLogoMBN));

                if (!result)
                {
                    AlertDialog.Builder dlgAlert  =
                            new AlertDialog.Builder(ChooseActionActivity.this);
                    dlgAlert.setMessage(getResources().getString(R.string.textNoMBN));
                    dlgAlert.setTitle(getResources().getString(R.string.textNoMBNError));
                    dlgAlert.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    dlgAlert.setCancelable(true);
                    dlgAlert.create().show();
                }
            }
        }.execute();
    }

}
