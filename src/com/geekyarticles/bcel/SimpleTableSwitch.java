package com.geekyarticles.bcel;

import java.io.IOException;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.AALOAD;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BIPUSH;
import org.apache.bcel.generic.BranchHandle;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.TABLESWITCH;
import org.apache.bcel.generic.Type;

public class SimpleTableSwitch {

    
    public static void main(String[] args) {
        //Create a ClassGen for our brand new class.
        ClassGen classGen=new ClassGen("com.geekyarticles.bcel.SyntheticClass", 
        		"java.lang.Object", "SyntheticClass.java", Constants.ACC_PUBLIC, null);
        
        //Get a reference to the constant pool of the class. This will be modified 
        //as we add methods, fields etc. Note that it already constains
        //a few constants.
        ConstantPoolGen constantPoolGen=classGen.getConstantPool();
        
        //The list of instructions for a method. 
        InstructionList instructionList=new InstructionList();
        
        //Add the appropriate instructions.
        

        
        instructionList.append(new ALOAD(0));//the argument of the main function
        instructionList.append(new BIPUSH((byte)0));//Push 0.
        instructionList.append(new AALOAD());//Got value of the variable as a String;
        instructionList.append(new INVOKESTATIC(constantPoolGen.addMethodref("java.lang.Integer", 
        		"parseInt", "(Ljava/lang/String;)I")));//Now we got the value as int

        
        InstructionHandle case1=instructionList.append(new GETSTATIC(constantPoolGen.addFieldref("java.lang.System", 
        		"out", "Ljava/io/PrintStream;")));        
        instructionList.append(new LDC(constantPoolGen.addString("One")));
        instructionList.append(new INVOKEVIRTUAL(constantPoolGen.addMethodref("java.io.PrintStream", 
        		"println", "(Ljava/lang/String;)V")));//Print
        BranchHandle goto1=instructionList.append(new GOTO(null));
        
        InstructionHandle case2=instructionList.append(new GETSTATIC(constantPoolGen.addFieldref("java.lang.System", 
        		"out", "Ljava/io/PrintStream;")));        
        instructionList.append(new LDC(constantPoolGen.addString("Two")));
        instructionList.append(new INVOKEVIRTUAL(constantPoolGen.addMethodref("java.io.PrintStream", 
        		"println", "(Ljava/lang/String;)V")));//Print
        BranchHandle goto2=instructionList.append(new GOTO(null));
        
        
        InstructionHandle case4=instructionList.append(new GETSTATIC(constantPoolGen.addFieldref("java.lang.System", 
        		"out", "Ljava/io/PrintStream;")));        
        instructionList.append(new LDC(constantPoolGen.addString("Four")));
        instructionList.append(new INVOKEVIRTUAL(constantPoolGen.addMethodref("java.io.PrintStream", 
        		"println", "(Ljava/lang/String;)V")));//Print
        BranchHandle goto4=instructionList.append(new GOTO(null));
        
        InstructionHandle caseDefault=instructionList.append(new GETSTATIC(constantPoolGen.addFieldref("java.lang.System", 
        		"out", "Ljava/io/PrintStream;")));    
        instructionList.append(new LDC(constantPoolGen.addString("Other")));
        instructionList.append(new INVOKEVIRTUAL(constantPoolGen.addMethodref("java.io.PrintStream", 
        		"println", "(Ljava/lang/String;)V")));//Print
        BranchHandle gotoDefault=instructionList.append(new GOTO(null));
        
        InstructionHandle ret=instructionList.append(new RETURN());
        
        //We did not know the Instructions earlier. So the switch statement must be inserted
        instructionList.insert(case1,new TABLESWITCH(new int [] {1,2,3,4},
        		new InstructionHandle[]{case1,case2,caseDefault, case4}, caseDefault));
        goto1.setTarget(ret);
        goto2.setTarget(ret);
        goto4.setTarget(ret);
        gotoDefault.setTarget(ret);
        
        
        //Now generate the method
        
        MethodGen methodGen=new MethodGen(Constants.ACC_PUBLIC|Constants.ACC_STATIC, 
        		Type.VOID, new Type[]{new ArrayType(Type.STRING, 1)}, new String[]{"args"}, "main", 
        		"com.geekyarticles.bcel.SyntheticClass", instructionList, constantPoolGen);
        
        methodGen.setMaxLocals();//Calculate the maximum number of local variables. 
        methodGen.setMaxStack();//Very important: must calculate the maximum size of the stack.
        
        classGen.addMethod(methodGen.getMethod()); //Add the method to the class
        
        //Print a few things.
        System.out.println("********Constant Pool**********");
        System.out.println(constantPoolGen.getFinalConstantPool());
        System.out.println("********Method**********");
        System.out.println(methodGen);
        System.out.println("********Instruction List**********");
        System.out.println(instructionList);
        

        //Now generate the class
        JavaClass javaClass=classGen.getJavaClass();
        try {
            //Write the class byte code into a file
            javaClass.dump("bin/com/geekyarticles/bcel/SyntheticClass.class");
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        //That's it.

    }

}