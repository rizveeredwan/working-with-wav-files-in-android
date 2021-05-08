# working-with-wav-files-in-android
A repository which contains the complete code to read data from an wav file and to create a new one for android applications.

# Motivation 
In python, it is quite easy to read data from an wav file and to write back into another one aka creation of a new wav file. All the byte level complexities have already been handled in various popular libraries (e.g., SciPy). But, I could not find any dedicated libaries for this in android. So, had to go through complete documentation and lots of stackoverflow answers to grasp the whole thing, the wav file structure, the byte level operations, the permissions etc. Thought to put up all the investigations in one place. 

## Logics and Codes
Now, I will see breakdown each task one by one and discuss my approach. This writeup mainly focuses on how we can read the complete byte information from an wav file, convert it to an workable state so that we can perform manipulation and finally how we can construct a new wav file after the manipulation of the data along with accessing the newly created file. Sections will be, 
- [Accessing a file](#accessing-a-file)

### Accessing a file
I put my wav file in the ``asset`` folder. I have attached a screenshot to understand my ![File Structure](https://github.com/rizveeredwan/working-with-wav-files-in-android/blob/main/file_structure.png). 
