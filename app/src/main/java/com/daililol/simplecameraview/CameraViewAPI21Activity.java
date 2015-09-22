package com.daililol.simplecameraview;


import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.daililol.cameraviewlibrary.CameraViewAPI21;

import java.io.FileOutputStream;


public class CameraViewAPI21Activity extends Activity implements View.OnClickListener, CameraViewAPI21.PhotoCaptureCallback{
	
	private RelativeLayout cameraViewHolder;
	private CameraViewAPI21 cameraView;
	private Button captureButton;
	private Button frontCamButton;
	private Button backCamButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_view_api21_activity);

		cameraViewHolder = (RelativeLayout) findViewById(R.id.cameraViewHolder);
		cameraView = (CameraViewAPI21) findViewById(R.id.cameraView);
		captureButton = (Button) findViewById(R.id.captureButton);
		frontCamButton = (Button) findViewById(R.id.frontCamButton);
		backCamButton = (Button) findViewById(R.id.backCamButton);

        setupSquareCameraView();

        cameraView.setPhotoCaptureCallback(this);
        captureButton.setOnClickListener(this);
        frontCamButton.setOnClickListener(this);
        backCamButton.setOnClickListener(this);
	}
	
	/**
	 * here I will setup the CameraVew as square camera
	 */
	private void setupSquareCameraView(){
		int size = this.getResources().getDisplayMetrics().widthPixels;
		cameraViewHolder.getLayoutParams().width = size;
		cameraViewHolder.getLayoutParams().height = size;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
            case R.id.captureButton:
                dissableButtons();
                cameraView.capture();
                break;
            case R.id.frontCamButton:
				cameraView.useFrontCamera();
                break;
            case R.id.backCamButton:
				cameraView.useBackCamera();
                break;
        }
	}
	


	@Override
	public void onPhotoCaptured(boolean success, Bitmap bitmap) {
		enableButtons();
		writeBitmapToFile(bitmap, Environment.getExternalStorageDirectory().getAbsolutePath() + "/tempCapture.jpg");
	}

	private boolean writeBitmapToFile(Bitmap bitmap, String desFileUrl){
		FileOutputStream outStream = null;

		try{
			outStream = new FileOutputStream(desFileUrl);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
			outStream.close();
			return true;
		}catch(Exception e){
			return false;
		}
	}


	private void enableButtons(){
		captureButton.setEnabled(true);
		frontCamButton.setEnabled(true);
		backCamButton.setEnabled(true);
	}

	private void dissableButtons(){
		captureButton.setEnabled(false);
		frontCamButton.setEnabled(false);
		backCamButton.setEnabled(false);
	}



}
