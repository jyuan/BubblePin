package bubblepin.com.bubblepin.profileModule;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.parse.ParseException;
import com.parse.ParseUser;

import bubblepin.com.bubblepin.MyApplication;
import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.util.ParseUtil;

public class ProfileEditActivity extends ActionBarActivity {

    private EditText cityEdit, countryEdit;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);
        MyApplication.getInstance().addActivity(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        cityEdit = (EditText) findViewById(R.id.profile_edit_city);
        countryEdit = (EditText) findViewById(R.id.profile_edit_country);
        ImageView saveButton = (ImageView) findViewById(R.id.profile_edit_submit);

        getUserInfo();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String city = cityEdit.getText().toString().trim();
                    String country = countryEdit.getText().toString().trim();
                    Log.i(getClass().getSimpleName(), "city: " + city + ", country: " + country);
                    ParseUtil.saveUserInfo(city, country);
                    updateProfile();
                    finish();
                } catch (ParseException e) {
                    Log.e(getClass().getSimpleName(),
                            "save city and country info error: " + e.getMessage());
                }
            }
        });
    }

    private void getUserInfo() {
        ParseUser parseUser = ParseUser.getCurrentUser();
        String city = parseUser.getString(ParseUtil.USER_CITY);
        String country = parseUser.getString(ParseUtil.USER_COUNTRY);

        cityEdit.setText(city);
        countryEdit.setText(country);
    }

    private void updateProfile() {
        ProfileActivity.refreshUserInfo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
