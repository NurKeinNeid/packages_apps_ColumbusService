package org.protonaosp.columbus.sensors;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.lang.reflect.Array;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;

public class TfClassifier {
    private Interpreter mInterpreter;
    private static final String TAG = "columbus/TfClassifier";

    public TfClassifier(AssetManager assetManager, String assetFileName) {
        try {
            AssetFileDescriptor assetFd = assetManager.openFd(assetFileName);
            mInterpreter =
                    new Interpreter(
                            new FileInputStream(assetFd.getFileDescriptor())
                                    .getChannel()
                                    .map(
                                            FileChannel.MapMode.READ_ONLY,
                                            assetFd.getStartOffset(),
                                            assetFd.getDeclaredLength()));
        } catch (Exception e) {
            Log.e(TAG, "Failed to load tflite file: " + e.toString());
        }
    }

    public ArrayList<ArrayList<Float>> predict(ArrayList<Float> input, int size) {
        if (mInterpreter == null) {
            return new ArrayList<>();
        }

        float[][][][] tfliteIn =
                (float[][][][]) Array.newInstance(float.class, 1, input.size(), 1, 1);
        for (int i = 0; i < input.size(); i++) {
            tfliteIn[0][i][0][0] = input.get(i);
        }

        HashMap tfliteOut = new HashMap();
        tfliteOut.put(0, (float[][]) Array.newInstance(float.class, 1, size));

        mInterpreter.runForMultipleInputsOutputs(new Object[] {tfliteIn}, tfliteOut);
        if (tfliteOut == null || tfliteOut.isEmpty()) {
            return new ArrayList<>();
        }

        float[][] tfliteContent = (float[][]) tfliteOut.get(0);
        ArrayList<ArrayList<Float>> output = new ArrayList<>();
        ArrayList<Float> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(Float.valueOf(tfliteContent[0][i]));
        }
        output.add(result);

        return output;
    }
}
