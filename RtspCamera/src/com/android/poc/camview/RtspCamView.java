package com.android.poc.camview;

import java.io.IOException;
import java.util.List;

import de.kp.net.rtp.recorder.RtspVideoRecorder;
import de.kp.net.rtsp.RtspConstants;
import de.kp.net.rtsp.server.RtspServer;
import android.annotation.SuppressLint;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class RtspCamView {
	private String TAG = "RTSPNativeCamera";
	
	private SurfaceView mCameraPreview;
	private SurfaceHolder previewHolder;
	private RtspVideoRecorder outgoingPlayer;
	private Camera camera = null;
	
	private boolean inPreview = false;
	private boolean cameraConfigured = false;
	private int mPreviewWidth = Integer.valueOf(RtspConstants.WIDTH);
	private int mPreviewHeight = Integer.valueOf(RtspConstants.HEIGHT);
	
	private RtspServer streamer = null;
	
	public RtspCamView(SurfaceView sfView)
	{
		mCameraPreview = sfView;
	}
	
	public void initStreamCast()
	{
		mCameraPreview.setVisibility(View.VISIBLE);
		previewHolder = mCameraPreview.getHolder();
		previewHolder.addCallback(surfaceCallback);
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

//		outgoingPlayer = new RtspVideoRecorder("h263-2000");
		outgoingPlayer = new RtspVideoRecorder("h264");
		outgoingPlayer.open();
	}
	
	public void setVisibility(int visibility)
	{
		mCameraPreview.setVisibility(visibility);
	}
	
	@SuppressLint("NewApi")
	public void resumeView()
	{
		// starts the RTSP Server

				try {

					// initialize video encoder to be used
					// for SDP file generation
					if (streamer == null) {
						
					RtspConstants.VideoEncoder rtspVideoEncoder = (MediaConstants.H264_CODEC == true) ? RtspConstants.VideoEncoder.H264_ENCODER
							: RtspConstants.VideoEncoder.H263_ENCODER;

					
						streamer = new RtspServer(RtspConstants.SERVER_PORT, rtspVideoEncoder);
						new Thread(streamer).start();
					}

					Log.d(TAG, "RtspServer started");

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
		/*
		 * Camera initialization
		 */
		if(camera == null)
		{
			camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
			camera.setDisplayOrientation(90);
		}
	}
	
	public void pauseView()
	{
		// stop RTSP server
				if (streamer != null)
					streamer.stop();
				streamer = null;
				
				camera.release();
				//camera = null;
	}
	
	
	/*
	 * SurfaceHolder callback triple
	 */
	SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
		/*
		 * Created state: - Open camera - initial call to startPreview() - hook
		 * PreviewCallback() on it, which notifies waiting thread with new
		 * preview data - start thread
		 * 
		 * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.
		 * SurfaceHolder )
		 */
		public void surfaceCreated(SurfaceHolder holder) {
			Log.d(TAG, "surfaceCreated");

		}

		/*
		 * Changed state: - initiate camera preview size, set
		 * camera.setPreviewDisplay(holder) - subsequent call to startPreview()
		 * 
		 * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.
		 * SurfaceHolder , int, int, int)
		 */
		public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
			Log.d(TAG, "surfaceChanged");
			initializePreview(w, h);
			startPreview();
		}

		/*
		 * Destroy State: Take care on release of camera
		 * 
		 * @see
		 * android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.
		 * SurfaceHolder)
		 */
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.d(TAG, "surfaceDestroyed");

			if (inPreview) {
				camera.stopPreview();
			}
			camera.setPreviewCallback(null);
			camera.release();
			camera = null;
			
			// stop captureThread
			outgoingPlayer.stop();


			inPreview = false;
			cameraConfigured = false;

		}
	};
	
	/**
	 * This method checks availability of camera and preview
	 * 
	 * @param width
	 * @param height
	 */
	private void initializePreview(int width, int height) {
		Log.d(TAG, "initializePreview");

		if (camera != null && previewHolder.getSurface() != null) {
			try {
				// provide SurfaceView for camera preview
				camera.setPreviewDisplay(previewHolder);

			} catch (Throwable t) {
				Log.e(TAG, "Exception in setPreviewDisplay()", t);
			}

			if (!cameraConfigured) {

				Camera.Parameters parameters = camera.getParameters();
				parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
				
				List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();
		        Camera.Size procSize_ = supportedSizes.get( supportedSizes.size()/2);
		       // parameters.setPreviewSize(procSize_.width, procSize_.height);
		        //RtspConstants.WIDTH = String.valueOf(procSize_.width);
		        //RtspConstants.HEIGHT = String.valueOf(procSize_.height);
				camera.setParameters(parameters);
				cameraConfigured = true;

			}
		}
	}

	private void startPreview() {
		Log.d(TAG, "startPreview");

		if (cameraConfigured && camera != null) {

			// activate onPreviewFrame()
			// camera.setPreviewCallback(cameraPreviewCallback);
			camera.setPreviewCallback(outgoingPlayer);
			
			// start captureThread
			outgoingPlayer.start();

			camera.startPreview();
			inPreview = true;

		}
	}


	
	public boolean isReady() {
		return this.inPreview;
	}
}
