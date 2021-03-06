package mycroft.ai.services;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class UDPMicListener {

    public byte[] buffer;
    public static DatagramSocket socket;
    private int port=50005;

    AudioRecord recorder;

    private int sampleRate = 16000 ; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    private boolean status = false;
    private String recvAddr = "192.168.1.1";
    public UDPMicListener(String receiveAddress) {
        this.recvAddr = receiveAddress;
    }

    public void stopListener () {
            status = false;
            recorder.release();
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

                    DatagramSocket socket = new DatagramSocket();
                    Log.d("VS", "Socket Created");

                    byte[] buffer = new byte[minBufSize];

                    Log.d("VS","Buffer created of size " + minBufSize);
                    DatagramPacket packet;

                    final InetAddress destination = InetAddress.getByName(recvAddr);
                    Log.d("VS", "Address retrieved");


                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig,audioFormat,minBufSize*10);
                    if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                        Log.d("VS", "Recorder initialized");

                        recorder.startRecording();


                        while (status == true) {


                            //reading data from MIC into buffer
                            minBufSize = recorder.read(buffer, 0, buffer.length);

                            //putting buffer in the packet
                            packet = new DatagramPacket(buffer, buffer.length, destination, port);

                            socket.send(packet);
                            //System.out.println("MinBufferSize: " + minBufSize);


                        }
                    } else {
                        Log.e("VS", "Recorder Failed to Initialize");

                    }



                } catch(UnknownHostException e) {
                    Log.e("VS", "UnknownHostException");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("VS", "IOException");
                } catch(IllegalStateException e) {
                Log.e("VS", "IllegalStateException");
                } finally {
                    recorder.release();
                }
            }

        });
        streamThread.start();
    }

    public boolean getListening() {
        return status;
    }
}
