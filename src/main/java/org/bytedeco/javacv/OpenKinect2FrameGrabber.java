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
import java.nio.ShortBuffer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.freenect2;
import org.bytedeco.javacpp.freenect2.CpuPacketPipeline;
import org.bytedeco.javacpp.freenect2.FrameMap;
import org.bytedeco.javacpp.freenect2.Freenect2;
import org.bytedeco.javacpp.freenect2.Freenect2Device;
import org.bytedeco.javacpp.freenect2.PacketPipeline;
import org.bytedeco.javacpp.freenect2.SyncMultiFrameListener;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/**
 *
 * @author Jeremy Laviole
 */
public class OpenKinect2FrameGrabber extends FrameGrabber {

    public static String[] getDeviceDescriptions() throws FrameGrabber.Exception {
        tryLoad();
        String[] desc = new String[1];
        desc[0] = "No description yet.";
        return desc;
    }

    public static int DEFAULT_DEPTH_WIDTH = 640;
    public static int DEFAULT_DEPTH_HEIGHT = 480;
    public static int DEFAULT_COLOR_WIDTH = 640;
    public static int DEFAULT_COLOR_HEIGHT = 480;

    private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    private int depthImageWidth = DEFAULT_DEPTH_WIDTH;
    private int depthImageHeight = DEFAULT_DEPTH_HEIGHT;
    private int depthFrameRate = 60;

    private int IRImageWidth = DEFAULT_DEPTH_WIDTH;
    private int IRImageHeight = DEFAULT_DEPTH_HEIGHT;
    private int IRFrameRate = 60;

    private SyncMultiFrameListener frameListener;

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    public static OpenKinect2FrameGrabber createDefault(int deviceNumber) throws FrameGrabber.Exception {
        return new OpenKinect2FrameGrabber(deviceNumber);
    }

    public static OpenKinect2FrameGrabber createDefault(File deviceFile) throws Exception {
        throw new Exception(OpenKinect2FrameGrabber.class + " does not support File devices.");
    }

    public static OpenKinect2FrameGrabber createDefault(String devicePath) throws Exception {
        throw new Exception(OpenKinect2FrameGrabber.class + " does not support path.");
    }

    private static FrameGrabber.Exception loadingException = null;
    private static Freenect2 freenect2Context;

    public static void tryLoad() throws FrameGrabber.Exception {
        if (loadingException != null) {
            loadingException.printStackTrace();
            throw loadingException;
        } else {
            try {
                if (freenect2Context != null) {
                    return;
                }
                Loader.load(org.bytedeco.javacpp.freenect2.class);

                // Context is shared accross cameras. 
                freenect2Context = new Freenect2();
            } catch (Throwable t) {
                throw loadingException = new FrameGrabber.Exception("Failed to load " + OpenKinect2FrameGrabber.class, t);
            }
        }
    }

    private boolean colorEnabled = false;
    private boolean depthEnabled = false;
    private boolean IREnabled = false;

    private String serial;
    private Freenect2Device device = null;

    private int frameTypes = 0;

    public OpenKinect2FrameGrabber(int deviceNumber) {
        if (freenect2Context == null) {
            try {
                OpenKinect2FrameGrabber.tryLoad();
            } catch (Exception e) {
                System.out.println("Exception in the TryLoad !" + e);
                e.printStackTrace();
            }
        }
        if (freenect2Context == null) {
            System.out.println("FATAL error: OpenKinect2 camera: driver could not load.");
            System.exit(-1);
        }
        if (freenect2Context.enumerateDevices() == 0) {
            System.out.println("FATAL error: OpenKinect2: no device connected!");
            return;
        }
        device = null;
        PacketPipeline pipeline = null;

        pipeline = new CpuPacketPipeline();
//        pipeline = new libfreenect2::OpenGLPacketPipeline();
//        pipeline = new libfreenect2::OpenCLPacketPipeline(deviceId);
//        pipeline = new libfreenect2::CudaPacketPipeline(deviceId);

        serial = freenect2Context.getDefaultDeviceSerialNumber().getString();
        device = freenect2Context.openDevice(serial, pipeline);
    }

    public static void main(String[] args) {
        try {
            OpenKinect2FrameGrabber.tryLoad();
        } catch (Exception e) {
            System.out.println("Exception in the TryLoad !" + e);
            e.printStackTrace();
        }
        Freenect2Device device = null;
        PacketPipeline pipeline = null;
        String serial = "";

        pipeline = new CpuPacketPipeline();
//        pipeline = new libfreenect2::OpenGLPacketPipeline();
//        pipeline = new libfreenect2::OpenCLPacketPipeline(deviceId);
//        pipeline = new libfreenect2::CudaPacketPipeline(deviceId);

        if (serial == "") {
            serial = freenect2Context.getDefaultDeviceSerialNumber().getString();
            System.out.println("Serial:" + serial);
        }

        device = freenect2Context.openDevice(serial, pipeline);
//       dev = freenect2.openDevice(serial);
        // [listeners]
        int types = 0;
//        if (enable_rgb) {
        types |= freenect2.Frame.Color;
//        if (enable_depth) {
        types |= freenect2.Frame.Ir | freenect2.Frame.Depth;
//        }

        SyncMultiFrameListener listener = new freenect2.SyncMultiFrameListener(types);

        device.setColorFrameListener(listener);
        device.setIrAndDepthFrameListener(listener);

        device.start();

        System.out.println("Serial: " + device.getSerialNumber().getString());
        System.out.println("Firmware: " + device.getFirmwareVersion().getString());
/// [start]

//  libfreenect2::Registration* registration = new libfreenect2::Registration(dev->getIrCameraParams(), dev->getColorCameraParams());
//  libfreenect2::Frame undistorted(512, 424, 4), registered(512, 424, 4);
        FrameMap frames = new FrameMap();
        // Fetch 100 frames. 
        int frameCount = 0;
        for (int i = 0; i < 100; i++) {
            System.out.println("getting frame " + frameCount);
            if (!listener.waitForNewFrame(frames, 10 * 1000)) // 10 sconds
            {
                System.out.println("timeout!");
                return;
            }

            freenect2.Frame rgb = frames.get(freenect2.Frame.Color);
            freenect2.Frame ir = frames.get(freenect2.Frame.Ir);
            freenect2.Frame depth = frames.get(freenect2.Frame.Depth);
/// [loop start]
            System.out.println("RGB image, w:" + rgb.width() + " " + rgb.height());
            byte[] imgData = new byte[1000];
//            ByteBuffer rgbData = 
            rgb.data().get(imgData);
//            byte[] array = rgbData.array();
            for (int pix = 0; pix < 10; pix++) {
                System.out.print(imgData[pix] + " ");
            }
            System.out.println();
            frameCount++;
            listener.release(frames);
            continue;
        }

        device.stop();
        device.close();
    }

