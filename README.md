# working-with-wav-files-in-android
A repository which contains the complete code to read data from an wav file and to create a new one for android applications.

# Motivation 
In python, it is quite easy to read data from an wav file and to write back into another one aka creation of a new wav file. All the byte level complexities have already been handled in various popular libraries (e.g., SciPy). But, I could not find any dedicated libaries for this in android. So, had to go through complete documentation and lots of stackoverflow answers to grasp the whole thing, the wav file structure, the byte level operations, the permissions etc. Thought to put up all the investigations in one place. 

## Logics and Codes
Now, I will see breakdown each task one by one and discuss my approach. This writeup mainly focuses on how we can read the complete byte information from an wav file, convert it to an workable state so that we can perform manipulation and finally how we can construct a new wav file after the manipulation of the data along with accessing the newly created file. Sections will be, 
- [Accessing a file](#accessing-a-file)

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

We have to read byte by byte from the file to extract all the information. To initiate the reading pointer, we will use the following code snippet. 
```
File file = new File(audioFile); // absolute path of the audio file 
InputStream fileInputstream = new FileInputStream(file); // we will use InputStream to read the bytes
```




