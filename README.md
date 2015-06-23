# Hello BCEL
The ByteCode Engineering Library (BCEL) is a library that provides a way to 
directly modify the bytecode of Java classes. 

This repository contains slightly modified code for a three-part tutorial 
at GeekyArticles: [Part 1](http://www.geekyarticles.com/2011/08/manipulating-java-class-files-with-bcel.html), 
[Part 2](http://www.geekyarticles.com/2011/08/manipulating-java-class-files-with-bcel_18.html), 
and [Part 3](http://www.geekyarticles.com/2011/08/manipulating-java-class-files-with-bcel_25.html).

You need to 
[download BCEL](http://commons.apache.org/proper/commons-bcel/download_bcel.cgi) 
for this example to work. 

### How it Works
* Each class in the `src` folder is run individually. 
  * Each one except for `DisplayDetails` and `ExpressionCompiler`  
    generates a class file called `SyntheticClass.class` in the `bin`
    folder with the other class files. 
  * For the classes that generates `SyntheticClass` you can then run
    the `SyntheticClass` to see the results. 
    *  The tutorial links above give a little more details on how to run each class. 
  * `ExpresionCompiler` takes a string containing an expression as a command line 
    argument and parses it, then generates `ExpressionTest.class` which displays
    the result of the expression when its `main` function is called.