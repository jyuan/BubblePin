package bubblepin.com.bubblepin.loginModule;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;

import com.parse.ParseException;
import com.parse.ParseUser;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import bubblepin.com.bubblepin.GoogleMapActivity;
import bubblepin.com.bubblepin.MyApplication;
import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.util.ParseUtil;
import bubblepin.com.bubblepin.util.PreferenceUtil;
import bubblepin.com.bubblepin.util.ValidateUtil;

public class SignUpActivity extends Activity implements LoaderCallbacks<Cursor> {

    public static final String SIGN_UP_SUCCESS = "sign_up_success";

    private EditText nameText;
    private AutoCompleteTextView emailText;
    private EditText passwordText;
    private ImageView signUpButton;

    private View progressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        MyApplication.getInstance().addActivity(this);

        initViewById();

        signUpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSignUp();
            }
        });
    }

    private void initViewById() {
        progressView = findViewById(R.id.signUp_progress);

        nameText = (EditText) this.findViewById(R.id.name_signUpActivity);
        emailText = (AutoCompleteTextView) this.findViewById(R.id.email_signUpActivity);
        populateAutoComplete();

        passwordText = (EditText) this.findViewById(R.id.password_signUpActivity);
        signUpButton = (ImageView) this.findViewById(R.id.loginButton_signUpActivity);
    }

    private void populateAutoComplete() {
        getLoaderManager().initLoader(0, null, this);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptSignUp() {
        // Reset errors.
        nameText.setError(null);
        emailText.setError(null);
        passwordText.setError(null);

        // Store values at the time of the login attempt.
        String name = nameText.getText().toString();
        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // check for a valid password
        if (TextUtils.isEmpty(password)) {
            passwordText.setError(getString(R.string.error_field_required));
            focusView = passwordText;
            cancel = true;
        } else if (!ValidateUtil.isPasswordValid(password)) {
            passwordText.setError(getString(R.string.error_password));
            focusView = passwordText;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            emailText.setError(getString(R.string.error_field_required));
            focusView = emailText;
            cancel = true;
        } else if (!ValidateUtil.isValidEmail(email)) {
            emailText.setError(getString(R.string.error_invalid_email));
            focusView = emailText;
            cancel = true;
        }

        // check for a valid username
        if (TextUtils.isEmpty(name)) {
            nameText.setError(getString(R.string.error_field_required));
            focusView = nameText;
            cancel = true;
        } else if (name.length() < 4) {
            nameText.setError(getString(R.string.error_invalid_name));
            focusView = nameText;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);
            signUp(email, name, password);
        }
    }

    /**
     * aim to test Parse(www.parse.com), here we suppose that email is the unique username,
     * and the username here is the nickname for a specific user, also should be unique
     *
     * @param email    unique email address
     * @param username user nickname
     * @param password user password
     */
    private void signUp(String email, String username, String password) {
        try {
            if (!ParseUtil.isUniqueWhenSignUp(username, ParseUtil.USER_NICKNAME)) {
                showProgress(false);
                nameText.requestFocus();
                nameText.setError(getString(R.string.error_exist_username));
            } else if (!ParseUtil.isUniqueWhenSignUp(email, ParseUtil.USER_EMAIL)) {
                showProgress(false);
                emailText.requestFocus();
                emailText.setError(getString(R.string.error_exist_email));
            } else {
                ParseUtil.signUp(email, username, password);
                if (ParseUser.getCurrentUser() != null) {
                    PreferenceUtil.setBoolean(SignUpActivity.this, PreferenceUtil.LOGIN_INFO, true);
                    Intent intent = new Intent(this, GoogleMapActivity.class);
                    intent.putExtra(SIGN_UP_SUCCESS, SIGN_UP_SUCCESS);
                    startActivity(intent);
                    finish();
                }
                showProgress(false);
            }
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(), "sign up failed: " + e.getMessage());
        }
    }

    // the code below is build by template

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<String>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }
        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(SignUpActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        emailText.setAdapter(adapter);
    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };
        int ADDRESS = 0;
    }
}



