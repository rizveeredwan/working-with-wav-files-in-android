# working-with-wav-files-in-android
A repository which contains the complete code to read data from an wav file and to create a new one for android applications.

# Motivation 
In python, it is quite easy to read data from an wav file and to write back into another one aka creation of a new wav file. All the byte level complexities have already been handled in various popular libraries (e.g., SciPy). But, I could not find any dedicated libaries for this in android. So, had to go through complete documentation and lots of stackoverflow answers to grasp the whole thing, the wav file structure, the byte level operations, the permissions etc. Thought to put up all the investigations in one place. 

## Logics and Codes
Now, I will see breakdown each task one by one and discuss my approach. This writeup mainly focuses on how we can read the complete byte information from an wav file, convert it to an workable state so that we can perform manipulation and finally how we can construct a new wav file after the manipulation of the data along with accessing the newly created file. Sections will be, 
- [Accessing a file](#accessing-a-file)
- [Structure of a WAV file](#structure-of-a-wav-file)
- [Reading a WAV file](#reading-a-wav-file)

### Accessing a file
I put my wav file in the ``asset`` folder. I have attached a screenshot to understand my file structure. Here, you can see the wav file (20_sec.wav). I had to access this file now.

![File Structure](https://github.com/rizveeredwan/working-with-wav-files-in-android/blob/main/file_structure.png). 

I used the following function, to get the complete path (absolute path) of the wav file. 
```
public static String assetFilePath(Context context, String assetName) throws IOException {
            File file = new File(context.getFilesDir(), assetName);
            if (file.exists() && file.length() > 0) {
                return file.getAbsolutePath();
            }
}
```
Now, based on the returned path, I could easily access the file. 

### Structure of a WAV file
First, I will refer to this ![documentation](soundfile.sapp.org/doc/WaveFormat/) which provides the file structure of an wav file. To summarize the whole information there, this following table can be seen, 

Order  | Offset | Name | Size (bytes) | Data Type | Expected value 
------------ | ------------- | --------- | --------------- | ----------------| --------
big | 0 | ChunkID | 4 | String |  "RIFF" 
little | 4 | ChunkSize | 4 | int | Denotes the size of the rest of the chunk
big | 8 | Format | 4 | String | "WAVE"
big | 12 | SubChunk1ID | 4 | String | "fmt " (there is a space)
little | 16 | SubChunk1Size | 4 | int | 16 for PCM
little | 20 | AudioFormat | 2 | short | 1 
little | 22 | NumChannels | 2 | short | 1 for mono, 2 for stereo
little | 24 | SampleRate | 4 | int | 16000, 44100, etc.
little | 28 | ByteRate | 4 | int | SampleRate * NumChannels * BitsPerSample/8
little | 32 | BlockAlign | 2 | short | NumChannels * BitsPerSample/8
little | 34 | BitsPerSample | 2 | short | 8 bits = 8, 16 bits = 16, etc
big | 36 | SubChunk2ID | 4 | string | "data"
little | 40 | SubChunk2Size | 4 | int | NumSamples * NumChannels * BitsPerSample/8
little | 44 | data | | | the actual data

But, there can be a very important variation. In offset 36-40, where we expect to have a string "data", we might get a different string which is "LIST". In this case, there will be some additional data and file structure will vary as follows, 

Order  | Offset | Name | Size (bytes) | Data Type | Expected value 
------------ | ------------- | --------- | --------------- | ----------------| --------
big | 36 | SubChunk2ID | 4 | string | "LIST"
little | 40 | Additional | 4 | int | This amount of byte value has to be skipped afterwards, lets say 26 is found
little | 44-69 | Additional Information | | Has to be skipped to get the actual data 
big | 70 | SubChunk2ID | 4 | string | "data"
little | 74 | SubChunk2Size | 4 | int | NumSamples * NumChannels * BitsPerSample/8
little | 78 | data | | | the actual data

Another important observation is, 
```
Strings are in BIG ENDIAN format
```
```
Numbers are in LITTLE ENDIAN format
```

### Reading a WAV file 
Here, we will discuss the important aspects to read a WAV file through the following points, 

- We have to read byte by byte from the file to extract all the information. To initiate the reading pointer, we will use the following code snippet. 
```
File file = new File(audioFile); // absolute path of the audio file 
InputStream fileInputstream = new FileInputStream(file); // we will use InputStream to read the bytes
```
- As, from the previous section, we already know the byte size of each field, so we can use that information to exact the value for each field. For example if we want to read ``ChunkID`` field, we can extract the byte information as follows,

```
byte byteArray[] = new byte[4]; // Declaration of Byte Array for the desired size
int r = fileInputstream.read(byteArray, 0, numberOfBytes[i]); // reading the byte information, r will store the number of bytes which is read
```
- We need to handle three types of data types here, String, int and short. String data type's values are stored here in BIG ENDIAN format and the numerical data type's values are stored here in LITTLE ENDIAN format. 
To read the String data types, we just have to do the following, 
```
chunkID =  new String(byteArray);
```
To, read the numerical data types, we have to take some extra measure. We will use ``ByteBuffer``, which provides a powerful interface to convert a byte array to a number (float, int, short, etc )
```
public ByteBuffer ByteArrayToNumber(byte bytes[], int numOfBytes, int type){
        ByteBuffer buffer = ByteBuffer.allocate(numOfBytes); //allocating a space in ByteBuffer object based on the desired numOfBytes value
        if (type == 0){
            buffer.order(BIG_ENDIAN); // setting the bit orders (BIG ENDIAN or LITTLE ENDIAN)
        }
        else{
            buffer.order(LITTLE_ENDIAN);
        }
        buffer.put(bytes); // putting the bytes 
        buffer.rewind(); // this is basically for byte size syncronization 
        return buffer;
}
byteBuffer = ByteArrayToNumber(byteArray, numOfBytes, type); // type = BIG or LITTLE ENDIAN, numOfBytes = denotes the allocating space size, byteArray = where the read byte information is
byteBuffer.getInt() // will provide the int converted value stored in byteBuffer 
byteBuffer.getShort() // will provide the short converted value stored in byteBuffer 
// ByteBuffer automatically handles the sign values and provides the correct output
```
Now, we will talk how, we can read the actual audio data. The logic is almost similar which we have discussed upto now. But, there are some important factor to highlight. In, ``BitsPerSample`` field, we get values like 8, 16, etc. Dividing it by 8 (8/8 =1, 16/8 = 2) we get the number of bytes required to read an individual sample. So, the whole reading audio data process can be done as follows,
```
public float convertToFloat(byte[] array, int type) { // Another way to convert a byte array to a number, you can also use the previous function
        ByteBuffer buffer = ByteBuffer.wrap(array);
        if (type == 1){
            buffer.order(LITTLE_ENDIAN);
        }
        return (float) buffer.getShort(); //converted it to short and then typecasted it to float
    }

bytePerSample = bitsPerSample/8; // how many byte(s) to read to get a single sample 
ArrayList<Float> dataVector = new ArrayList<>();
while (true){
    byte byteArray[] = new byte[bytePerSample]; // making byte array
    int v = fileInputstream.read(byteArray, 0, bytePerSample); // reading the byte(s)
    value = convertToFloat(byteArray,1); // I needed float value to work with, so using ByteBuffer first I converted it to short and then type casted it to float.
    dataVector.add(value); // A vector list container to store the value 
    if (v == -1) break; // when all the bytes will be read, we will get -1 in variable v
  }
```
This basically sums up the basic logic block to read an wav file. The complete code can be found in [WAVManipulation.java](https://github.com/rizveeredwan/working-with-wav-files-in-android/blob/main/WavManipulation.java). 

### Writing a WAV file 
As, we have already understood the file's (byte information and fields) structure, now to create a new WAV file, we just have to put all the informations accordingly, following the field's order, data types (String, int, short) and bit orders (BIG or LITTLE ENDIAN). The main points of the work flow can be stated as follows, 
- First, we need to create a pointer to write the data. We, will create the file in SD Card or external environment. Then based on that, we will create the file writing pointer. 
```
File root = android.os.Environment.getExternalStorageDirectory();
File dir = new File (root.getAbsolutePath() + "/new_wav_directory");
if (dir.exists() == false){
     dir.mkdirs();
}
File file = new File(dir, "new_wav_file.wav");
OutputStream os;
os = new FileOutputStream(file);
BufferedOutputStream bos = new BufferedOutputStream(os);
DataOutputStream outFile = new DataOutputStream(bos);
```
- Now, the challenge comes, how to convert the information of each field in bytes and write them. 
```
// To convert the strings, we can directly use writeBytes function
outFile.writeBytes("RIFF");
```
```
// To convert a numerical value to bytes and to write in LITTLE ENDIAN format we can use reverseBytes and corresponding data type function
outFile.writeInt(Integer.reverseBytes((int)ChunkSize)); // 04 - how big is the rest of this file?
outFile.writeShort(Short.reverseBytes((short)Format)); // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
```





