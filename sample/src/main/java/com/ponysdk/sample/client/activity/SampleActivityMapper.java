
package com.ponysdk.sample.client.activity;

import org.springframework.beans.factory.annotation.Autowired;

import com.ponysdk.core.activity.Activity;
import com.ponysdk.core.activity.ActivityMapper;
import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.application.ApplicationActivity;
import com.ponysdk.sample.client.LoginActivity;
import com.ponysdk.sample.client.place.LoginPlace;

public class SampleActivityMapper implements ActivityMapper {

    @Autowired
    private LoginActivity loginActivity;

    @Autowired
    private ApplicationActivity applicationActivity;

    @Override
    public Activity getActivity(final Place place) {
        if (place instanceof LoginPlace) return loginActivity;
        else return applicationActivity;
    }

}
