package com.reactlibrary;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.Utils;
import org.opencv.imgproc.Imgproc;
import android.util.Base64;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Core;

import java.io.File;

import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;

public class RNOpenCvLibraryModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public RNOpenCvLibraryModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNOpenCvLibrary";
    }

    @ReactMethod
    public void checkForBlurryImage(String imageAsBase64, Callback errorCallback, Callback successCallback) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = true;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            byte[] decodedString = Base64.decode(imageAsBase64, Base64.DEFAULT);
            Bitmap image = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);


//      Bitmap image = decodeSampledBitmapFromFile(imageurl, 2000, 2000);
            int l = CvType.CV_8UC1; //8-bit grey scale image
            Mat matImage = new Mat();
            Utils.bitmapToMat(image, matImage);
            Mat matImageGrey = new Mat();
            Imgproc.cvtColor(matImage, matImageGrey, Imgproc.COLOR_BGR2GRAY);

            Bitmap destImage;
            destImage = Bitmap.createBitmap(image);
            Mat dst2 = new Mat();
            Utils.bitmapToMat(destImage, dst2);
            Mat laplacianImage = new Mat();
            dst2.convertTo(laplacianImage, l);
            Imgproc.Laplacian(matImageGrey, laplacianImage, CvType.CV_8U);
            Mat laplacianImage8bit = new Mat();
            laplacianImage.convertTo(laplacianImage8bit, l);

            Bitmap bmp = Bitmap.createBitmap(laplacianImage8bit.cols(), laplacianImage8bit.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(laplacianImage8bit, bmp);
            int[] pixels = new int[bmp.getHeight() * bmp.getWidth()];
            bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
            int maxLap = -16777216; // 16m
            for (int pixel : pixels) {
                if (pixel > maxLap)
                    maxLap = pixel;
            }

//            int soglia = -6118750;
            int soglia = -8118750;
            if (maxLap <= soglia) {
                System.out.println("is blur image");
            }

            successCallback.invoke(maxLap <= soglia);
        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void removeGlare(String uri, Callback errorCallback, Callback successCallback) {
        try {
            // Read the image from file using OpenCV
            Mat imageMat = Imgcodecs.imread(uri, Imgcodecs.IMREAD_COLOR);

            // Convert to grayscale - reduce noise, remove high-frequency details
            // A grayscale image reduces computational complexity
            Mat grayScaleGaussianBlur = new Mat();
            Imgproc.cvtColor(imageMat, grayScaleGaussianBlur, Imgproc.COLOR_RGBA2GRAY);
            Imgproc.GaussianBlur(grayScaleGaussianBlur, grayScaleGaussianBlur, new Size(5, 5), 0);

            // Calculate min and max values of the blurred image
            Core.MinMaxLocResult minMaxLocResultBlur = Core.minMaxLoc(grayScaleGaussianBlur);
            double maxval = minMaxLocResultBlur.maxVal;
            double minval = minMaxLocResultBlur.minVal;

            if (maxval >= 253.0 && minval > 0.0 && minval < 20.0) {
                // Threshold, convert to binary, and invert the image
                Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_RGBA2GRAY);
                Imgproc.threshold(imageMat, imageMat, 128, 255, Imgproc.THRESH_BINARY);
                Core.bitwise_not(imageMat, imageMat);
            }

            // Save the processed image
            String processedImageUri = uri.replace(".jpg", "_processed.jpg");
            Imgcodecs.imwrite(processedImageUri, imageMat);

            // Release Mat resources
            imageMat.release();
            grayScaleGaussianBlur.release();

            // Invoke success callback with processed image URI
            successCallback.invoke(processedImageUri);
            System.out.println("glare removed");
        } catch (Exception e) {
            // Invoke error callback if an exception occurs
            errorCallback.invoke(e.getMessage());
        }
    }
}