package com.chibatching.viewrecordersample;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.chibatching.viewrecorder.ViewRecorder;
import com.chibatching.viewrecorder.ViewRecorderBuilder;

import org.jetbrains.annotations.NotNull;

public class SampleFragment extends Fragment {

    private static final int[] COLORS = {Color.BLUE, Color.GREEN, Color.RED};
    private int mColorIndex = 0;

    private ViewRecorder recorder;

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

        final Button startButton = (Button) contentView.findViewById(R.id.btn_start_rec);
        final Button stopButton = (Button) contentView.findViewById(R.id.btn_stop_rec);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NotNull View view) {
                recorder = new ViewRecorderBuilder(getActivity(), getActivity().getWindow().getDecorView())
//                        .setDuration(4000)
                        .setLoopCount(0)
                        .setFrameRate(20)
                        .setScale(0.3)
                        .setOnRecordFinishListener(new ViewRecorder.OnRecordFinishListener() {
                            @Override
                            public void onRecordFinish() {
                                stopButton.setEnabled(false);
                                startButton.setEnabled(true);
                            }
                        })
                        .create();

                recorder.start();
                stopButton.setEnabled(true);
                startButton.setEnabled(false);
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NotNull View view) {
                recorder.stop();
                stopButton.setEnabled(false);
                startButton.setEnabled(true);
            }
        });

        Button changeButton = (Button) contentView.findViewById(R.id.btn_change_color);
        changeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NotNull View view) {
                layout.setBackgroundColor(COLORS[mColorIndex++%COLORS.length]);
            }
        });

        return contentView;
    }

}
