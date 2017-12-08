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

/**
 * Created by khanhdang on 11/28/17.
 */

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class DiaryAdapter extends ArrayAdapter<Diary> {

    public DiaryAdapter(Context context, int resource, List<Diary> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_diary, parent, false);
        }

        ImageView photoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
        TextView diaryTextView = (TextView) convertView.findViewById(R.id.diaryTextView);
        TextView authorTextView = (TextView) convertView.findViewById(R.id.nameTextView);
        TextView dateTextView = (TextView) convertView.findViewById(R.id.dateTextView);
        TextView latitudeTextView = (TextView) convertView.findViewById(R.id.latitudeTextView);
        TextView longitudeTextView = (TextView) convertView.findViewById(R.id.longitudeTextView);

        Diary diary = getItem(position);

        boolean isPhoto = diary.getPhotoUrl() != null;
        if (isPhoto) {
            diaryTextView.setVisibility(View.GONE);
            photoImageView.setVisibility(View.VISIBLE);
            Glide.with(photoImageView.getContext())
                    .load(diary.getPhotoUrl())
                    .into(photoImageView);
        } else {
            diaryTextView.setVisibility(View.VISIBLE);
            photoImageView.setVisibility(View.GONE);
            diaryTextView.setText(diary.getText());
        }
        authorTextView.setText(diary.getName());
        dateTextView.setText(String.valueOf(diary.getDate()));
        latitudeTextView.setText(String.valueOf(diary.getLatitude()));
        longitudeTextView.setText(String.valueOf(diary.getLongitude()));

        return convertView;
    }

}
