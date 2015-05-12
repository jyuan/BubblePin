package bubblepin.com.bubblepin.loginModule;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;

import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.util.ParseUtil;

public class ForgetPasswordActivity extends ActionBarActivity {

    private AutoCompleteTextView emailTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        emailTextView = (AutoCompleteTextView) findViewById(R.id.forget_password_edit);
        ImageView submitButton = (ImageView) findViewById(R.id.forget_password_button);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailTextView.getText().toString();
                sendEmail(email);
            }
        });
    }

    /**
     * Parse doucment: https://parse.com/docs/android/guide#users-resetting-passwords
     *
     * @param email user input
     */
    private void sendEmail(String email) {
        try {
            boolean isEmailExist = ParseUtil.isEmailExistInServer(email);
            if (isEmailExist) {
                ParseUser.requestPasswordResetInBackground(email, new RequestPasswordResetCallback() {
                    public void done(ParseException e) {
                        if (e == null) {
                            sendEmailSuccessDialog();
                        } else {
                            String error = getString(R.string.send_email_error) + e.getMessage();
                            Log.e(getClass().getSimpleName(), error);
                            showToast(error);
                        }
                    }
                });
            } else {
                showToast(getString(R.string.email_not_exist));
                emailTextView.setText("");
            }
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(), "check for the email error:" + e.getMessage());
        }

    }

    private void sendEmailSuccessDialog() {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.success));
        alertDialogBuilder
                .setMessage(getString(R.string.send_email_success_message))
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(ForgetPasswordActivity.this, LoginActivity.class);
                        startActivity(intent);
                        dialog.dismiss();
                        finish();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_forget_password, menu);
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
