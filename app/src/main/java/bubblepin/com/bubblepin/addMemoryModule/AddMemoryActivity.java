package bubblepin.com.bubblepin.addMemoryModule;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ProgressCallback;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import bubblepin.com.bubblepin.GoogleMapActivity;
import bubblepin.com.bubblepin.MyApplication;
import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.util.ImageUtil;
import bubblepin.com.bubblepin.util.ParseUtil;

public class AddMemoryActivity extends Activity implements View.OnClickListener {

    public static final int MEDIA = 1;
    public static final int HANDLE_IMAGE = 10;
    public static final int CANCEL = -10;

    public static final String CAPTURE_IMAGE_PATH = "capture_image_path";
    public static final String CAPTURE_BITMAP_PATH = "capture_bitmap_path";
    public static final String CAPTURE_IMAGE_TYPE = "capture_image_type";
    public static final String IMAGE_FROM_CAMERA = "image_from_camera";
    public static final String IMAGE_FROM_GALLERY = "image_from_gallery";

    private EditText titleEditText, descriptionEditText;
    private TextView dateTextView, privacyTextView,
            chooseMediaTitleTextView, chooseMediaDeleteTextView;
    private ImageView mediaDelete, addMediaButton, submitButton, greyButton;
    private ImageView mediaPicture;

    private ProgressDialog progressDialog;

    private ImageUtil imageUtil;

    // date
    private int mYear;
    private int month;
    private int day;
    // privacy
    private String[] privacy;
    private int privacySelect = 0;

    // submit info
    // the path of the image that prepared to submit into server
    private String imagePath;
    // image resource type: IMAGE_FROM_CAMERA or IMAGE_FROM_GALLERY
    private String imageType;
    // get text from editView
    private String title = "";
    private String description = "";

    private String address;
    private ParseGeoPoint parseGeoPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressDialog = new ProgressDialog(this);

        MyApplication.getInstance().addActivity(this);

        Intent intent = getIntent();
        double latitude = intent.getExtras().getDouble(GoogleMapActivity.LATITUDE);
        double longitude = intent.getExtras().getDouble(GoogleMapActivity.LONGITUDE);
        address = intent.getExtras().getString(GoogleMapActivity.ADDRESS);
        parseGeoPoint = new ParseGeoPoint(latitude, longitude);

        setContentView(R.layout.activity_add_memory);

        imageUtil = new ImageUtil(this);

