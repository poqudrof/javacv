/*
 * Copyright (C) 2014 Jeremy Laviole, Samuel Audet
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE.txt file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bytedeco.javacv;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.swing.JOptionPane;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core;

import org.openni.*;
import org.openni.Device;
import org.openni.DeviceInfo;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/**
 *
 * @author Jeremy Laviole
 */
public class OpenNI2FrameGrabber extends FrameGrabber {

    public static String[] getDeviceDescriptions() throws FrameGrabber.Exception {
        tryLoad();

        java.util.List<DeviceInfo> devicesInfo = OpenNI.enumerateDevices();
        String[] desc = new String[devicesInfo.size()];
        for (int i = 0; i < devicesInfo.size(); i++) {
            String name = devicesInfo.get(i).getName();
            String v = devicesInfo.get(i).getVendor();
            String uri = devicesInfo.get(i).getUri();
            desc[i] = name + " " + v + " " + uri + "\n";
        }
        return desc;
    }

    public static int DEFAULT_DEPTH_WIDTH = 640;
    public static int DEFAULT_DEPTH_HEIGHT = 480;
    public static int DEFAULT_COLOR_WIDTH = 640;
    public static int DEFAULT_COLOR_HEIGHT = 480;
    public static int DEFAULT_COLOR_FRAMERATE = 30;
    public static int DEFAULT_DEPTH_FRAMERATE = 30;

    private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    private int depthImageWidth = DEFAULT_DEPTH_WIDTH;
    private int depthImageHeight = DEFAULT_DEPTH_HEIGHT;
    private int depthFrameRate = 30;

    private int IRImageWidth = DEFAULT_DEPTH_WIDTH;
    private int IRImageHeight = DEFAULT_DEPTH_HEIGHT;
    private int IRFrameRate = 30;

    Device device;

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    public static OpenNI2FrameGrabber createDefault(int deviceNumber) throws FrameGrabber.Exception {
        return new OpenNI2FrameGrabber(deviceNumber);
    }

    public static OpenNI2FrameGrabber createDefault(File deviceFile) throws Exception {
        throw new Exception(OpenNI2FrameGrabber.class + " does not support File devices.");
    }

    public static OpenNI2FrameGrabber createDefault(String devicePath) throws Exception {
        throw new Exception(OpenNI2FrameGrabber.class + " does not support path.");
    }

    private static FrameGrabber.Exception loadingException = null;

    public static boolean initialized = false;

    public static void tryLoad() throws FrameGrabber.Exception {
        if (loadingException != null) {
            loadingException.printStackTrace();
            throw loadingException;
        } else {
            if (initialized) {

                System.out.println("OPENNI: alread initialized.");
                return;
            }
            try {
                System.out.println("OPENNI: initialize.");
                OpenNI.initialize();
                initialized = true;
            } catch (Throwable t) {
                throw loadingException = new FrameGrabber.Exception("Failed to load " + OpenNI2FrameGrabber.class, t);
            }
        }
    }

    private int deviceNumber = 0;
    private boolean depth = false; // default to "video"
    private boolean colorEnabled = false;
    private boolean depthEnabled = false;
    private boolean IREnabled = false;
    private FrameConverter converter = new OpenCVFrameConverter.ToIplImage();

    public OpenNI2FrameGrabber(int deviceNumber) {
        this.deviceNumber = deviceNumber;
    }

    public static void main(String[] args) {
//        context context = new context();
//        System.out.println("Devices found: " + context.get_device_count());
//        device device = context.get_device(0);
//        System.out.println("Using device 0, an " + device.get_name());
//        System.out.println(" Serial number: " + device.get_serial());
    }

    public void enableColorStream() {
        if (!colorEnabled) {
            if (imageWidth == 0) {
                imageWidth = DEFAULT_COLOR_WIDTH;
            }
            if (imageHeight == 0) {
                imageHeight = DEFAULT_COLOR_HEIGHT;
            }
            if (frameRate == 0) {
                frameRate = DEFAULT_COLOR_FRAMERATE;
            }
            colorEnabled = true;
        }
    }

