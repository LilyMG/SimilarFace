package com.microsoft.projectoxford.face.samples.ui;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.samples.R;
import com.microsoft.projectoxford.face.samples.helper.SampleApp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by LilitTevosyan on 10/27/16.
 */

public class FoundedImagesActivity extends AppCompatActivity {

    Bitmap mBitmap;
    protected static final int REQUEST_ADD_FACE = 0;
    protected static final int REQUEST_SELECT_IMAGE = 1;
    private ArrayList<Face> detectedFaces;
    ProgressDialog mProgressDialog;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_founded);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.progress_dialog_title));
        detectedFaces = new ArrayList<>();
        initAndFindImages();
    }

    private void initAndFindImages(){
        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.face_a);
        if (mBitmap != null) {

            // Put the image into an input stream for detection.
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
            ByteArrayInputStream inputStream
                    = new ByteArrayInputStream(output.toByteArray());
            // Start a background task to detect faces in the image.
            new FoundedImagesActivity.DetectionTask(REQUEST_ADD_FACE).execute(inputStream);
        }

    }



    // Background task for face detection
    class DetectionTask extends AsyncTask<InputStream, String, Face[]> {
        private boolean mSucceed = true;
        int mRequestCode;
        DetectionTask(int requestCode) {
            mRequestCode = requestCode;
        }

        @Override
        protected Face[] doInBackground(InputStream... params) {
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try{
                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        null);
            }  catch (Exception e) {
                mSucceed = false;
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
        }

        @Override
        protected void onPostExecute(Face[] result) {
            detectedFaces.addAll(Arrays.asList(result));
            System.out.println(detectedFaces);
            mProgressDialog.dismiss();
        }
    }

}
