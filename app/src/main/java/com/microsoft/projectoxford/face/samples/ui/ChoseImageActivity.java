//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Face-Android
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package com.microsoft.projectoxford.face.samples.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.samples.R;
import com.microsoft.projectoxford.face.samples.helper.ImageHelper;
import com.microsoft.projectoxford.face.samples.helper.LogHelper;
import com.microsoft.projectoxford.face.samples.helper.SampleApp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class ChoseImageActivity extends AppCompatActivity {

    ChoseImageActivity.TargetFaceAdapter mTargetFaceListAdapter;
    ProgressDialog mProgressDialog;
    private UUID mFaceId;
    private Intent data;
    Bitmap mTargetBitmap;
    private Uri mUriPhotoTaken;
    protected static final int REQUEST_SELECT_IMAGE = 2;
    private static final int REQUEST_TAKE_PHOTO = 0;
    private static final int REQUEST_SELECT_IMAGE_IN_ALBUM = 1;


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
            try {
                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        null);
            } catch (Exception e) {
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
        protected void onPostExecute(Face[] result) {
            if (mRequestCode == REQUEST_SELECT_IMAGE) {
                setUiAfterDetectionForSelectImage(result);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "next").setIcon(R.drawable.abc_ic_menu_paste_mtrl_am_alpha)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                Intent activityIntent = new Intent(this, FoundedImagesActivity.class);
                activityIntent.setData(data.getData());
                startActivity(activityIntent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chose_image);
        mTargetFaceListAdapter = new ChoseImageActivity.TargetFaceAdapter();
        data = getIntent();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.progress_dialog_title));
        getDetectedImages();
        initializeFaceList();
        LogHelper.clearFindSimilarFaceLog();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
            case REQUEST_SELECT_IMAGE_IN_ALBUM:
                if (resultCode == RESULT_OK) {
                    if (data != null && data.getData() != null) {
                        this.data = data;
                        getDetectedImages();
                        initializeFaceList();
                    }
                }
                break;
            default:
                break;
        }
    }

    // When the button of "Take a Photo with Camera" is pressed.
    public void takePhoto(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Save the photo taken to a temporary file.
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            try {
                File file = File.createTempFile("IMG_", ".jpg", storageDir);
                mUriPhotoTaken = Uri.fromFile(file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            } catch (IOException e) {
                setInfo(e.getMessage());
            }
        }
    }

    // When the button of "Select a Photo in Album" is pressed.
    public void selectImageInAlbum(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_SELECT_IMAGE_IN_ALBUM);
        }
    }

    // Set the information panel on screen.
    private void setInfo(String info) {
        TextView textView = (TextView) findViewById(R.id.info);
        textView.setText(info);
    }

    private void initializeFaceList() {
        ListView listView = (ListView) findViewById(R.id.list_faces);

        // When a detected face in the GridView is clicked, the face is selected to verify.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChoseImageActivity.TargetFaceAdapter faceListAdapter = mTargetFaceListAdapter;

                if (!faceListAdapter.faces.get(position).faceId.equals(mFaceId)) {
                    mFaceId = faceListAdapter.faces.get(position).faceId;

                    ImageView imageView = (ImageView) findViewById(R.id.image);
                    imageView.setImageBitmap(faceListAdapter.faceThumbnails.get(position));

                }

                // Show the list of detected face thumbnails.
                ListView listView = (ListView) findViewById(R.id.list_faces);
                listView.setAdapter(faceListAdapter);
            }
        });
    }


    private class TargetFaceAdapter extends BaseAdapter {
        // The detected faces.
        List<Face> faces;

        // The thumbnails of detected faces.
        List<Bitmap> faceThumbnails;

        Map<UUID, Bitmap> faceIdThumbnailMap;

        TargetFaceAdapter() {
            faces = new ArrayList<>();
            faceThumbnails = new ArrayList<>();
            faceIdThumbnailMap = new HashMap<>();
        }

        public void addFaces(Face[] detectionResult, Bitmap bitmap) {
            if (detectionResult != null) {
                List<Face> detectedFaces = Arrays.asList(detectionResult);
                for (Face face : detectedFaces) {
                    faces.add(face);
                    try {
                        Bitmap faceThumbnail = ImageHelper.generateFaceThumbnail(
                                bitmap, face.faceRectangle);
                        faceThumbnails.add(faceThumbnail);
                        faceIdThumbnailMap.put(face.faceId, faceThumbnail);
                    } catch (IOException e) {
                        // Show the exception when generating face thumbnail fails.
//                        setInfo(e.getMessage());
                    }
                }
            }
        }

        @Override
        public int getCount() {
            return faces.size();
        }

        @Override
        public Object getItem(int position) {
            return faces.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater
                        = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_face, parent, false);
            }
            convertView.setId(position);

            Bitmap thumbnailToShow = faceThumbnails.get(position);
            if (faces.get(position).faceId.equals(mFaceId)) {
                thumbnailToShow = ImageHelper.highlightSelectedFaceThumbnail(thumbnailToShow);
            }

            // Show the face thumbnail.
            ((ImageView) convertView.findViewById(R.id.image_face)).setImageBitmap(thumbnailToShow);

            return convertView;
        }
    }

    void setUiAfterDetectionForSelectImage(Face[] result) {

        // Show the detailed list of detected faces.
        mTargetFaceListAdapter = new ChoseImageActivity.TargetFaceAdapter();
        mTargetFaceListAdapter.addFaces(result, mTargetBitmap);

        // Show the list of detected face thumbnails.
        ListView listView = (ListView) findViewById(R.id.list_faces);
        listView.setAdapter(mTargetFaceListAdapter);

        // Set the default face ID to the ID of first face, if one or more faces are detected.
        if (mTargetFaceListAdapter.faces.size() != 0) {
            mFaceId = mTargetFaceListAdapter.faces.get(0).faceId;
            // Show the thumbnail of the default face.
            ImageView imageView = (ImageView) findViewById(R.id.image);
            imageView.setImageBitmap(mTargetFaceListAdapter.faceThumbnails.get(0));
        }


        mTargetBitmap = null;
        mProgressDialog.dismiss();

        // Set the status bar.
    }


    private void getDetectedImages() {

        mTargetBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                data.getData(), getContentResolver());
        if (mTargetBitmap != null) {

            // Put the image into an input stream for detection.
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            mTargetBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
            ByteArrayInputStream inputStream
                    = new ByteArrayInputStream(output.toByteArray());


            // Start a background task to detect faces in the image.
            new DetectionTask(REQUEST_SELECT_IMAGE).execute(inputStream);
        }
    }
}
