package com.geekyarticles.bcel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.AALOAD;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DADD;
import org.apache.bcel.generic.DDIV;
import org.apache.bcel.generic.DMUL;
import org.apache.bcel.generic.DNEG;
import org.apache.bcel.generic.DSUB;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.Type;


public class ExpressionCompiler {

    /**
     * @param args
     * @throws ExpressionException 
     */
    public static void main(String[] args) throws ExpressionException {
        if(args.length!=1){
            System.out.println("Usage: java ExpressionCompiler <Expression>");
            return;
        }
        
        String expr=args[0];
        //Generate tokens
        List<Token> tokens=lex(expr);
        System.out.println(tokens);
        
        //Generate parse tree
        Symbol symbol=parse(tokens, expr);
        System.out.println(symbol);
        
        //Generate intermdiate code
        List<Symbol> flattened=flatten(symbol);
        printSymbols(flattened);
        
        //Create variable list
        List<String> varList=varList(flattened);
        System.out.println(varList);
        
        //Optimize
        List<Symbol> optimized=optimize(flattened);
        printSymbols(optimized);
        
        //Generate code
        generateCode("com.geekyarticles.bcel.ExpressionTest",varList,optimized);
        
        

    }


    public static void printSymbols(List<Symbol> flattened) {
        for(Symbol s:flattened){
            if(s instanceof Terminal){
                System.out.print(((Terminal) s).terminalToken.value+" ");
            }else if(s instanceof BinaryNonTerminal){
                System.out.print(((BinaryNonTerminal) s).operator+" ");
            }else if(s instanceof UnaryNonTerminal){
                System.out.print(((UnaryNonTerminal) s).operator+":U ");
            }
        }
        System.out.println();
    }

    public static class ExpressionException extends Exception{
        public String expression;
        public String message;
        public int position;
        public ExpressionException(String expr, int pos, String msg){
            expression=expr;
            position=pos;
            message=msg;
        }
        public String toString(){
            StringBuilder errMsg=new StringBuilder();
            errMsg.append(message).append("\n");
            errMsg.append(expression).append("\n");
            for(int i=0;i<expression.length();i++){
                if(i==position){
                    errMsg.append('^');
                }else{
                    errMsg.append('_');
                }
            }
            return errMsg.toString();
        }
    }

    /*Lexer starts here*/

    public static enum TokenType{
        NUMERIC,VARIABLE, OPERATOR, PARENTHESES
    }

    public static enum LexerStates{
        NUMERIC, SPACE, VARIABLE, BEGIN
    }
    public static class Token{
        public String value;
        public int position;
        public TokenType type;
        public String toString(){
            return type+":"+value;
        }
    }

    public static void error(String expr, int pos, String message) throws ExpressionException{
        throw new ExpressionException(expr, pos, message);
    }


