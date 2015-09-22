/**
 * Author: Martin Shum
 * This is the camera view which used below api 21.
 */

package com.daililol.cameraviewlibrary;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewParent;
import android.widget.RelativeLayout;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, PictureCallback{
	
	private ViewParent parentView;
	private Camera camera;
	private List<Size> supportedPreviewSizes;
	private Size optimalPreviewSize;


	private PhotoCaptureCallback photoCaptureCallback;
	private OnCameraChangedListener onCameraChangedListener;
	
	private Point viewSize;
	private int viewHeight = 0;
	private int frontCamera = -1;
	private int backCamera = -1;
	private static int currentCamera = - 1;
	
	
	public static interface OnCameraChangedListener{
		public void onCameraChanged(int cameraIndex);
	}
	public static interface PhotoCaptureCallback{
		public void onPhotoCaptured(Bitmap bitmap);
	}
	
	public CameraView(Context context) throws IllegalAccessException{
		this(context, null);
	}
	
	public CameraView(Context context, AttributeSet attrs) throws IllegalAccessException {
		super(context, attrs);

		this.getKeepScreenOn();
		this.getHolder().addCallback(this);
		
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
			this.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		assignCamera();
		currentCamera = backCamera;
		
		try{
			if (currentCamera == -1){
				camera = Camera.open();
			}else{
				camera = Camera.open(currentCamera);
			}
			
		}catch(Exception e){
			
		}
		
		calculateOptimalPreviewSize();
		
	}


	
	public void setPhotoCaptureCallback(PhotoCaptureCallback photoCaptureCallback){
		this.photoCaptureCallback = photoCaptureCallback;
	}
	
	public void setOnCameraChangedListener(OnCameraChangedListener onCameraChangedListener){
		this.onCameraChangedListener = onCameraChangedListener;
	}
	
	public int getCurrentCamera(){
		return currentCamera;
	}
	
	public int getBackCamera(){
		return backCamera;
	}
	
	public int getFrontCamera(){
		return frontCamera;
	}
	
	public void setCamera(int cameraIndex){
		if (cameraIndex == -1 || 
				(cameraIndex != backCamera &&
				cameraIndex != frontCamera)) return;
		currentCamera = cameraIndex;
		releaseCamera();
		reconnectCamera();
		
		if (onCameraChangedListener != null) onCameraChangedListener.onCameraChanged(currentCamera);
		
	}
	
	public void swapCamera(){
		if (currentCamera == backCamera){
			setCamera(frontCamera);
		}else{
			setCamera(backCamera);
		}
	}
	
	
	
	public void startCameraPreview(){
		
		if (camera == null) return;
		
		Camera.Parameters param = camera.getParameters();
        //param.setPreviewFrameRate(20);
		List<Integer> previewFrameRates = param.getSupportedPreviewFrameRates();
		if (previewFrameRates != null)
			param.setPreviewFrameRate(previewFrameRates.get(previewFrameRates.size() - 1));

        if (param.getSupportedFocusModes().size() != 0){
        	for (int i = 0; i < param.getSupportedFocusModes().size(); i++){
        		if (param.getSupportedFocusModes().get(i).equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
        			param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        		}
        	}
        }
        
        param.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height);

        try {
        	camera.setDisplayOrientation(90);
            camera.setParameters(param);
			camera.setPreviewDisplay(getHolder());
			camera.startPreview();
		} catch (Exception e) {
			Log.v("error", e.toString());
		}
        
	}
	
	public void capture(){

		try{
			camera.takePicture(null, null, this);
		}catch(Exception e){
			if (photoCaptureCallback != null) photoCaptureCallback.onPhotoCaptured(null);
		}
		
	}
	
	public void reconnectCamera(){

		try {
			if (currentCamera == -1){
				camera = Camera.open();
			}else{
				camera = Camera.open(currentCamera);
			}
			
			calculateOptimalPreviewSize();
			startCameraPreview();
			this.setKeepScreenOn(false);
		} catch (Exception e) {
			Log.v("exception", e.toString());
		}
	}
	
	public void releaseCamera(){
		if (camera == null) return;
		try{
			camera.stopPreview();
			camera.release();
		}catch(Exception e){

		}
		
		this.setKeepScreenOn(false);
	}
	
	private void calculateOptimalPreviewSize(){
		if (camera != null) {
			supportedPreviewSizes = camera.getParameters().getSupportedPreviewSizes();
			optimalPreviewSize = getOptimalPreviewSize(supportedPreviewSizes, 600, 600);
		}
	}
	
	private void assignCamera(){
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
	    for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
	        Camera.getCameraInfo(i, cameraInfo);
	        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	        	frontCamera = i;
	        }
	        
	        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
	        	backCamera = i;
	        }
	    }
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		processPhoto(data);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
		try{
			startCameraPreview();
		}catch(Exception e){
			
		}
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		
		releaseCamera();
	}
	
	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		parentView = getParent();
		if (parentView == null || !(parentView instanceof RelativeLayout)){
			throw new RuntimeException("The CameraView's parent view must be a RelativeLayout.");
		}

		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)getLayoutParams();
		if (params == null) params = new RelativeLayout.LayoutParams
				(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);

		viewSize = new Point(((RelativeLayout) parentView).getWidth(), ((RelativeLayout) parentView).getHeight());
		float sizePercentage = (float)viewSize.x / (float)optimalPreviewSize.height;
		viewHeight = (int)((float)optimalPreviewSize.width * sizePercentage);

		this.setMeasuredDimension(viewSize.x, viewHeight);
    }

	private void processPhoto(byte[] data){

		if (data == null){
			return;
		}

		AsyncTask<Object, Void, Object> task = new AsyncTask<Object, Void, Object>(){

			@Override
			protected Object doInBackground(Object... params) {

				byte[] data = (byte[])params[0];

				Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
				if (bitmap == null) return null;

				float ratio = (float)bitmap.getHeight() / (float)viewSize.x;
				int heightDifference = bitmap.getWidth() - (int)((float)viewSize.y * ratio);

				if (heightDifference > 2) {
					float offset = (float)heightDifference / 2.0f;
					bitmap = Bitmap.createBitmap(bitmap, (int)offset, 0, (int)((float)viewSize.y * ratio), bitmap.getHeight());
					bitmap = createRotatedBitmap(bitmap, (getCurrentCamera() == getFrontCamera()) ? -90 : 90);
				}


				return bitmap;

			}

			@Override
			protected void onPostExecute(Object bitmap){
				super.onPostExecute(bitmap);
				camera.startPreview();
				if (photoCaptureCallback != null) photoCaptureCallback.onPhotoCaptured((Bitmap)bitmap);
			}

		};

		if (Build.VERSION.SDK_INT >= 11){
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data);
		}else{
			task.execute(data);
		}
	}


	private Bitmap createRotatedBitmap(Bitmap bitmap, int rotation){

		if (bitmap == null) return null;

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		Matrix mtx = new Matrix();
		mtx.preRotate(rotation);
		bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);

		return bitmap;
	}


	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }



}
