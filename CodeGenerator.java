import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeGenerator {
    String outputContent="";
    List<String> mermaidCodeList = new ArrayList<>();
    public static void main(String[] args) {
        CodeGenerator codegenerator = new CodeGenerator();
        ClassNameTracker tracker = new ClassNameTracker();
        ClassMapper mapper = new ClassMapper();
        TypeDetector detector = new TypeDetector();
        Parser parser = new Parser();

        /*Initialize a Map with key(className) and its ArrayList */
        //read the file in and append on mermaidCodeList
        codegenerator.fileReader(args);
        //System.out.println(codegenerator.mermaidCodeList);
        //Find all the exist nameClass in mermaidCode and add on the "tracker's namelist"
        for(String line : codegenerator.mermaidCodeList){
           tracker.existClass(line);
        }
        //System.out.println(tracker.getNameList());
        //After we know what className exist, we create key with same value, and its empty arrayList on Map
        for(String className : tracker.getNameList()){
            //if not found the classname on the hashtable we create a new one
            if(!mapper.findClassList(className)){
                mapper.makeClassList(className);
            }
        }
        /*key(class name) -> its mapping(each line info)*/
        //Dertermine each-line is within which classes and store line info on the Map in classMapper
        //Each line has been already trimed in parser.fileReader
        //classNameForSingleLine is wrong
        for(String line : codegenerator.mermaidCodeList){
            String className = tracker.classNameForSingleLine(line);
            //System.out.println(line);
            // "" key does not need a map
            if(className.equals("")) continue;
            //store information
            mapper.setClassInfo(className, line);
        }
        //System.out.println(mapper.getClassInfoTable());
        /*Start to translate input codes to right java codes line by line, depending on each line's identity*/
        //Iterate all the Map key to find its own ArrayList
        for (Map.Entry<String, List<String>> entry : mapper.getClassInfoTable().entrySet()) {
            String className = entry.getKey();
            //Iterate each elements of an Arraylist which map a certain keys
            List<String> eachClassList = entry.getValue();
            codegenerator.contentWriter("public class " + className +" {\n");
            for(String line : eachClassList){
                if(detector.isVariable(line) == "VARIABLE"){
                    codegenerator.contentWriter(parser.variableTranslator(className, line));
                }
                else if(detector.isMethod(line) == "getMETHOD"){
                    codegenerator.contentWriter(parser.getMethodTranslator(className, line, "getMETHOD"));
                }
                else if(detector.isMethod(line) == "setMETHOD"){
                    codegenerator.contentWriter(parser.setMethodTranslator(className, line, "setMETHOD"));
                }
                else if(detector.isMethod(line) == "normMETHOD"){
                    codegenerator.contentWriter(parser.normMethodTranslator(className, line, "normMETHOD")); 
                }
            }
            codegenerator.contentWriter("}");
            codegenerator.fileWritter(className);
            //flush after a run of a class in order to save new content written in to new file
            codegenerator.contentPrinter();
            codegenerator.contentFlushing();       
        }
    }
    
    private void contentWriter(String content){
            outputContent += content;
    } 
    //flush the fileContent to empty
    private void contentFlushing(){
        outputContent = "";
    } 
    private void contentPrinter(){
        System.out.println(outputContent);
    }

    public void fileReader(String[] args){
        String filePath = "";
        //a variable ready to store codes
        String mermaidCode = "";

        if (args.length != 1) {
            System.err.println("Usage: java CodeGenerator <name> <path> ");
        }
        else{
            filePath = args[0];
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            
            //Read each line from the file
            while ((mermaidCode = reader.readLine()) != null) {
                // Skip processing empty lines
                if (mermaidCode.trim().isEmpty()||mermaidCode.equals("classDiagram")) {
                    continue;
                }
                mermaidCode = mermaidCode.trim();
                mermaidCodeList.add(mermaidCode);
                //System.out.println(mermaidCode);
            }
        } catch (IOException e) {
            System.err.println("Cannot read " + filePath);
            e.printStackTrace(); // Handle IOExceptio
        } 
    }

    public void fileWritter(String className) {
        try {
            File file = new File(className + ".java");
            if (!file.exists()) {
                file.createNewFile();
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write(outputContent);
            }
            //System.out.println("Java class has been generated: " + className + ".Java");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


/*dealing 4 things: 
 *1 Check the existence of the given className for its mapping
 *2 Establish "new" mapping STRING_NAME -> itsList
 *3 Set the value on the list via mapping(if the mapping exist)
 *4 Get the specific value(type:list)  via mapping
*/
class ClassMapper{
    Map<String, List<String>> Map = new HashMap<>();
    public boolean findClassList(String className){
        if(Map.containsKey(className)){
            return true;//if found, we set value
        }
        else return false;//if not found, we create new mapping
    }
    public void makeClassList(String className){
        List<String> singleClassInfoList = new ArrayList<>();
        Map.put(className, singleClassInfoList);
    }
    public void setClassInfo(String className, String memberInfo){
        Map.get(className).add(memberInfo);
    }
    public Map<String, List<String>> getClassInfoTable(){
        return Map;
    }
}

class ClassNameTracker{
    public List<String> classNameList = new ArrayList<>();
    private String previousClassName = "";
    //Variable status to keep track of { and }
    private String status = "OUT_OF_BRACE";

    //determine the existence of a className
    public void existClass(String line){
        String className = "";
        String regex = "\\s*(class)\\s+(\\w+)\\s*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);
        if(matcher.find()){
            className = matcher.group(2);
        }
        //if the classname is not empty, we store the name
        if(!className.isEmpty())
            classNameList.add(className);
    }
    public List<String> getNameList(){
        return classNameList;
    }

    public String classNameForSingleLine(String line){
        String currentClassName = "";
        //extract the class name from the line
        if(line.contains(":")){
            String withOutBraceRegex = "(\\w+)\\s*(\\:)\\s*";
            Pattern withOutBracePattern = Pattern.compile(withOutBraceRegex);
            Matcher withOutBraMatcher = withOutBracePattern.matcher(line);

            if(withOutBraMatcher.find()){
                currentClassName = withOutBraMatcher.group(1);
                return currentClassName;
            }
        }
        else if(line.contains("{")){
            String braceRegex = "(\\w+)\\s+(\\w+)\\s*\\{";
            Pattern bracPattern = Pattern.compile(braceRegex);
            Matcher braceMatcher = bracPattern.matcher(line);
            
            //It will Update the value if we found another class special case 
            if(braceMatcher.find()){
                this.previousClassName = braceMatcher.group(2);
            }
            status = "IN_THE_BRACE";
            return this.previousClassName;
        }

        else if(line.equals("}")){
            status = "OUT_OF_BRACE";
            return "";
        }
        else if(status.equals("IN_THE_BRACE")){
            return this.previousClassName;
        }
        return "";
    }
}

class TypeDetector{
    public String isClass(String line){
        String identity = "";
        String regex = "class\\s{1}\\w+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);
        if(matcher.find()){
            identity = "CLASS";
        }
        else {;}
        return identity;
    }
    public String isVariable(String line){
        String identity = "";
        if(line.contains("+")||line.contains("-")){
            if(!(line.contains("{")||line.contains("("))){
                identity = "VARIABLE";
            }
            else {
                identity = "";
            };
        }
        else identity = "";
        //System.out.println(identity);
        return identity;
    }
    public String isMethod(String line){
        String identity = "";
        if (line.contains("(")) {
            String getterRegex = "\\s*[\\+\\-]\\s*(get){1}[A-Z]{1}";
            String setterRegex = "\\s*([\\+\\-])\\s*(set)([A-Z]{1}\\w+)\\s*\\(([^)]*)\\)\\s*";
            Pattern getPattern = Pattern.compile(getterRegex);
            Pattern setPattern = Pattern.compile(setterRegex);
            Matcher getMatcher = getPattern.matcher(line);
            Matcher setMatcher = setPattern.matcher(line);

            if(getMatcher.find()){
                identity = "getMETHOD";
            }
            else if(setMatcher.find())
                //If no parameter in argument of setter. like  +setName() +setName(   )
                if(setMatcher.group(4).matches("\\s*")){
                    identity = "normMETHOD";
                }
                else{
                    identity = "setMETHOD";
                }
            else
                identity = "normMETHOD";
        }
        else identity = "";
        //System.out.println(identity);
        return identity;
    }
}

class Parser {
    List<String> linesList = new ArrayList<>();
    List<String> oneLinesList = new ArrayList<>();

    public String variableTranslator(String className, String oneLineClassInfo){
        String content = "";
        String regex = "\\s*([\\+\\-])\\s*(\\w+\\s*\\[?\\s*\\]?)\\s*(\\w+)";
        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(oneLineClassInfo);
        if(matcher.find()){
            String attribute = matcher.group(1).equals("+") ? "public" : "private";
            String variableType = matcher.group(2);
            String variableName = matcher.group(3);

        //Consider space in variableType
        variableType = variableType.trim();
        //Consider []
        if(variableType.contains("[")){
            variableType = variableType.replaceAll("\\s*\\[\\s*", "[");
        }
        //System.out.println(variableType);
        content += "    " + attribute + " " + variableType 
                    + " " + variableName + ";\n";
        } 
        //System.out.println(content);
        return content;
    }
    public String getMethodTranslator(String className, String oneLineClassInfo, String identity){
        String content = "";
        //Since get method would have a word behind ), we use \w+, at least occur once
        String getMethodRegex = "\\s*([\\+\\-])\\s*(get)(\\w+)\\s*\\(([^\\)]*)\\)\\s*(\\w+\\s*\\[?\\s*\\]?)?";
        Pattern getMethodPattern = Pattern.compile(getMethodRegex);
        Matcher getMethodMatcher = getMethodPattern.matcher(oneLineClassInfo);
        if(getMethodMatcher.find()){
            //If line contains +, then the attribute is public
            String attribute = getMethodMatcher.group(1).equals("+") ? "public" : "private";
            String methodName = getMethodMatcher.group(2) + getMethodMatcher.group(3);
            String returnName = getMethodMatcher.group(3);
            String returnType = getMethodMatcher.group(5);

            //Consider CamelCase getOwnerHarrisSu -> return ownerHarrisSu
            returnName = returnName.substring(0, 1).toLowerCase() 
                        + returnName.substring(1);
            
            content = "    " + attribute + " " + returnType + " " + methodName + "()"
                    + " " + "{" + "\n" + "        " + "return" + " " + returnName +
                    ";" + "\n" + "    " + "}" + "\n";
        }
        else{
            System.err.println("Fail to print getMETHOD");
        }
        return content;
    }
    public String setMethodTranslator(String className, String oneLineClassInfo, String identity){
        String content = "";
        String setMethodRegex = "\\s*([\\+\\-])\\s*(set)(\\w+)\\s*\\(([^)]*)\\)\\s*";
                Pattern setMethodPattern = Pattern.compile(setMethodRegex);
                Matcher setMethodMatcher = setMethodPattern.matcher(oneLineClassInfo);
                if(setMethodMatcher.find()){
                    String attribute = setMethodMatcher.group(1).equals("+") ? "public" : "private";
                    String methodName = setMethodMatcher.group(2) + setMethodMatcher.group(3);
                    String returnType = "void";
                    String orgVariable = setMethodMatcher.group(3);
                    String thisVariable = orgVariable.substring(0,1).toLowerCase()
                                            + orgVariable.substring(1);
                    String argumentName = setMethodMatcher.group(4);
                    //+ setOwner (  String hi  ) -> (String hi)
                    argumentName = argumentName.trim();
                    argumentName = argumentName.replaceAll("\\s+", " ");
                    //System.out.println(argumentName);
                    //grab the actual argumentname String name4 -> name4
                    String insideVariable = argumentName.replaceFirst("\\w+\\s", "");
                    //System.out.println(insideVariable);
                    content = "    " + attribute + " " + returnType + " " + methodName + "(" +argumentName + ")"
                            + " " + "{" + "\n" + "        " + "this." + thisVariable +
                            " " + "=" + " " + insideVariable + ";" + "\n" + "    " + "}" + "\n";
                }
                else{
                    System.err.println("Fail to print setMethod");
                }
        return content;
    }
    public String normMethodTranslator(String className, String oneLineClassInfo, String identity){
        String content = "";
        //Consider many random space or [] occur
        String normMethodRegex = "\\s*([\\+\\-])\\s*(\\w+)\\s*\\(([^\\)]*)\\)\\s*(\\w+)*";
        Pattern normMethodPattern = Pattern.compile(normMethodRegex);
        
        //System.out.println(oneLineClassInfo);
        Matcher normMethodMatcher = normMethodPattern.matcher(oneLineClassInfo);
        if(normMethodMatcher.find()){
            //Do not know why group count not work
            //int groupCount = normMethodMatcher.groupCount();
            String attribute = normMethodMatcher.group(1).equals("+") ? "public" : "private";
            String methodName = normMethodMatcher.group(2);  
            
            String arguments = "";
            //if match only three group + taker5(),+,taker5
            if(normMethodMatcher.group(3)==null){;
            }
            else{
                arguments = normMethodMatcher.group(3);
            }
            
            //if arguments is () no space
            if(arguments.equals("")){;
            }
            //if arguments is ( ) , one space
            else if(arguments.equals(" ")){
                arguments = arguments.replaceFirst("\\s", "");
            }
            else{
                arguments = arguments.trim();
                arguments = arguments.replaceAll("\\s+", " ");
            }
            
            //Turn [], [ ] one space,to be []
            if(arguments.contains("[")){
                arguments = arguments.replaceAll("\\s*\\[\\s*\\]", "[]");
            }
            
            //Consider comma
            if(arguments.contains(",")){
                arguments = arguments.replaceAll("\\s+,\\s+", ", ");
            }

            //Consider return type
            /*And the returnType we must assigned mannually, for matcher is out of the bound
            *Index 0 ~ index4, so the number of group would be 5
            */
            String returnType = "";
            if(normMethodMatcher.group(4)==null){
                returnType = "void";
            }
            else{
                returnType = normMethodMatcher.group(4).trim();
            }
            
            //System.out.println("return"+returnType);
            //Consider void, boolean, int, String method to return its default value
            String returnDefault = "{;}";
            
            if(returnType.equals("void")){;
            }
            //Default return values for boolean,string,int
            else if(returnType.equals("boolean")){
                returnDefault = "{return false;}";
            }
            else if(returnType.equals("int")){
                returnDefault = "{return 0;}";
            }
            else if (returnType.equals("String")){
                returnDefault = "{return \"\";}";
            }
                    
            content += "    " + attribute + " " + returnType +" " + methodName 
                     + "(" + arguments + ")" + " " + returnDefault + "\n";
        }
        else{
            System.err.println("Fail to print normMETHOD");
        }
        return content;
    }
}