        findViewById();
        initialCurrentDate();
        privacy = new String[]{getString(R.string.privacy_friend), getString(R.string.privacy_private)};
    }

    private void findViewById() {
        titleEditText = (EditText) findViewById(R.id.add_memory_title);
        descriptionEditText = (EditText) findViewById(R.id.add_memory_description);
        addMediaButton = (ImageView) findViewById(R.id.choose_media_button);
        submitButton = (ImageView) findViewById(R.id.add_media_submit);
        greyButton = (ImageView) findViewById(R.id.add_media_grey_submit);
        LinearLayout addDateButton = (LinearLayout) findViewById(R.id.add_date);
        LinearLayout addPrivacyButton = (LinearLayout) findViewById(R.id.add_privacy);
        mediaPicture = (ImageView) findViewById(R.id.add_media_picture);
        dateTextView = (TextView) findViewById(R.id.date);
        chooseMediaDeleteTextView = (TextView) findViewById(R.id.choose_media_delete_text);
        privacyTextView = (TextView) findViewById(R.id.privacy);
        chooseMediaTitleTextView = (TextView) findViewById(R.id.choose_media_text);
        mediaDelete = (ImageView) findViewById(R.id.choose_media_delete);

        addMediaButton.setOnClickListener(this);
        submitButton.setOnClickListener(this);
        addDateButton.setOnClickListener(this);
        addPrivacyButton.setOnClickListener(this);
        mediaPicture.setOnClickListener(this);
        mediaDelete.setOnClickListener(this);

        addTextChangedListener();
    }

    private void addTextChangedListener() {
        titleEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                title = titleEditText.getText().toString().trim();
                setSubmitButton();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        descriptionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                description = descriptionEditText.getText().toString().trim();
                setSubmitButton();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void setSubmitButton() {
        if (title.length() > 0 && description.length() > 0) {
            submitButton.setVisibility(View.VISIBLE);
            greyButton.setVisibility(View.GONE);
        } else {
            submitButton.setVisibility(View.GONE);
            greyButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.choose_media_button:
                startActivityForResult(new Intent(this, ChooseMediaActivity.class), MEDIA);
                break;
            case R.id.add_media_submit:
                submit();
                break;
            case R.id.add_date:
                addDate();
                break;
            case R.id.add_privacy:
                addPrivacy();
                break;
            case R.id.choose_media_delete:
                changeMediaUI(false);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MEDIA) {
            if (resultCode == HANDLE_IMAGE) {
                imagePath = data.getStringExtra(CAPTURE_IMAGE_PATH);
                imageType = data.getStringExtra(CAPTURE_IMAGE_TYPE);

                changeMediaUI(true);

                // compress the image
                Bitmap bitmap;
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                bitmap = BitmapFactory.decodeFile(imagePath, options);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 30, out);
                mediaPicture.setImageBitmap(bitmap);

                showToast(getString(R.string.get_image_success));
            }
        }
    }

    /**
     * show the media in the UI or not
     * if flag is true, shows the media and hide the basic initial UI
     * if flag is not, shows the initial UI
     *
     * @param flag true: has picture
     */
    private void changeMediaUI(boolean flag) {
        if (flag) {
            chooseMediaDeleteTextView.setVisibility(View.VISIBLE);
            mediaPicture.setVisibility(View.VISIBLE);
            mediaDelete.setVisibility(View.VISIBLE);
            chooseMediaTitleTextView.setVisibility(View.GONE);
            addMediaButton.setVisibility(View.GONE);
        } else {
            mediaPicture.setVisibility(View.GONE);
            mediaPicture.setImageBitmap(null);
            chooseMediaDeleteTextView.setVisibility(View.GONE);
            mediaDelete.setVisibility(View.GONE);
            chooseMediaTitleTextView.setVisibility(View.VISIBLE);
            addMediaButton.setVisibility(View.VISIBLE);
            imagePath = null;
            imageType = ParseUtil.TEXT;
        }
    }

    /**
     * get the current date and show into the date TextView
     */
    private void initialCurrentDate() {
        final Calendar cal = Calendar.getInstance();
        mYear = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        day = cal.get(Calendar.DAY_OF_MONTH);
        updateDisplay();
    }

    /**
     * Choose memory
     */
    private void addDate() {
        DatePickerDialog.OnDateSetListener pDateSetListener =
                new DatePickerDialog.OnDateSetListener() {

                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        mYear = year;
                        month = monthOfYear;
                        day = dayOfMonth;
                        updateDisplay();
                    }
                };
        DatePickerDialog dialog = new DatePickerDialog(this,
                pDateSetListener, mYear, month, day);
        dialog.getDatePicker().setMaxDate(new Date().getTime());
        dialog.show();
    }

    /**
     * update the date in the TextView
     */
    private void updateDisplay() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append(month + 1).append("/")
                .append(day).append("/")
                .append(mYear).append(" ");
        dateTextView.setText(stringBuilder);
    }

    /**
     * Using Dialog to choose the privacy status
     */
    private void addPrivacy() {
        Dialog myDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_privacy_status))
                .setSingleChoiceItems(privacy, privacySelect,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                privacySelect = which;
                                privacyTextView.setText(privacy[which]);
                                dialog.dismiss();
                            }
                        })
                .create();
        myDialog.show();
    }

    /**
     * submit and return back to GoogleMapActivity
     */
    private void submit() {
        // Media Type: Text
        if (null == imagePath || imagePath.equals("")) {
            saveText();
        } else {
            // Media Type: Image
            try {
                File file = new File(imagePath);
                Bitmap original = BitmapFactory.decodeFile(imagePath);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                original.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
                byte[] byteArray = outputStream.toByteArray();

                final ParseFile parseFile;

                if (imageType.equals(IMAGE_FROM_CAMERA)) {
                    parseFile = new ParseFile(file.getName(), byteArray);
                } else {
                    parseFile = new ParseFile(imageUtil.generateImageFileName(), byteArray);
                }
                saveImage(parseFile);
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "Error: " + e.getMessage());
            }
        }
    }

    /**
     * Save Media into server
     * Media Type: Text
     */
    private void saveText() {
        String privacy = privacyTextView.getText().toString();
        try {
            ParseUtil.saveMemoryIntoParse(address, parseGeoPoint, title, description,
                    getDate(), privacy, ParseUtil.TEXT, null);
            showDialog(getString(R.string.upload_text_success), true);
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(), "save text error: " + e.getMessage());
            showToast(e.getMessage());
        }
    }

    /**
     * Two sources:
     * 1. Save image file from taking photo on SD card and also upload the image
     * on Parse cloud server(www.parse.com)
     * 2. Upload the file from gallery on Parse cloud server(www.parse.com)
     *
     * @param parseFile parseFile that is going to save
     * @throws java.io.IOException
     */
    private void saveImage(final ParseFile parseFile) throws IOException {
        parseFile.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (null == e) {
                    progressDialog.dismiss();
                    String privacy = privacyTextView.getText().toString();
                    try {
                        ParseUtil.saveMemoryIntoParse(address, parseGeoPoint, title, description,
                                getDate(), privacy, ParseUtil.IMAGE, parseFile);
                        showDialog(getString(R.string.upload_image_success), true);
                    } catch (ParseException e1) {
                        showToast(e1.getMessage());
                        Log.e(getClass().getSimpleName(), "save image error: " + e1.getMessage());
                    }
                } else {
                    showToast(e.getMessage());
                }
            }
        }, new ProgressCallback() {
            @Override
            public void done(Integer integer) {
                progressDialog.setMax(100);
                progressDialog.setMessage(getString(R.string.uploading) + parseFile.getName());
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setProgress(integer);
                progressDialog.show();
            }
        });
    }

    /**
     * show dialog
     *
     * @param message message shows on the dialog
     * @param success whether the memory is success upload to parse or not
     */
    private void showDialog(String message, final boolean success) {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(this);
        // set title
        alertDialogBuilder.setTitle(getString(R.string.upload_memory));
        // set dialog message
        alertDialogBuilder
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (success) {
                            updateMap();
                            setResult(RESULT_OK);
                            finish();
                        }
                        dialog.dismiss();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /**
     * get Current date
     *
     * @return current date
     */
    private Date getDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(mYear, month, day);
        return cal.getTime();
    }

    private void showToast(String info) {
        Toast.makeText(this, info, Toast.LENGTH_LONG).show();
    }

    private void updateMap() {
        GoogleMapActivity.refreshUpdateMarkers();
    }
}
