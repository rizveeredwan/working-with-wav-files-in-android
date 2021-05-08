
package com.example.myfirstapp;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.icu.text.SimpleDateFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.rtp.AudioStream;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.IValue;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;

import static androidx.core.app.ActivityCompat.requestPermissions;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;


class AudioFileProcess {
    int type [] = {0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1};
    int numberOfBytes[] = {4, 4, 4, 4, 4, 2, 2, 4, 4, 2, 2, 4, 4};
    int chunkSize, subChunk1Size, sampleRate, byteRate, subChunk2Size=1, bytePerSample;
    short audioFomart, numChannels, blockAlign, bitsPerSample=8;
    String chunkID, format, subChunk1ID, subChunk2ID;


    public ByteBuffer ByteArrayToNumber(byte bytes[], int numOfBytes, int type){
        ByteBuffer buffer = ByteBuffer.allocate(numOfBytes);
        if (type == 0){
            buffer.order(BIG_ENDIAN); // Check the illustration. If it says little endian, use LITTLE_ENDIAN
        }
        else{
            buffer.order(LITTLE_ENDIAN);
        }
        buffer.put(bytes);
        buffer.rewind();
        return buffer;
    }

    public float convertToFloat(byte[] array, int type) {
        ByteBuffer buffer = ByteBuffer.wrap(array);
        if (type == 1){
            buffer.order(LITTLE_ENDIAN);
        }
        return (float) buffer.getShort();
    }

    public float[] ReadingAudioFile(String audioFile) throws IOException{
        try {
            File file = new File(audioFile);
            int length = (int) file.length();
            //System.out.println(length);
            InputStream fileInputstream = new FileInputStream(file);
            ByteBuffer byteBuffer;
            for(int i=0; i<numberOfBytes.length; i++){
                byte byteArray[] = new byte[numberOfBytes[i]];
                int r = fileInputstream.read(byteArray, 0, numberOfBytes[i]);
                byteBuffer = ByteArrayToNumber(byteArray, numberOfBytes[i], type[i]);
                if (i == 0) {chunkID =  new String(byteArray); System.out.println(chunkID);}
                if (i == 1) {chunkSize = byteBuffer.getInt(); System.out.println(chunkSize);}
                if (i == 2) {format =  new String(byteArray); System.out.println(format);}
                if (i == 3) {subChunk1ID = new String(byteArray);System.out.println(subChunk1ID);}
                if (i == 4) {subChunk1Size = byteBuffer.getInt(); System.out.println(subChunk1Size);}
                if (i == 5) {audioFomart = byteBuffer.getShort(); System.out.println(audioFomart);}
                if (i == 6) {numChannels = byteBuffer.getShort();System.out.println(numChannels);}
                if (i == 7) {sampleRate = byteBuffer.getInt();System.out.println(sampleRate);}
                if (i == 8) {byteRate = byteBuffer.getInt();System.out.println(byteRate);}
                if (i == 9) {blockAlign = byteBuffer.getShort();System.out.println(blockAlign);}
                if (i == 10) {bitsPerSample = byteBuffer.getShort();System.out.println(bitsPerSample);}
                if (i == 11) {
                    subChunk2ID = new String(byteArray) ;
                    if(subChunk2ID.compareTo("data") == 0) {
                        continue;
                    }
                    else if( subChunk2ID.compareTo("LIST") == 0) {
                        byte byteArray2[] = new byte[4];
                        r = fileInputstream.read(byteArray2, 0, 4);
                        byteBuffer = ByteArrayToNumber(byteArray2, 4, 1);
                        int temp = byteBuffer.getInt();
                        //redundant data reading
                        byte byteArray3[] = new byte[temp];
                        r = fileInputstream.read(byteArray3, 0, temp);
                        r = fileInputstream.read(byteArray2, 0, 4);
                        subChunk2ID = new String(byteArray2) ;
                    }
                }
                if (i == 12) {subChunk2Size = byteBuffer.getInt();System.out.println(subChunk2Size);}
            }
            bytePerSample = bitsPerSample/8;
            float value;
            ArrayList<Float> dataVector = new ArrayList<>();
            while (true){
                byte byteArray[] = new byte[bytePerSample];
                int v = fileInputstream.read(byteArray, 0, bytePerSample);
                value = convertToFloat(byteArray,1);
                dataVector.add(value);
                if (v == -1) break;
            }
            float data [] = new float[dataVector.size()];
            for(int i=0;i<dataVector.size();i++){
                data[i] = dataVector.get(i);
            }
            System.out.println("Total data bytes "+sum);
            return data;
        }
        catch (Exception e){
            System.out.println("Error: "+e);
            float[] f = new float[1];
            return f;
        }
    }

    public byte[] NumericalArray2ByteArray(short[] values){
        ByteBuffer buffer = ByteBuffer.allocate(2 * values.length);
        buffer.order(LITTLE_ENDIAN); // data must be in little endian format
        for (short value : values){
            buffer.putShort(value);
        }
        buffer.rewind();
        return buffer.array();
    }

    public void checkExternalMedia(){
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        System.out.println("\n\nExternal Media: readable="
                +mExternalStorageAvailable+" writable="+mExternalStorageWriteable);
    }