    public static List<Token> lex(String expr) throws ExpressionException{
        List<Token> tokenList=new ArrayList<ExpressionCompiler.Token>();
        LexerStates state=LexerStates.BEGIN;
        char [] exprChars = expr.toCharArray();
        StringBuilder value=null;
        for(int i=0;i<exprChars.length;i++){
            char c=exprChars[i];

            switch(state){
            case BEGIN:
                if(c=='+'||c=='-'){
                    Token token=new Token();
                    token.position=i;
                    token.value=Character.valueOf(c).toString();
                    token.type=TokenType.OPERATOR;
                    tokenList.add(token);
                }else if(c>='0'&&c<='9'||c=='.'){
                    value=new StringBuilder();
                    value.append(c);
                    state=LexerStates.NUMERIC;
                }else if(c=='('){
                    Token token=new Token();
                    token.position=i;
                    token.value=Character.valueOf(c).toString();
                    token.type=TokenType.PARENTHESES;
                    tokenList.add(token);
                }else if((c>='A'&&c<='Z')||(c>='a'&&c<='z')||c=='$'||c=='_'){
                    value=new StringBuilder();
                    value.append(c);
                    state=LexerStates.VARIABLE;
                }else if(c==' '){
                    //do not do anything for white space
                }else{
                    error(expr, i, "Invalid character: "+c);
                }
                break;
            case NUMERIC:
                if(c=='+'||c=='-'||c=='*'||c=='/'){
                    Token token=new Token();
                    token.position=i;
                    token.value=value.toString();
                    token.type=TokenType.NUMERIC;
                    tokenList.add(token);

                    token=new Token();
                    token.position=i;
                    token.value=Character.valueOf(c).toString();
                    token.type=TokenType.OPERATOR;
                    tokenList.add(token);

                    state=LexerStates.BEGIN;
                }else if(c>='0'&&c<='9'||c=='.'){
                    if(c=='.'&&value.indexOf(".")>=0){
                        error(expr, i, "Invalid character: "+c);
                    }
                    value.append(c);
                }else if(c==')'){
                    Token token=new Token();
                    token.position=i;
                    token.value=value.toString();
                    token.type=TokenType.NUMERIC;
                    tokenList.add(token);

                    token=new Token();
                    token.position=i;
                    token.value=Character.valueOf(c).toString();
                    token.type=TokenType.PARENTHESES;
                    tokenList.add(token);
                    state=LexerStates.SPACE;
                }else if(c==' '){
                    Token token=new Token();
                    token.position=i;
                    token.value=value.toString();
                    token.type=TokenType.NUMERIC;
                    tokenList.add(token);
                    state=LexerStates.SPACE;
                }else{
                    error(expr, i, "Invalid character: "+c);
                }
                break;
            case SPACE:
                if(c=='+'||c=='-'||c=='*'||c=='/'){
                    Token token=new Token();
                    token=new Token();
                    token.position=i;
                    token.value=Character.valueOf(c).toString();
                    token.type=TokenType.OPERATOR;
                    tokenList.add(token);

                    state=LexerStates.BEGIN;
                }else if(c>='0'&&c<='9'||c=='.'){
                    value=new StringBuilder();
                    value.append(c);
                    state=LexerStates.NUMERIC;
                }else if(c=='('){
                    Token token=new Token();
                    token.position=i;
                    token.value=Character.valueOf(c).toString();
                    token.type=TokenType.PARENTHESES;
                    tokenList.add(token);
                    state=LexerStates.BEGIN;
                }else if(c==')'){
                    Token token=new Token();
                    token.position=i;
                    token.value=Character.valueOf(c).toString();
                    token.type=TokenType.PARENTHESES;
                    tokenList.add(token);
                    state=LexerStates.SPACE;
                }else if((c>='A'&&c<='Z')||(c>='a'&&c<='z')||c=='$'||c=='_'){
                    value=new StringBuilder();
                    value.append(c);
                    state=LexerStates.VARIABLE;
                }else if(c==' '){
                    //do not do anything for white space
                }else{
                    error(expr, i, "Invalid character: "+c);
                }
                break;
            case VARIABLE:
                if((c>='A'&&c<='Z')||(c>='a'&&c<='z')||c=='$'||c=='_'||c>='0'&&c<='9'){
                    value.append(c);
                }else if(c=='+'||c=='-'||c=='*'||c=='/'){
                    Token token=new Token();
                    token.position=i;
                    token.value=value.toString();
                    token.type=TokenType.VARIABLE;
                    tokenList.add(token);

                    token=new Token();
                    token.position=i;
                    token.value=Character.valueOf(c).toString();
                    token.type=TokenType.OPERATOR;
                    tokenList.add(token);

                    state=LexerStates.BEGIN;
                }else if(c==' '){
                    Token token=new Token();
                    token.position=i;
                    token.value=value.toString();
                    token.type=TokenType.OPERATOR;
                    tokenList.add(token);
                    state=LexerStates.SPACE;
                }else if(c==')'){
                    Token token=new Token();
                    token.position=i;
                    token.value=value.toString();
                    token.type=TokenType.VARIABLE;
                    tokenList.add(token);

                    token=new Token();
                    token.position=i;
                    token.value=Character.valueOf(c).toString();
                    token.type=TokenType.PARENTHESES;
                    tokenList.add(token);
                    state=LexerStates.SPACE;
                }else{
                    error(expr, i, "Invalid character: "+c);
                }
                break;
            }
        }
        //Now need to add the remaining upto the end

        switch(state){
        case NUMERIC:
            Token token=new Token();
            token.position=exprChars.length-1;
            token.value=value.toString();
            token.type=TokenType.NUMERIC;
            tokenList.add(token);
            break;
        case VARIABLE:
            token=new Token();
            token.position=exprChars.length-1;
            token.value=value.toString();
            token.type=TokenType.VARIABLE;
            tokenList.add(token);
            break;
        }


        return tokenList;
    }

