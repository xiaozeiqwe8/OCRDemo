package io.github.karl.ocrdemo;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class OcrResult {
    @SerializedName("errId")
    int errId;
    @SerializedName("errMsg")
    String errMsg;
    @SerializedName("result")
    List<Result> result = new ArrayList<>();

    public static class Result {
        @SerializedName("boxes")
        List<List<Integer>> boxes = new ArrayList<>();
        @SerializedName("text")
        String text;
        @SerializedName("score")
        float score;
    }
}
