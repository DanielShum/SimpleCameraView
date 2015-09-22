package com.daililol.simplecameraview;



import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.view.View;

import com.daililol.cameraviewlibrary.CameraView;

import java.io.FileOutputStream;


public class CameraViewActivity extends Activity implements View.OnClickListener, CameraView.PhotoCaptureCallback{
	
	private RelativeLayout cameraViewHolder;
	private CameraView cameraView;
	private Button captureButton;
	private Button frontCamButton;
	private Button backCamButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_view_activity);

		cameraViewHolder = (RelativeLayout) findViewById(R.id.cameraViewHolder);
		cameraView = (CameraView) findViewById(R.id.cameraView);
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
        cameraViewHolder = (RelativeLayout) findViewById(R.id.cameraViewHolder);

        int size = this.getResources().getDisplayMetrics().widthPixels;
		cameraViewHolder.getLayoutParams().width = size;
		cameraViewHolder.getLayoutParams().height = size;
        cameraViewHolder.invalidate();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
            case R.id.captureButton:
                dissableButtons();
                cameraView.capture();
                break;
            case R.id.frontCamButton:
                if (cameraView.getCurrentCamera() == cameraView.getFrontCamera())
                    return;

                if (cameraView.getFrontCamera() != -1)
                    cameraView.setCamera(cameraView.getFrontCamera());
                break;
            case R.id.backCamButton:
                if (cameraView.getCurrentCamera() == cameraView.getBackCamera())
                    return;

                if (cameraView.getBackCamera() != -1)
                    cameraView.setCamera(cameraView.getBackCamera());
                break;
        }
	}
	


    @Override
    public void onPhotoCaptured(Bitmap bitmap) {
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
