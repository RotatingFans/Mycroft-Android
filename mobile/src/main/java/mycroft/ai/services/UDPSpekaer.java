package mycroft.ai.services;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

public class UDPSpekaer {


    static AudioFormat format;
    static int port = 50005;
    static int sampleRate = 16000;
    static int channels = 1;
    private boolean status = false;
    public UDPSpekaer() {

    }

    public void stopListener () {
        status = false;
        Log.d("VS","Recorder released");
    }
    public void startListener() {


        status = true;
        startStreaming();


    }

    private void startStreaming() {
        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    DatagramSocket serverSocket = new DatagramSocket(50006);
                    serverSocket.setSoTimeout(100);
                    serverSocket.setReceiveBufferSize(327680);
                    System.out.println(serverSocket.getReceiveBufferSize());

                    // ( 1280 for 16 000Hz and 3584 for 44 100Hz (use AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) to get the correct size)


                    while (status == true) {
                        try {

                            byte[] receiveData = new byte[4096];
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            DatagramPacket receivePacket = new DatagramPacket(receiveData,
                                    receiveData.length);
                            serverSocket.receive(receivePacket);

                            receiveData = receivePacket.getData();
                            String result = new String(receiveData, "ASCII");

                            if (result.contains("START")) {
                                // serverSocket.receive(receivePacket);

                                result = result.substring(result.indexOf("{") + 1, result.indexOf("}"));
                                String[] data = result.split(",");

                                sampleRate = Integer.valueOf(data[0].substring(4));
                                channels = Integer.valueOf(data[1].substring(4));

                                receivePacket.setData("STARTING".getBytes("ASCII"));
                                serverSocket.send(receivePacket);
                                int retries = 0;

                                while (new String(receiveData, "ASCII").contains("START") && retries < 10) {
                                    receivePacket.setData("STARTING".getBytes("ASCII"));
                                    serverSocket.send(receivePacket);
                                    Thread.sleep(100);
                                    try {
                                        serverSocket.receive(receivePacket);

                                        receiveData = receivePacket.getData();
                                    } catch (SocketTimeoutException e) {
                                        receiveData = new byte[4096];
                                    }
                                    retries++;

                                }


                                int format = AudioFormat.CHANNEL_OUT_MONO;
                                if (channels == 2) {
                                    format = AudioFormat.CHANNEL_OUT_STEREO;
                                }
                                int intSize = android.media.AudioTrack.getMinBufferSize(sampleRate, format, AudioFormat.ENCODING_PCM_16BIT);
                                System.out.println("Sample Rate " + sampleRate);
                                AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, format, AudioFormat.ENCODING_PCM_16BIT, intSize, AudioTrack.MODE_STREAM);
                                receiveData = new byte[512];

                                receivePacket = new DatagramPacket(receiveData,
                                        receiveData.length);
                                serverSocket.receive(receivePacket);
                                System.out.println(serverSocket.getReceiveBufferSize());

                                receiveData = receivePacket.getData();
                                int packets = 0;
                                if (at != null) {
                                    // Write the byte array to the track
                                    int failures = 0;
                                    at.play();

                                    while (!new String(receiveData, "ASCII").startsWith("START")) {


                                        at.write(receiveData, 0, receiveData.length);
                                            serverSocket.receive(receivePacket);
                                        //baos.write(receiveData);
                                        //failures = 0;
                                        //packets++;

                                    }
                                    at.stop();
                                    at.release();
                                    System.out.println("Sent # packets: " + packets);
                                    System.out.println(serverSocket.getReceiveBufferSize());

                                   /* while (!new String(receiveData, "ASCII").contains("END") & failures < 10) {
                                        baos.write(receiveData);

                                        try {
                                            serverSocket.receive(receivePacket);

                                            receiveData = receivePacket.getData();
                                            failures = 0;

                                        } catch (SocketTimeoutException e) {
                                            failures++;
                                        }
                                    }
                                    retries = 0;
                                    while ((new String(receiveData, "ASCII").startsWith("END")) && retries < 10) {

                                        System.out.println(new String(receiveData, "ASCII"));
                                        receivePacket.setData("ENDING".getBytes("ASCII"));
                                        serverSocket.send(receivePacket);
                                        try {
                                            serverSocket.receive(receivePacket);
                                            Thread.sleep(100);
                                            receiveData = receivePacket.getData();
                                        } catch (SocketTimeoutException e) {
                                            receiveData = new byte[4096];
                                        }


                                        retries++;
                                    }
                                    receivePacket.setData("ENDING".getBytes("ASCII"));
                                    serverSocket.send(receivePacket);*/

                                }
                            }


                        } catch (SocketTimeoutException ignored) {

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
        streamThread.setPriority(Thread.MAX_PRIORITY);
        streamThread.start();
    }

    public boolean getListening() {
        return status;
    }
}