    public void WriteCleanAudioWav(Context context, String newFileName, short[] wavData) throws Exception{
        File dir = null;
        try{
            checkExternalMedia();
            File root = android.os.Environment.getExternalStorageDirectory();
            System.out.println("\nExternal file system root: "+root);
            dir = new File (root.getAbsolutePath() + "/clean_speech");
            if (dir.exists() == false){
                dir.mkdirs();
                System.out.println("Directory created");
                System.out.println(dir);
            }
            else{
                System.out.println("Directory exists");
                System.out.println(dir);
            }
        }
        catch (Exception e){
            System.out.println("Error "+e);
        }
        try {
            File file = new File(dir, "clean_20_sec.wav");
            if (file.exists()) {
                System.out.println("YES file exists");
            }
            //System.out.println(file);
            OutputStream os;
            //System.out.println(newFileName);
            os = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream outFile = new DataOutputStream(bos);

            try {
                long mySubChunk1Size = subChunk1Size;
                int myBitsPerSample= bitsPerSample;
                int myFormat = audioFomart;
                long myChannels = numChannels;
                long mySampleRate = sampleRate;
                long myByteRate = mySampleRate * myChannels * myBitsPerSample/8;
                int myBlockAlign = (int) (myChannels * myBitsPerSample/8);
                System.out.println("Ei porjonto completed");
                byte clipData[] = ShortArray2ByteArray(wavData);
                System.out.println("Ei porjonto completed 1");
                long myDataSize = clipData.length;
                long myChunk2Size =  myDataSize * myChannels * myBitsPerSample/8;
                long myChunkSize = 36 + myChunk2Size;

                outFile.writeBytes("RIFF");  // 00 - RIFF
                outFile.writeInt(Integer.reverseBytes((int)myChunkSize)); // 04 - how big is the rest of this file?
                outFile.writeBytes("WAVE");                                 // 08 - WAVE
                outFile.writeBytes("fmt ");                                 // 12 - fmt
                outFile.writeInt(Integer.reverseBytes((int)mySubChunk1Size));  // 16 - size of this chunk
                outFile.writeShort(Short.reverseBytes((short)myFormat));     // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
                outFile.writeShort(Short.reverseBytes((short)myChannels));   // 22 - mono or stereo? 1 or 2?  (or 5 or ???)
                outFile.writeInt(Integer.reverseBytes((int)mySampleRate));     // 24 - samples per second (numbers per second)
                outFile.writeInt(Integer.reverseBytes((int)myByteRate));       // 28 - bytes per second
                outFile.writeShort(Short.reverseBytes((short)myBlockAlign)); // 32 - # of bytes in one sample, for all channels
                outFile.writeShort(Short.reverseBytes((short)myBitsPerSample));  // 34 - how many bits in a sample(number)?  usually 16 or 24
                outFile.writeBytes("data");                                 // 36 - data
                outFile.writeInt(Integer.reverseBytes((int)myDataSize));       // 40 - how big is this data chunk
                outFile.write(clipData);
                // System.out.println("File creation complete");
            }
            catch (Exception e){
                System.out.println("Error "+e);

            }
            outFile.flush();
            outFile.close();
        }
        catch (Exception e){
            System.out.println("Error: "+e);
        }
    }
}


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Assume this Activity is the current activity
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck==PackageManager.PERMISSION_GRANTED){
            //this means permission is granted and you can do read and write
            System.out.println("First condition");
        }else{
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            System.out.println("Second condition");
        }

        String audioFileAbsolutePath = null;

        try{
            audioFileAbsolutePath = assetFilePath(this, "20_sec.wav");
            //System.out.println(audioFileAbsolutePath);
            AudioFileProcess audioFileProcess = new AudioFileProcess();
            float[] audioData = audioFileProcess.ReadingAudioFile(audioFileAbsolutePath);

            float [] manipulatedAudioData = new float[audioData.length];
            /*
              Assume did some manipulation on the wav file aka over audioData array[] and got new array named manipulatedAudioData[]
            */
            short int16[] =  float32ToInt16(manipulatedAudioData); // suppose, the new wav file's each sample will be in int16 Format
            audioFileProcess.WriteCleanAudioWav(this,"clean_20sec.wav", int16);
        }
        catch (Exception e){
            System.out.println("Error: "+e);
        }
    }


    public static short[] float32ToInt16(float arr[]){  //int[]
        // logic is built to support the numpy.int16 output
        short int16Arr[] = new short [arr.length];
        for(int i=0; i<arr.length; i++){
            if(arr[i]<0) {
                if (arr[i]>-1) {
                    int16Arr[i] = 0;
                }
                else{
                    int16Arr[i] = (short) Math.ceil((double)arr[i]);
                }
            }
            else if (arr[i]>0){
                int16Arr[i] = (short) Math.floor((double)arr[i]);
            }
            else{
                int16Arr[i] = 0;
            }
        }
        return int16Arr;
    }

    /**
     * Copies specified asset to the file in /files app directory and returns this file absolute path.
     *
     * @return absolute file path
     */
        public static String assetFilePath(Context context, String assetName) throws IOException {
            File file = new File(context.getFilesDir(), assetName);
            if (file.exists() && file.length() > 0) {
                return file.getAbsolutePath();
            }
        }
}
