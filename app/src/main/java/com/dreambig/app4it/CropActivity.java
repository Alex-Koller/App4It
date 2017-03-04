package com.dreambig.app4it;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.dreambig.app4it.helper.A4ItHelper;
import com.dreambig.app4it.util.FileUtil;
import com.dreambig.app4it.util.ImageUtil;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.isseiaoki.simplecropview.CropImageView;

import java.io.FileOutputStream;

public class CropActivity extends Activity  {

    private App4ItApplication getDelegate() {
        return (App4ItApplication)getApplication();	
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        showTheImage((Uri)getIntent().getParcelableExtra(MessageIdentifiers.URI_TO_IMAGE_TO_CROP));
    }

    private CropImageView getCropImageView() {
        return (CropImageView)findViewById(R.id.cropImageView);
    }

    private void showTheImage(Uri uriToImageToCrop) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uriToImageToCrop);
            getCropImageView().setImageBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this,"Failed reading the image to crop",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.crop, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_done:
                handleDoneClick();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleDoneClick() {

        FileOutputStream fio = null;
        int resultCode;

        try {
            fio = new FileOutputStream(MyProfileActivity.getCropLinkImageFile());
            ImageUtil.compressBitmapToJPEGFile(getCropImageView().getCroppedBitmap(), 80, fio);
            resultCode = RESULT_OK;
        } catch (Exception e) {
            Toast.makeText(this,"Failed saving the cropped image :-(",Toast.LENGTH_SHORT).show();
            resultCode = RESULT_CANCELED;
        } finally {
            FileUtil.attemptToClose(fio);
        }

        setResult(resultCode);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getDelegate().activityStops();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getDelegate().activityStarts(null);
    }
}
