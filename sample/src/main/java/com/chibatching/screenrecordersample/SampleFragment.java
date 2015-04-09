package com.chibatching.screenrecordersample;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.chibatching.screenrecorder.ScreenRecorder;

import org.jetbrains.annotations.NotNull;

public class SampleFragment extends Fragment {

    private static final int[] COLORS = {Color.BLUE, Color.GREEN, Color.RED};
    private int mColorIndex = 0;

    public static SampleFragment newInstance() {
        return new SampleFragment();
    }

    public SampleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View contentView = inflater.inflate(R.layout.fragment_sample, container, false);
        final RelativeLayout layout = (RelativeLayout) contentView.findViewById(R.id.container);

        Button recButton = (Button) contentView.findViewById(R.id.btn_rec);
        recButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NotNull View view) {
                ScreenRecorder sr =
                        new ScreenRecorder(
                                getActivity(),
                                getActivity().getWindow().getDecorView(),
                                5000,
                                30,
                                0.5);
                sr.start();
            }
        });

        Button changeButton = (Button) contentView.findViewById(R.id.btn_change_color);
        changeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layout.setBackgroundColor(COLORS[mColorIndex++%COLORS.length]);
            }
        });

        return contentView;
    }

}
