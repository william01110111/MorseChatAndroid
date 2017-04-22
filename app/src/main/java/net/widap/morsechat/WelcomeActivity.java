package net.widap.morsechat;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;

public class WelcomeActivity extends AppCompatActivity {

	private boolean hasAppeared = false;
	private boolean isVisable = false;

	View loadingSpinner;
	View signInBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	    FirebaseHelper.init();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
	    loadingSpinner=findViewById(R.id.loadingSpinner);
	    signInBtn=findViewById(R.id.logInBtn);
    }

    public void signInClicked(View v) {

	    SignInActivity.createAccount = false;
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
    }

	@Override
	public void onStart() {
		super.onStart();
		isVisable = true;

		//errorMsgLabel.hidden = true
		//viewHasAppeared = false

		FirebaseHelper.stateChangedListener=new UpdateLoginState();
		FirebaseHelper.stateChangedListener.loginStateChanged();
	}

	@Override
	public void onStop() {
		super.onStop();
		isVisable = false;
	}

	void showSpinner()
	{
		if (hasAppeared)
		{
			loadingSpinner.setVisibility(View.VISIBLE);
			signInBtn.setVisibility(View.INVISIBLE);
		}
	}

	void showBtn() {
		if (hasAppeared) {
			signInBtn.setVisibility(View.VISIBLE);
			//createAccountBtn.hidden = false
			loadingSpinner.setVisibility(View.INVISIBLE);
		}
	}

	void showError(String msg) {

		//errorMsgLabel.text = msg;
		//errorMsgLabel.hidden = false;
	}

	/*func startLoginUI() {
		if viewHasAppeared {
			firebaseHelper.loginUI(self)
		}
	}*/

	void leaveActivity() {
		if (isVisable) {
			if (FirebaseHelper.initialAccountSetupDone) {
				//self.performSegueWithIdentifier("logInFromWelcomeSegue", sender: self)
			}
			else {
				//self.performSegueWithIdentifier("setupAccountSegue", sender: self)
			}
		}
	}

	/*public void createAccountBtnPressed(View view) {
		LogInVC.createAccount = true
		LogInVC.exitSegueStr = "exitToWelcomeSegue"
		performSegueWithIdentifier("signInFromWelcomeSegue", sender: self)
	}*/

	private class UpdateLoginState implements FirebaseHelper.StateChangedListener
	{
		@Override
		public void loginStateChanged()
		{
			stateChanged();
		}

		@Override
		public void userDataChanged()
		{
			stateChanged();
		}

		private void stateChanged()
		{
			if (isVisable) {
				if (FirebaseHelper.isSignedIn()) {
					leaveActivity();
				} else {
					if (FirebaseHelper.initialLoginAttemptDone) {
						showBtn();
					} else {
						showSpinner();
					}
				}
			}
		}
	}

}