    public void enableColorStream() {
        if (!colorEnabled) {
            frameTypes |= freenect2.Frame.Color;
            colorEnabled = true;
        }
    }

    public void enableDepthStream() {
        if (!depthEnabled) {
            frameTypes |= freenect2.Frame.Depth;
//            device.enable_stream(RealSense.depth, depthImageWidth, depthImageHeight, RealSense.z16, depthFrameRate);
            depthEnabled = true;
        }
    }

    public void enableIRStream() {
        if (!IREnabled) {
            frameTypes |= freenect2.Frame.Ir;
//            device.enable_stream(RealSense.infrared, IRImageWidth, IRImageHeight, RealSense.y8, IRFrameRate);
            IREnabled = true;
        }
    }

    public void release() throws FrameGrabber.Exception {
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }

    @Override
    public void start() throws FrameGrabber.Exception {
        frameListener = new freenect2.SyncMultiFrameListener(frameTypes);

        if (colorEnabled) {
            device.setColorFrameListener(frameListener);
        }
        if (depthEnabled || colorEnabled) {
            device.setIrAndDepthFrameListener(frameListener);
        }
        device.start();

        System.out.println("OpenKinect2 device started.");
        System.out.println("Serial: " + device.getSerialNumber().getString());
        System.out.println("Firmware: " + device.getFirmwareVersion().getString());
    }

    /**
     *
     * @throws Exception
     */
    @Override
    public void stop() throws FrameGrabber.Exception {
        device.stop();
        frameNumber = 0;
    }

//    private Pointer rawVideoImageData;
    private IplImage rawVideoImage = null;
    private IplImage videoImageRGBA = null;

    protected void grabVideo() {

        if (!colorEnabled) {
            System.out.println("Color stream not enabled, impossible to get the image.");
            return;
        }

        int iplDepth = IPL_DEPTH_8U;
        freenect2.Frame rgb = frames.get(freenect2.Frame.Color);

        int channels = (int) rgb.bytes_per_pixel();
        int deviceWidth = (int) rgb.width();
        int deviceHeight = (int) rgb.height();

        Pointer rawVideoImageData = rgb.data();
        if (rawVideoImage == null) {
            rawVideoImage = IplImage.createHeader(deviceWidth, deviceHeight, iplDepth, channels);
        }
        cvSetData(rawVideoImage, rawVideoImageData, deviceWidth * channels * iplDepth / 8);

        if (iplDepth > 8 && !ByteOrder.nativeOrder().equals(byteOrder)) {
            // ack, the camera's endianness doesn't correspond to our machine ...
            // swap bytes of 16-bit images
            ByteBuffer bb = rawVideoImage.getByteBuffer();
            ShortBuffer in = bb.order(ByteOrder.BIG_ENDIAN).asShortBuffer();
            ShortBuffer out = bb.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            out.put(in);
        }

        if (videoImageRGBA == null) {
            videoImageRGBA = rawVideoImage.clone();
        }
        cvCvtColor(rawVideoImage, videoImageRGBA, COLOR_BGRA2RGBA);
    }

    private FrameMap frames = new FrameMap();

    /**
     *
     * @return null grabs all images, get them with grabColor, grabDepth, and
     * grabIR instead.
     * @throws org.bytedeco.javacv.FrameGrabber.Exception
     */
    public Frame grab() throws Exception {

        System.out.println("OpenKinect2 grab");
        if (!frameListener.waitForNewFrame(frames, 10 * 1000)) // 10 seconds
        {
            System.out.println("Openkinect2: timeout!");
            // TODO: throw exception
        }
        frameNumber++;
        System.out.println("OpenKinect2 get images");
        if (colorEnabled) {
            grabVideo();
        }
        if (IREnabled) {
        }
        if (depthEnabled) {
        }

            frameListener.release(frames);
        return null;
//            freenect2.Frame ir = frames.get(freenect2.Frame.Ir);
//            freenect2.Frame depth = frames.get(freenect2.Frame.Depth);
    }

    public IplImage getVideoImage() {
        return videoImageRGBA;
    }

    @Override
    public void trigger() throws Exception {
//        device.wait_for_frames();
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
}