    public void disableColorStream() {
        if (colorEnabled) {
            colorEnabled = false;
        }
    }

    public void enableDepthStream() {
        if (!depthEnabled) {
            depthEnabled = true;
        }
    }

    public void disableDepthStream() {
        if (depthEnabled) {
            depthEnabled = false;
        }
    }

    public void enableIRStream() {
        if (!IREnabled) {
            if (imageWidth == 0) {
                imageWidth = DEFAULT_DEPTH_WIDTH;
            }
            if (imageHeight == 0) {
                imageHeight = DEFAULT_DEPTH_HEIGHT;
            }
            if (frameRate == 0) {
                frameRate = DEFAULT_DEPTH_FRAMERATE;
            }
            IREnabled = true;
        }
    }

    public void disableIRStream() {
        if (IREnabled) {
            IREnabled = false;
        }
    }

    public void release() throws FrameGrabber.Exception {
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }

    /**
     * Warning can lead to unsafe situations.
     *
     * @return
     */
    public Device getOpenNIDevice() {
        return this.device;
    }

    @Override
    public double getFrameRate() {  // TODO: check this. 
        return super.getFrameRate();
    }

    private boolean startedOnce = false;
    private boolean behaveAsColorFrameGrabber = false;

    static String globalDeviceURI = null;
    static Device globalDevice = null;

