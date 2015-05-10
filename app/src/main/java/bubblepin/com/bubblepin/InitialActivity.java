package bubblepin.com.bubblepin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import bubblepin.com.bubblepin.loginModule.LoginActivity;
import bubblepin.com.bubblepin.loginModule.SignUpActivity;


public class InitialActivity extends Activity {

    ImageView loginButton, takeTourButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);

        loginButton = (ImageView) findViewById(R.id.loginButton_mainAcitivity);
        takeTourButton = (ImageView) findViewById(R.id.takeTourButton_mainAcitivity);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(InitialActivity.this, LoginActivity.class));
                InitialActivity.this.finish();
            }
        });

        takeTourButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(InitialActivity.this, SignUpActivity.class));
                InitialActivity.this.finish();
            }
        });
    }
}
