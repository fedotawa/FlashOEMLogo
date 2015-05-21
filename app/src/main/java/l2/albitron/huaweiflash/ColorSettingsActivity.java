package l2.albitron.huaweiflash;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;


public class ColorSettingsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RadioButton radio444 = (RadioButton)findViewById(R.id.radio444);
        RadioButton radio454 = (RadioButton)findViewById(R.id.radio454);
        RadioButton radio555 = (RadioButton)findViewById(R.id.radio555);
        RadioButton radio555ff = (RadioButton)findViewById(R.id.radio555ff);
        RadioButton radio565 = (RadioButton)findViewById(R.id.radio565);
        RadioButton radio565table = (RadioButton)findViewById(R.id.radio565table);
        RadioButton radio565ff = (RadioButton)findViewById(R.id.radio565ff);
        RadioButton radioNoDownsampling = (RadioButton)findViewById(R.id.radioNoDownsampling);

        switch (getIntent().getIntExtra(FlashActivity.TAG_DOWNSAMPLING,
                FlashActivity.DOWNSAMPLE_565_FFMPEG))
        {
            case FlashActivity.DOWNSAMPLE_444:
                radio444.setChecked(true);
                break;
            case FlashActivity.DOWNSAMPLE_454:
                radio454.setChecked(true);
                break;
            case FlashActivity.DOWNSAMPLE_555:
                radio555.setChecked(true);
                break;
            case FlashActivity.DOWNSAMPLE_555_FFMPEG:
                radio555ff.setChecked(true);
                break;
            case FlashActivity.DOWNSAMPLE_565:
                radio565.setChecked(true);
                break;
            case FlashActivity.DOWNSAMPLE_565_TABLE:
                radio565table.setChecked(true);
                break;
            case FlashActivity.DOWNSAMPLE_565_FFMPEG:
                radio565ff.setChecked(true);
                break;
            default:
                radioNoDownsampling.setChecked(true);
                break;
        }

        radio444.setTag(R.id.value_tag, new Integer(FlashActivity.DOWNSAMPLE_444));
        radio454.setTag(R.id.value_tag, new Integer(FlashActivity.DOWNSAMPLE_454));
        radio555.setTag(R.id.value_tag, new Integer(FlashActivity.DOWNSAMPLE_555));
        radio555ff.setTag(R.id.value_tag, new Integer(FlashActivity.DOWNSAMPLE_555_FFMPEG));
        radio565.setTag(R.id.value_tag, new Integer(FlashActivity.DOWNSAMPLE_565));
        radio565table.setTag(R.id.value_tag, new Integer(FlashActivity.DOWNSAMPLE_565_TABLE));
        radio565ff.setTag(R.id.value_tag, new Integer(FlashActivity.DOWNSAMPLE_565_FFMPEG));
        radioNoDownsampling.setTag(R.id.value_tag, new Integer(FlashActivity.NO_DOWNSAMPLING));

        View.OnClickListener radioListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(FlashActivity.TAG_DOWNSAMPLING, (Integer)v.getTag(R.id.value_tag));
                setResult(RESULT_OK, intent);
                finish();
            }
        };

        radio444.setOnClickListener(radioListener);
        radio454.setOnClickListener(radioListener);
        radio555.setOnClickListener(radioListener);
        radio555ff.setOnClickListener(radioListener);
        radio565.setOnClickListener(radioListener);
        radio565table.setOnClickListener(radioListener);
        radio565ff.setOnClickListener(radioListener);
        radioNoDownsampling.setOnClickListener(radioListener);

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