    /*Parser starts here*/
    public static interface Symbol{
        public int getPosition();
    }

    public static interface NonTerminal extends Symbol{

    }

    public static class UnaryNonTerminal implements NonTerminal{
        public String operator;
        public Symbol operand;
        @Override
        public int getPosition() {
            // TODO Auto-generated method stub
            return operand.getPosition();
        }

        public String toString(){
            return operand.toString()+operator+":U ";
        }
    }

    public static class BinaryNonTerminal implements NonTerminal{
        public String operator;
        public Symbol operandLeft;
        public Symbol operandRight;
        @Override
        public int getPosition() {
            // TODO Auto-generated method stub
            return operandRight.getPosition();
        }

        public String toString(){
            return operandLeft.toString()+operandRight.toString()+operator+" ";
        }
    }

    public static class Terminal implements Symbol{
        public Token terminalToken;

        @Override
        public int getPosition() {
            // TODO Auto-generated method stub
            return terminalToken.position;
        }

        public String toString(){
            return terminalToken.value+" ";
        }
    }



    public static Symbol parse(List<Token> tokens, String expr) throws ExpressionException{
        Stack<Symbol> symbolStack=new Stack<Symbol>();
        WhetherLookAheadFailed failed=new WhetherLookAheadFailed();
        for(int i=0;i<tokens.size();i++){
            Token token=tokens.get(i);
            Terminal nextSymbol=null;
            if(i<tokens.size()-1){
                nextSymbol=new Terminal();
                nextSymbol.terminalToken=tokens.get(i+1);
            }
            Terminal terminal=new Terminal();
            terminal.terminalToken=token;

            symbolStack.push(terminal);
            while(doCheckBinary(symbolStack, nextSymbol, failed)
                    ||(!failed.lookAheadFail
                            &&(doCheckUnary(symbolStack)
                                    ||doCheckParen(symbolStack, expr))));

        }

        if(symbolStack.size()==1){
            return symbolStack.pop();
        }else if(symbolStack.size()>=1){
            Symbol prob=symbolStack.get(symbolStack.size()-1);
            error(expr, prob.getPosition(),"Parsing error");
        }
        return null;
    }

    public static class WhetherLookAheadFailed{
        public boolean lookAheadFail=false;
    }

    //Check wheather the top of the stack can be reduced to a binary non-terminal. If possible, do it
    public static boolean doCheckBinary(Stack<Symbol> symbolStack, Symbol nextSymbol, WhetherLookAheadFailed failed){
        failed.lookAheadFail=false;
        if(symbolStack.size()<3){
            return false;
        }
        Symbol rightSymbol=symbolStack.get(symbolStack.size()-1);
        Symbol leftSymbol=symbolStack.get(symbolStack.size()-3);
        Symbol midSymbol=symbolStack.get(symbolStack.size()-2);
        if(midSymbol instanceof Terminal){
            Terminal mid=(Terminal) midSymbol;
            if(mid.terminalToken.type==TokenType.OPERATOR){

                if((rightSymbol instanceof NonTerminal 
                        ||((Terminal)rightSymbol).terminalToken.type==TokenType.NUMERIC
                        ||((Terminal)rightSymbol).terminalToken.type==TokenType.VARIABLE)
                        &&(leftSymbol instanceof NonTerminal 
                                ||((Terminal)leftSymbol).terminalToken.type==TokenType.NUMERIC
                                ||((Terminal)leftSymbol).terminalToken.type==TokenType.VARIABLE)){

                    if("+".equals(mid.terminalToken.value)||"-".equals(mid.terminalToken.value)){
                        if(nextSymbol!=null&&nextSymbol instanceof Terminal){
                            if("*".equals(((Terminal)nextSymbol).terminalToken.value)
                                    ||"/".equals(((Terminal)nextSymbol).terminalToken.value)){

                                //Did a look ahead, no go here
                                failed.lookAheadFail=true;
                                return false;
                            }
                        }
                    }

                    symbolStack.pop();
                    symbolStack.pop();
                    symbolStack.pop();

                    BinaryNonTerminal binaryNonTerminal=new BinaryNonTerminal();
                    binaryNonTerminal.operandLeft=leftSymbol;
                    binaryNonTerminal.operandRight=rightSymbol;
                    binaryNonTerminal.operator=mid.terminalToken.value;
                    symbolStack.push(binaryNonTerminal);
                    return true;
                }
            }
        }
        return false;
    }

