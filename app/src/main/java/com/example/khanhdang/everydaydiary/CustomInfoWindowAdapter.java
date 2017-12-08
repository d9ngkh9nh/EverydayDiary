/*
  RMIT University Vietnam
  Course: COSC2657 - Android Development
  Semester: 2017C
  Assignment: 2
  Author: Dang Dinh Khanh
  ID: s3618748
  Created date: 05/12/2017
  Acknowledgement:
  -https://firebase.google.com/docs/android/setup
  -https://www.udacity.com
  -https://developers.google.com/maps/documentation/android-api/marker
  -https://www.lynda.com
*/

package com.example.khanhdang.everydaydiary;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;



public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private Activity context;

    public CustomInfoWindowAdapter(Activity context){
        this.context = context;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        View view = context.getLayoutInflater().inflate(R.layout.customwindow, null);

        TextView tvTitle = (TextView) view.findViewById(R.id.tv_title);
        TextView tvSubTitle = (TextView) view.findViewById(R.id.tv_subtitle);

        tvTitle.setText(marker.getTitle());
        tvSubTitle.setText(marker.getSnippet());

        return view;
    }
}
