
package com.ponysdk.impl.webapplication.page;

import com.ponysdk.core.activity.Activity;
import com.ponysdk.core.activity.ActivityMapper;
import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.application.ApplicationActivity;
import com.ponysdk.impl.webapplication.page.place.LoginPlace;

public class ApplicationActivityMapper implements ActivityMapper {

    private Activity loginActivity;
    private ApplicationActivity applicationActivity;

    @Override
    public Activity getActivity(final Place place) {
        if (place instanceof LoginPlace) return loginActivity;
        else return applicationActivity;
    }

    public void setLoginActivity(final Activity loginActivity) {
        this.loginActivity = loginActivity;
    }

    public void setApplicationActivity(final ApplicationActivity applicationActivity) {
        this.applicationActivity = applicationActivity;
    }

}