    //Check wheather the top of the stack can be reduced to a unary non-terminal
    public static boolean doCheckUnary(Stack<Symbol> symbolStack){
        if(symbolStack.size()<2){
            return false;
        }
        Symbol rightSymbol=symbolStack.get(symbolStack.size()-1);
        Symbol midSymbol=symbolStack.get(symbolStack.size()-2);

        if(midSymbol instanceof Terminal){
            Terminal mid=(Terminal) midSymbol;
            if(mid.terminalToken.type==TokenType.OPERATOR&&("+".equals(mid.terminalToken.value)
            		|| "-".equals(mid.terminalToken.value))){
                if((rightSymbol instanceof NonTerminal 
                        ||((Terminal)rightSymbol).terminalToken.type==TokenType.NUMERIC
                        ||((Terminal)rightSymbol).terminalToken.type==TokenType.VARIABLE)){

                    symbolStack.pop();
                    symbolStack.pop();

                    UnaryNonTerminal unaryNonTerminal=new UnaryNonTerminal();
                    unaryNonTerminal.operator=mid.terminalToken.value;
                    unaryNonTerminal.operand=rightSymbol;
                    symbolStack.push(unaryNonTerminal);
                    return true;
                }
            }
        }

        return false;
    }

    //Check wheather the top of the stack can be reduced to a parentheses non-terminal
    public static boolean doCheckParen(Stack<Symbol> symbolStack, String expr) throws ExpressionException{
        if(symbolStack.size()<3){
            return false;
        }
        Symbol rightSymbol=symbolStack.get(symbolStack.size()-1);
        Symbol leftSymbol=symbolStack.get(symbolStack.size()-3);
        Symbol midSymbol=symbolStack.get(symbolStack.size()-2);
        if(rightSymbol instanceof Terminal && leftSymbol instanceof Terminal){
            if(")".equals(((Terminal)rightSymbol).terminalToken.value) 
            		&& "(".equals(((Terminal)leftSymbol).terminalToken.value)){
                if((midSymbol instanceof NonTerminal 
                        ||((Terminal)midSymbol).terminalToken.type==TokenType.NUMERIC
                        ||((Terminal)midSymbol).terminalToken.type==TokenType.VARIABLE)){
                    symbolStack.pop();
                    symbolStack.pop();
                    symbolStack.pop();
                    symbolStack.push(midSymbol);
                    return true;
                }else{                
                    error(expr, ((Terminal)midSymbol).terminalToken.position, "Invalid Symbol");
                }
            }
        }
        return false;

    }

    /*Intermediate code generation. Simple post order traversal, no rocket science.*/
    public static List<Symbol> flatten(Symbol top){
        List<Symbol> symbols=new ArrayList<Symbol>();
        Stack<Symbol> depthStack=new Stack<Symbol>();
        depthStack.push(top);

        while(depthStack.size()>0){
            Symbol current=depthStack.pop();
            if(current instanceof Terminal){
                symbols.add(current);
            }else if(current instanceof BinaryNonTerminal){
                BinaryNonTerminal binaryNonTerminal=(BinaryNonTerminal) current;
                if(binaryNonTerminal.operandLeft!=null){
                    depthStack.push(binaryNonTerminal);
                    depthStack.push(binaryNonTerminal.operandRight);
                    depthStack.push(binaryNonTerminal.operandLeft);
                    binaryNonTerminal.operandRight=null;
                    binaryNonTerminal.operandLeft=null;
                }else{
                    symbols.add(current);
                }
            }else if(current instanceof UnaryNonTerminal){
                UnaryNonTerminal unaryNonTerminal=(UnaryNonTerminal) current;
                if(unaryNonTerminal.operand!=null){
                    depthStack.push(unaryNonTerminal);
                    depthStack.push(unaryNonTerminal.operand);
                    unaryNonTerminal.operand=null;
                }else{
                    symbols.add(current);
                }
            }
        }

        return symbols;
    }
    
