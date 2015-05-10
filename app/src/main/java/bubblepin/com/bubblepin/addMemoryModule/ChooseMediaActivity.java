package bubblepin.com.bubblepin.addMemoryModule;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

import bubblepin.com.bubblepin.MyApplication;
import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.util.ImageUtil;

public class ChooseMediaActivity extends Activity implements View.OnClickListener {

    private static final int CAPTURE_IMAGE = 1;
    private static final int UPLOAD_IMAGE = 2;

    private ImageUtil imageUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_media);

        MyApplication.getInstance().addActivity(this);

        imageUtil = new ImageUtil(this);
        findViewById();
    }

    private void findViewById() {
        ImageView cancelButton = (ImageView) findViewById(R.id.choose_media_cancel);
        ImageView pictureButton = (ImageView) findViewById(R.id.choose_media_picture);
        ImageView cameraButton = (ImageView) findViewById(R.id.choose_media_camera);
        ImageView vedioButton = (ImageView) findViewById(R.id.choose_media_vedio);
        ImageView recordingButton = (ImageView) findViewById(R.id.choose_media_recording);
        ImageView musicButton = (ImageView) findViewById(R.id.choose_media_music);
        ImageView recommendButton = (ImageView) findViewById(R.id.choose_media_recommend);

        cancelButton.setOnClickListener(this);
        pictureButton.setOnClickListener(this);
        cameraButton.setOnClickListener(this);
        vedioButton.setOnClickListener(this);
        recordingButton.setOnClickListener(this);
        musicButton.setOnClickListener(this);
        recommendButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.choose_media_cancel:
                setResult(AddMemoryActivity.CANCEL);
                finish();
                break;
            case R.id.choose_media_camera:
                addCamera();
                break;
            case R.id.choose_media_picture:
                addPicture();
                break;
//            case R.id.choose_media_music:
//                addMusic();
//                break;
//            case R.id.choose_media_recording:
//                addRecording();
//                break;
//            case R.id.choose_media_vedio:
//                addVedio();
//                break;
//            case R.id.choose_media_recommend:
//                addRecommend();
//                break;
            default:
                showToast("Coming Soon");
        }
    }

    /**
     * start a new intent to perform and request image capture
     * and also create a new file to store current captured image
     * file on SD card
     */
    private void addCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imageFile = imageUtil.generateImageFile();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
        startActivityForResult(intent, CAPTURE_IMAGE);
    }

    /**
     * choose image from gallery
     */
    private void addPicture() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent,
                getString(R.string.testImage_uploadTitle)), UPLOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == CAPTURE_IMAGE) {
                File imageFile = imageUtil.getImageFile();
                String imageFilePath = imageFile.getAbsolutePath();
                Uri imageUri = Uri.fromFile(imageFile);

                Intent intent = getIntent();
                intent.putExtra(AddMemoryActivity.CAPTURE_IMAGE_TYPE, AddMemoryActivity.IMAGE_FROM_CAMERA);
                intent.putExtra(AddMemoryActivity.CAPTURE_IMAGE_PATH, imageFilePath);
                intent.putExtra(AddMemoryActivity.CAPTURE_BITMAP_PATH, imageUri.toString());
                setResult(AddMemoryActivity.HANDLE_IMAGE, intent);
                finish();

            } else if (requestCode == UPLOAD_IMAGE) {
                Uri imageUri = data.getData();
                String imageFilePath = imageUtil.getRealPathFromURI(Build.VERSION.SDK_INT, imageUri);

                Intent intent = getIntent();
                intent.putExtra(AddMemoryActivity.CAPTURE_IMAGE_TYPE, AddMemoryActivity.IMAGE_FROM_GALLERY);
                intent.putExtra(AddMemoryActivity.CAPTURE_IMAGE_PATH, imageFilePath);
                intent.putExtra(AddMemoryActivity.CAPTURE_BITMAP_PATH, imageUri.toString());
                setResult(AddMemoryActivity.HANDLE_IMAGE, intent);
                finish();
            }
        }
    }

    private void addMusic() {

    }

    private void addRecording() {

    }

    private void addVedio() {

    }

    private void addRecommend() {

    }

    private void showToast(String info) {
        Toast.makeText(this, info, Toast.LENGTH_LONG).show();
    }

}
