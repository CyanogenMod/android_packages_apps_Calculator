package com.android2.calculator3;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * @author Will Harmon
 **/
public class About extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        try {
            String aboutDesc = String.format(getString(R.string.about), getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
            TextView about = (TextView) findViewById(R.id.about);
            about.setText(aboutDesc);
            about.setMovementMethod(LinkMovementMethod.getInstance());
        }
        catch(NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