    public static List<String> varList(List<Symbol>  symbols){
        List<String> varList=new ArrayList<String>();
        for(Symbol s:symbols){
            if(s instanceof Terminal && ((Terminal)s).terminalToken.type==TokenType.VARIABLE){
                if(!varList.contains( ((Terminal)s).terminalToken.value)){
                    varList.add(((Terminal)s).terminalToken.value);
                }
            }
        }
        return varList;
    }
    
    /*Optimization. Will eliminate all unary + and consecutive unary -*/
    public static List<Symbol> optimize(List<Symbol>  symbols){
        List<Symbol> returnList=new ArrayList<Symbol>();
        boolean gotUMinus=false;
        Symbol lastMinus=null;
        for(Symbol s:symbols){
            if(s instanceof UnaryNonTerminal){
                UnaryNonTerminal unaryNonTerminal=(UnaryNonTerminal) s;
                if("+".equals(unaryNonTerminal.operator)){
                    continue;//Why on earth do you write an unary plus anyway?
                }else{
                    lastMinus=s;
                    if(gotUMinus){
                        gotUMinus=false;
                        continue;
                    }else{
                        gotUMinus=true;
                    }
                }
            }else{
                if(gotUMinus){
                    returnList.add(lastMinus);
                    gotUMinus=false;
                }
                returnList.add(s);
            }
        }
        return returnList;
    }
    
    public static void generateCode(String className, List<String> varList, List<Symbol> source){

        //Create a ClassGen for our brand new class.
        ClassGen classGen=new ClassGen(className, "java.lang.Object",
        		className.substring(className.lastIndexOf('.')+1)+".exp", 
        		Constants.ACC_PUBLIC, null);
        
        //Get a reference to the constant pool of the class. This will be modified as we add methods, fields etc. Note that it already constains
        //a few constants.
        ConstantPoolGen constantPoolGen=classGen.getConstantPool();
        
        //The list of instructions for a method. 
        InstructionList instructionList=new InstructionList();
        
        instructionList.append(new GETSTATIC(constantPoolGen.addFieldref("java.lang.System", 
        		"out", "Ljava/io/PrintStream;")));
        
        for(Symbol s:source){
            if(s instanceof Terminal){
                Terminal terminal=(Terminal) s;
                if(terminal.terminalToken.type==TokenType.NUMERIC){
                    instructionList.append(new LDC2_W(constantPoolGen.addDouble(Double.parseDouble(terminal.terminalToken.value))));
                }else if(terminal.terminalToken.type==TokenType.VARIABLE){
                    int indexOfVariable=varList.indexOf(terminal.terminalToken.value);
                    instructionList.append(new ALOAD(0));//the argument of the main function
                    instructionList.append(new LDC(constantPoolGen.addInteger(indexOfVariable)));
                    instructionList.append(new AALOAD());//Got value of the variable as a String;
                    instructionList.append(new INVOKESTATIC(constantPoolGen.addMethodref("java.lang.Double", 
                    		"parseDouble", "(Ljava/lang/String;)D")));//Now we got the value as double
                }
            }else if(s instanceof UnaryNonTerminal){
                UnaryNonTerminal nonTerminal=(UnaryNonTerminal) s;
                if(nonTerminal.operator.equals("-")){//This is supposed to be the only unary operator avaiable
                    instructionList.append(new DNEG());
                }
            }else{
                BinaryNonTerminal nonTerminal=(BinaryNonTerminal) s;
                if(nonTerminal.operator.equals("+")){
                    instructionList.append(new DADD());
                }else if(nonTerminal.operator.equals("-")){
                    instructionList.append(new DSUB());
                }else if(nonTerminal.operator.equals("*")){
                    instructionList.append(new DMUL());
                }else if(nonTerminal.operator.equals("/")){
                    instructionList.append(new DDIV());
                }
            }
        }
        //Invoke println. we already have the object ref in the stack
        instructionList.append(new INVOKEVIRTUAL(constantPoolGen.addMethodref("java.io.PrintStream", "println", "(D)V")));
        
        //Return from the method
        instructionList.append(new RETURN());
        
        
        MethodGen methodGen=new MethodGen(Constants.ACC_PUBLIC|Constants.ACC_STATIC, 
        		Type.VOID, new Type[]{new ArrayType(Type.STRING, 1)}, new String[]{"args"}, 
        		"main", className, instructionList, constantPoolGen);
        
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
            javaClass.dump(className.replace('.', '/')+".class");
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        //That's it.
    }

    
}