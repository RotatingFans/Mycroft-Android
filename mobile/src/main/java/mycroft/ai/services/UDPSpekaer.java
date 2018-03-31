package mycroft.ai.services;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

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


                    DatagramSocket serverSocket = new DatagramSocket(50005);



                    // ( 1280 for 16 000Hz and 3584 for 44 100Hz (use AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) to get the correct size)


                    while (status == true) {
                        byte[] receiveData = new byte[4096];
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DatagramPacket receivePacket = new DatagramPacket(receiveData,
                                receiveData.length);
                        serverSocket.receive(receivePacket);

                        receiveData = receivePacket.getData();
                        if (new String(receiveData, 0, receivePacket.getLength()).equals("START")) {
                            serverSocket.receive(receivePacket);

                            receiveData = receivePacket.getData();
                            sampleRate = Integer.valueOf(new String(receiveData, 0, receivePacket.getLength()));
                            serverSocket.receive(receivePacket);

                            receiveData = receivePacket.getData();
                            channels = Integer.valueOf(new String(receiveData, 0, receivePacket.getLength()));
                            int format = AudioFormat.CHANNEL_OUT_MONO;
                            if (channels == 2) {
                                format = AudioFormat.CHANNEL_OUT_STEREO;
                            }
                            int intSize = android.media.AudioTrack.getMinBufferSize(sampleRate, format, AudioFormat.ENCODING_PCM_16BIT);
                            System.out.println("Sample Rate " + sampleRate);
                            AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, format, AudioFormat.ENCODING_PCM_16BIT, intSize, AudioTrack.MODE_STREAM);
                            serverSocket.receive(receivePacket);

                            receiveData = receivePacket.getData();
                            if (at != null) {
                                // Write the byte array to the track
                                while (!new String(receiveData, 0, receivePacket.getLength()).equals("END")) {
                                    baos.write(receiveData);


                                    serverSocket.receive(receivePacket);

                                    receiveData = receivePacket.getData();
                                }
                                at.play();
                                at.write(baos.toByteArray(), 0 , baos.toByteArray().length);
                                at.stop();
                                at.release();
                            }
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
        streamThread.start();
    }

    public boolean getListening() {
        return status;
    }
}
