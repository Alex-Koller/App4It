package com.dreambig.app4it;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dreambig.app4it.api.FirebaseUserProfileCallback;
import com.dreambig.app4it.api.SuccessOrFailureCallback;
import com.dreambig.app4it.entity.App4ItUserProfile;
import com.dreambig.app4it.helper.A4ItHelper;
import com.dreambig.app4it.impl.App4ItUserProfileManager;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.util.FileUtil;
import com.dreambig.app4it.util.ImageUtil;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.firebase.client.FirebaseError;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyProfileActivity extends Activity  {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_CROP = 2;
    private static final int REQUEST_PICK_IMAGE = 3;

    private File getPhotoImageFile() {
        String currentPhotoFileName = "JPEG_BeApp4It_Profile.jpg";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(storageDir,currentPhotoFileName);
    }

    public static File getCropLinkImageFile() {
        String fileName = "JPEG_BeApp4It_Profile_CropLink.jpg";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(storageDir,fileName);
    }

    private void dispatchTakePictureIntent() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File currentPhotoFile = getPhotoImageFile();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(currentPhotoFile));
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }  else {
            Toast.makeText(this,"Your phone doesn't seem to have a camera installed",Toast.LENGTH_LONG).show();
        }

    }

    private void dispatchPictureFromGallery() {
        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_PICK_IMAGE);
    }

    private App4ItApplication getDelegate() {
        return (App4ItApplication)getApplication();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);
        readSavedInstance(savedInstanceState);
        giveButtonsActions();

        putWaitASecVeilOn();
        FirebaseGateway firebaseGateway = new FirebaseGateway(this);
        firebaseGateway.downloadFullProfileForUserId(getDelegate().getLoggedInUserId(), new FirebaseUserProfileCallback() {
            @Override
            public void acceptUserProfile(App4ItUserProfile userProfile, FirebaseError error) {

                putWaitASecVeilOff();

                if(error != null) {
                    profileImageAppearance(false,true,"Failed downloading :-(");
                    Toast.makeText(MyProfileActivity.this,"Failed to download your profile :-(",Toast.LENGTH_SHORT).show();
                } else if (userProfile != null) {
                    getMyProfileNameView().setText(userProfile.getName());
                    if(userProfile.getPicture() != null) {
                        profileImageAppearance(true,false,null);
                        getMyProfilePictureView().setImageBitmap(userProfile.getPicture());
                    } else {
                        profileImageAppearance(false,true,"We are missing your photo");
                    }
                } else {
                    profileImageAppearance(false,true,"We are missing your photo");
                }
            }
        });
    }

    private void profileImageAppearance(boolean imageVisible, boolean noImageTextVisible, String imageText) {
        ImageView imageView = getMyProfilePictureView();
        TextView noImageTextView = getMyProfileNoPictureTextView();

        if(imageVisible) {
            imageView.setVisibility(View.VISIBLE);
        }
        else {
            imageView.setVisibility(View.GONE);
        }

        if(noImageTextVisible) {
            noImageTextView.setVisibility(View.VISIBLE);
            noImageTextView.setText(imageText);
        } else {
            noImageTextView.setVisibility(View.GONE);
        }
    }

    private void readSavedInstance(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            Bitmap bitmap = savedInstanceState.getParcelable(MessageIdentifiers.SHOWING_PROFILE_PICTURE);
            if(bitmap != null) {
                getMyProfilePictureView().setImageBitmap(bitmap);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);


        if(getMyProfilePictureView().getDrawable() != null && ((BitmapDrawable)getMyProfilePictureView().getDrawable()).getBitmap() != null) {
            outState.putParcelable(MessageIdentifiers.SHOWING_PROFILE_PICTURE,((BitmapDrawable)getMyProfilePictureView().getDrawable()).getBitmap());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            dispatchImageForCropping(Uri.fromFile(getPhotoImageFile()));
        } else if (requestCode == REQUEST_IMAGE_CROP && resultCode == RESULT_OK) {
            receiveCroppedImage();
        } else if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            dispatchImageForCropping(data.getData());
        }
    }

    private void receiveCroppedImage() {
        profileImageAppearance(true,false,null);
        Bitmap bitmap = BitmapFactory.decodeFile(getCropLinkImageFile().getPath());
        if(bitmap == null) {
            Toast.makeText(this,"Failed reading the cropped image",Toast.LENGTH_SHORT).show();
        } else {
            getMyProfilePictureView().setImageBitmap(bitmap);
        }
    }

    private void dispatchImageForCropping(Uri uri) {
        Intent intent = new Intent(getApplicationContext(), CropActivity.class);
        intent.putExtra(MessageIdentifiers.URI_TO_IMAGE_TO_CROP, uri);
        startActivityForResult(intent, REQUEST_IMAGE_CROP);
    }

    private void giveButtonsActions() {

        Button cameraButton = (Button)findViewById(R.id.myProfileCameraButton);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        Button galleryButton = (Button)findViewById(R.id.myProfileGalleryButton);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchPictureFromGallery();
            }
        });
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.myprofile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_save:
                handleSaveClick();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleSaveClick() {

        A4ItHelper.hideKeyboard(this,getMyProfileNameView());
        putWaitASecVeilOn();


        String nameCandidate = getMyProfileNameView().getText().toString().trim();
        if("".equals(nameCandidate)) {
            nameCandidate = null;
        }
        final String name = nameCandidate;

        Bitmap bigImage = getCurrentlyShowingImage();
        final Bitmap scaledDownImage;

        if(bigImage != null) {
            scaledDownImage = ImageUtil.toSmallImageVersion(bigImage);
        } else {
            scaledDownImage = null;
        }

        FirebaseGateway firebaseGateway = new FirebaseGateway(this);
        firebaseGateway.updateProfileForUserId(getDelegate().getLoggedInUserId(),name,bigImage,scaledDownImage,new SuccessOrFailureCallback() {
            @Override
            public void callback(boolean success, String errorMessage) {

                putWaitASecVeilOff();

                if(!success) {
                    Toast.makeText(MyProfileActivity.this, "Failed saving your new profile :-(", Toast.LENGTH_SHORT).show();
                } else {
                    App4ItUserProfileManager.useThisUserProfileForUserId(MyProfileActivity.this,getDelegate().getLoggedInUserId(),new App4ItUserProfile(name,scaledDownImage,scaledDownImage != null)); //it appears to work fine without this too. just to be sure
                    finish();
                }
            }
        });

    }

    private Bitmap getCurrentlyShowingImage() {
        ImageView imageView = getMyProfilePictureView();

        if(imageView.getDrawable() != null && ((BitmapDrawable)imageView.getDrawable()).getBitmap() != null) {
            return ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        } else {
            return null;
        }
    }

    private ImageView getMyProfilePictureView() {
        return (ImageView)findViewById(R.id.myProfilePicture);
    }

    private TextView getMyProfileNoPictureTextView() {
        return (TextView)findViewById(R.id.myProfileNoPictureText);
    }

    private EditText getMyProfileNameView() {
        return (EditText)findViewById(R.id.myProfileNameField);
    }

    private void putWaitASecVeilOn() {
        findViewById(R.id.myProfileLoadingNotice).setVisibility(View.VISIBLE);
    }

    private void putWaitASecVeilOff() {
        findViewById(R.id.myProfileLoadingNotice).setVisibility(View.GONE);
    }



}