    public Device loadDevice() throws FrameGrabber.Exception {

        System.out.println("OPENNI2: loadDevice");
        tryLoad();

        // TODO: check if device is already open...
        if (OpenNI.enumerateDevices().size() <= deviceNumber) {
            throw new Exception("FATAL error: OpenNI2 camera: " + deviceNumber + " not connected/found");
        }

//        String uri = OpenNI.enumerateDevices().get(this.deviceNumber).getUri();
//        uri = devicesInfo.get(this.systemNumber).getUri();
//        System.out.println("OpenNI URI: " + uri.toString());
//        device = Device.open(uri);
//        System.out.println("OPENNI2: open device " + uri);
//        device = Device.open(uri);
        java.util.List<DeviceInfo> devicesInfo = OpenNI.enumerateDevices();
        if (devicesInfo.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No device is connected", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        String uri;
        uri = devicesInfo.get(this.deviceNumber).getUri();
//        System.out.println("OpenNI URI: " + uri.toString());

// TEST
        if (globalDevice != null) {
            return globalDevice;
        }

        Device device = Device.open(uri);
        globalDevice = device;
        globalDeviceURI = uri;

        System.out.println("OPENNI2: device opened. ");
        return device;
    }

    @Override
    public void start() throws FrameGrabber.Exception {

        this.device = loadDevice();

        // Choose the camera to enable by format.
        if (format != null) {
            if ("".equals(format)) {
                this.enableColorStream();
            }
            if ("rgb".equals(format)) {
                this.enableColorStream();
            }
            if ("ir".equals(format)) {
            }
            if ("depth".equals(format)) {
                this.enableDepthStream();
            }
        } else {
            this.enableColorStream();
        }

        try {
            if (colorEnabled) {
                System.out.println("OPENNI2: Color initStream.");
                colorStream = initStream(
                        org.openni.PixelFormat.RGB888,
                        SensorType.COLOR,
                        new FrameListener(this), this);
                System.out.println("OPENNI2: Color init OK, starting...");
                colorStream.setMirroringEnabled(false);
                colorStream.start();

                System.out.println("OPENNI2: Color started !");
            }
            if (IREnabled) {
                System.out.println("NI: start IR stream.");
                irStream = initStream(
                        org.openni.PixelFormat.RGB888,
                        SensorType.IR,
                        new FrameListener(this), this);
                irStream.setMirroringEnabled(false);
                irStream.start();
            }
            if (depthEnabled) {
                // Not supported
            }
        } catch (java.lang.Exception e) {
            System.out.println("Error starting camera OpenNI: " + e);
            e.printStackTrace();
        }

        defaultImage = IplImage.create(DEFAULT_COLOR_WIDTH, DEFAULT_COLOR_HEIGHT, IPL_DEPTH_8U, 1);

    }

    IplImage defaultImage = null;

    private void updateCurrentImage(IplImage newImage) {
        this.rawVideoImage = newImage.clone();
    }
    protected opencv_core.IplImage rawVideoImage = null, grayImage = null;

//        private Pointer rawDepthImageData = new Pointer((Pointer) null),
//                rawVideoImageData = new Pointer((Pointer) null),
//                rawIRImageData = new Pointer((Pointer) null);
    
    protected VideoStream colorStream, irStream, depthStream;

    private VideoStream initStream(org.openni.PixelFormat format2,
            SensorType type,
            OpenNI2FrameGrabber.FrameListener listener,
            OpenNI2FrameGrabber grabber) {

        VideoStream videoStream = VideoStream.create(device, type);
        videoStream.setMirroringEnabled(false);
        VideoMode vm = new VideoMode(grabber.getImageWidth(),
                grabber.getImageHeight(),
                (int) grabber.getFrameRate(), format2.toNative());

        videoStream.setVideoMode(vm);

        videoStream.addNewFrameListener(listener);
        return videoStream;
    }

    /**
     *
     * @throws Exception
     */
    @Override
    public void stop() throws FrameGrabber.Exception {

//        if (colorStream != null) {
//            colorStream.stop();
//        }
//        if (irStream != null) {
//            irStream.stop();
//        }
//        if (depthStream != null) {
//            depthStream.stop();
//        }
//
//        device.close();
//        colorEnabled = false;
//        IREnabled = false;
//        depthEnabled = false;
        frameNumber = 0;
    }

    /**
     *
     * @return does not really, grab but returns the image.
     * @throws org.bytedeco.javacv.FrameGrabber.Exception
     */
    public Frame grab() throws Exception {

        // For Framegrabber
        if (colorEnabled) {
            IplImage image = rawVideoImage;

            if (image == null) {
                System.out.println("OPENNI2: No image...");
                return converter.convert(defaultImage);
            }
            if (this.imageMode == ImageMode.GRAY) {
                if (grayImage == null) {
                    int deviceWidth = this.getImageWidth();
                    int deviceHeight = this.getImageHeight();
//                grayImage = IplImage.create(deviceWidth, deviceHeight, IPL_DEPTH_8U, 3);
                    grayImage = IplImage.create(deviceWidth, deviceHeight, IPL_DEPTH_8U, 1);
                }
                cvCvtColor(image, grayImage, CV_BGR2GRAY);
                return converter.convert(grayImage);
            }

        };
        // For Framegrabber
        if (IREnabled) {
            IplImage image = rawVideoImage;

            if (image == null) {
                System.out.println("OPENNI2: No image...");
                return converter.convert(defaultImage);
            }

            if (grayImage == null) {
                int deviceWidth = this.getImageWidth();
                int deviceHeight = this.getImageHeight();
//                grayImage = IplImage.create(deviceWidth, deviceHeight, IPL_DEPTH_8U, 3);
                grayImage = IplImage.create(deviceWidth, deviceHeight, IPL_DEPTH_8U, 1);
            }
            cvCvtColor(image, grayImage, CV_BGR2GRAY);
            return converter.convert(grayImage);
        }
        return converter.convert(defaultImage);
    }

    class FrameListener implements VideoStream.NewFrameListener {

        VideoFrameRef lastFrame;
        OpenNI2FrameGrabber grabber;

        public FrameListener(OpenNI2FrameGrabber grabber) {
            this.grabber = grabber;
        }

        private Pointer rawDepthImageData = new Pointer((Pointer) null),
                rawVideoImageData = new Pointer((Pointer) null),
                rawIRImageData = new Pointer((Pointer) null);
        protected opencv_core.IplImage rawVideoImage = null;
        protected opencv_core.IplImage rawVideoImageGray = null;

        @Override
        public synchronized void onFrameReady(VideoStream stream) {
//            if (this.camera == colorCamera) {
//                System.out.println("Getting a color frame.");
//            } else {
//                System.out.println("Getting another frame.");
//            }

            if (lastFrame != null) {
                lastFrame.release();
                lastFrame = null;
            }

            lastFrame = stream.readFrame();
            ByteBuffer frameData = lastFrame.getData().order(ByteOrder.LITTLE_ENDIAN);

            // todo this should not change. 
            // make sure we have enough room
//            if (mImagePixels == null || mImagePixels.length < lastFrame.getWidth() * lastFrame.getHeight()) {
//                mImagePixels = new int[lastFrame.getWidth() * lastFrame.getHeight()];
//            }
            int deviceWidth = grabber.getImageWidth();
            int deviceHeight = grabber.getImageHeight();

            // To Java/Processing -  Int format
            // Not used yet
            // IF 3 channels
            if (true) {
                int iplDepth = IPL_DEPTH_8U;
                int channels = 3;
                int frameSize = deviceWidth * deviceHeight * channels;

                byte[] frameDataBytes = new byte[frameSize];
                frameData.get(frameDataBytes);
                if (rawVideoImage == null || rawVideoImage.width() != deviceWidth || rawVideoImage.height() != deviceHeight) {
                    rawVideoImage = opencv_core.IplImage.create(deviceWidth, deviceHeight, iplDepth, channels);
                    rawVideoImageGray = opencv_core.IplImage.create(deviceWidth, deviceHeight, iplDepth, 1);
                }
                rawVideoImage.getByteBuffer().put(frameDataBytes, 0, frameSize);
//                opencv_imgproc.cvCvtColor(rawVideoImage, rawVideoImage, COLOR_RGB2BGR);
//                opencv_imgproc.cvCvtColor(rawVideoImage, rawVideoImageGray, COLOR_BGR2GRAY);
                grabber.updateCurrentImage(rawVideoImage);
//                camera.updateCurrentImage(rawVideoImageGray);
            }

            // if depth -- Not supported
//            if (false) {
//                int iplDepth = IPL_DEPTH_8U;
//                int channels = 2;
//
//                int frameSize = deviceWidth * deviceHeight * channels;
//                // TODO: Handle as a sort buffer instead of byte.
//                byte[] frameDataBytes = new byte[frameSize];
//                frameData.get(frameDataBytes);
//                if (rawVideoImage == null || rawVideoImage.width() != deviceWidth || rawVideoImage.height() != deviceHeight) {
//                    rawVideoImage = opencv_core.IplImage.create(deviceWidth, deviceHeight, iplDepth, channels);
//                }
//                rawVideoImage.getByteBuffer().put(frameDataBytes, 0, frameSize);
//                camera.updateCurrentImage(rawVideoImage);
//            }
        }
    }

    @Override
    public void trigger() throws Exception {
    }

    public int getDepthImageWidth() {
        return depthImageWidth;
    }

    public void setDepthImageWidth(int depthImageWidth) {
        this.depthImageWidth = depthImageWidth;
    }

    public int getDepthImageHeight() {
        return depthImageHeight;
    }

    public void setDepthImageHeight(int depthImageHeight) {
        this.depthImageHeight = depthImageHeight;
    }

    public int getIRImageWidth() {
        return IRImageWidth;
    }

    public void setIRImageWidth(int IRImageWidth) {
        this.IRImageWidth = IRImageWidth;
    }

    public int getIRImageHeight() {
        return IRImageHeight;
    }

    public void setIRImageHeight(int IRImageHeight) {
        this.IRImageHeight = IRImageHeight;
    }

    public int getDepthFrameRate() {
        return depthFrameRate;
    }

    public void setDepthFrameRate(int frameRate) {
        this.depthFrameRate = frameRate;
    }

    public int getIRFrameRate() {
        return IRFrameRate;
    }

    public void setIRFrameRate(int IRFrameRate) {
        this.IRFrameRate = IRFrameRate;
    }

    @Override
    public double getGamma() {
        // I guess a default gamma of 2.2 is reasonable...
        if (gamma == 0.0) {
            return 2.2;
        } else {
            return gamma;
        }
    }

    // Gamma from the device is not good.
//    @Override
//    public double getGamma(){
//        double gamma = device.get_option(RealSense.RS_OPTION_COLOR_GAMMA);
//        System.out.println("Getting cameraGamma " + gamma);
//        return gamma;
//    }
}
