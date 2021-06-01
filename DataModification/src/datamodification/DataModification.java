package datamodification;
import javax.swing.JFileChooser;
import java.io.*;   
import java.util.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.json.JSONArray;
import org.json.JSONObject;

public class DataModification {
    public static  File file;//выбранный пользователем файл
    public static  ArrayList <JSONArray> list = new ArrayList<JSONArray>();//хранит все функции (body) в файле
    public static  String funcName = "";//название процедуры/функции, введенное пользователем
    public static  Integer funcLine = 0;//номер строки процедуры/функции, введенное пользователем
    public static  ArrayList<FuncInfo> listNameOfFunc = new ArrayList<FuncInfo>();//хранит названия всех функций и номера строк 
                                                                                  //в файле    
    public static  ArrayList<JSONArray> listOfInsideFunc = new ArrayList<JSONArray>();//хранит все внутренние функции 
    public static  ArrayList<JSONArray> listOfGenElem = new ArrayList<JSONArray>();//хранит general_elements
    public static  ArrayList <FuncCallInfo> NameOfInsideFunc = new ArrayList <FuncCallInfo>();//хранит названия, строки вложенных функций
    public static  ArrayList<String> filesName = new ArrayList<String>();//хранит название файлов, на которые ссылаются функции
    public static  ArrayList<JSONArray> listOfFuncForResult = new ArrayList<JSONArray>();//хранит функции для ответа       
    public static  ArrayList<Structure> resultList = new ArrayList<Structure>();//хранит ответ     
    public static  ArrayList<String> arrOfStr = new ArrayList<String>();//хранит название таблиц
    public static void main(String[] args) throws IOException {
       //выбор файла
        FileSelection();
        System.out.println("Выбранный файл Json: " + file.getName());        
        //чтение из файла построчно и построение json для передачи в jsonObject
        String json = "";           
        try {
            FileReader read = new FileReader(file.getAbsoluteFile());
            BufferedReader BR = new BufferedReader(read);
            String l = BR.readLine();     
            while (l != null) {
                json = json + l;
                l = BR.readLine();   
            }
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }        
        //создание массива из json файла
        JSONArray arr = new JSONArray();
        JSONObject obj = new JSONObject();
        try {
            obj = new JSONObject(json);
            arr = obj.getJSONArray("parserRule");
        } 
        catch (NullPointerException ex) {
            ex.printStackTrace();
        }        
        //обход по массиву и получение всех функций
        getJSONObjectFunc(arr);    
        if(!list.isEmpty())//если в файле есть функции и процедуры
        {
            //получение названий функций
            GetNameAndLineFunc(list,listNameOfFunc);
            System.out.println("Названия всех функций: ");
            PrintFuncInfo(listNameOfFunc);
        
            //ввод названия и строки процедуры/функции
            Scanner in = new Scanner(System.in);
            System.out.print("Введите название функции/процедуры: ");
            funcName = in.nextLine();            
            System.out.print("Введите номер строки функции/процедуры: ");
            funcLine = in.nextInt();
            
            //существует ли функция по запросу пользователя
            boolean ok = false; Integer ind = 0;
            for(int i = 0; i<listNameOfFunc.size(); i++){
                if((listNameOfFunc.get(i).FuncName.equals(funcName))&&(listNameOfFunc.get(i).FuncLine.equals(funcLine))){
                    ok = true;
                    ind = i;
                }
            }
            if(ok) {
                System.out.println("Выбранная функция: " + funcName + ",  Номер строки: " + funcLine);
                JSONArray fArr = list.get(ind);
                System.out.println("Будем работать с этим массивом: " + fArr);
                
                //ищем func_call или proc_call
                getJSONObjectFuncCall(fArr);
                //ищем general_element
                getJSONObjectGeneralElem(fArr);
                
                //добавляем название и строку вложенных функций по func_call, proc_call
                if(!listOfInsideFunc.isEmpty()){
                    System.out.println("Вложенные функции, полученные по ключам function_call, procedure_call, найдены!"); 
                    //идем по массиву с вложенными функциями, запоминаем название и строку
                    GetNameAndLineInsideFunc(listOfInsideFunc, NameOfInsideFunc);                     
                    System.out.println("Названия вложенных функций: ");
                    for(FuncCallInfo a: NameOfInsideFunc){
                        System.out.println(a);
                    }
                    //ищем body
                    for(FuncCallInfo a: NameOfInsideFunc){
                        if("null".equals(a.FuncName2)){
                            //то ищем body в этом файле
                            for(FuncInfo i: listNameOfFunc) {
                                if((a.FuncName1==i.FuncName) && (Objects.equals(a.FuncLine, i.FuncLine))) {
                                    System.out.println("Тело вложенной функции " + i + " найдено в этом же файле");
                                    //добавляем функцию/процедуру в список для будующей проверки на работу с данными                                
                                    for(JSONArray i2: listOfInsideFunc) { 
                                        FuncInfo f2 = new FuncInfo();
                                        try{
                                            f2.FuncName = i2.getJSONObject(0).getJSONArray("routine_name").getJSONObject(0).getJSONArray("identifier")
                                                            .getJSONObject(0).getJSONArray("id_expression").getJSONObject(0).
                                                                getJSONArray("regular_id").getJSONObject(0).getString("text");
                                            f2.FuncLine = i2.getJSONObject(0).getJSONArray("routine_name").getJSONObject(0).getJSONArray("identifier")
                                                            .getJSONObject(0).getJSONArray("id_expression").getJSONObject(0).
                                                                getJSONArray("regular_id").getJSONObject(0).getInt("line");
                                            if((Objects.equals(f2.FuncLine, a.FuncLine))&&(f2.FuncName==a.FuncName1)){
                                                listOfFuncForResult.add(i2);
                                                System.out.println("Добавили элемент в итоговый лист!");
                                            }  
                                        }
                                        catch(Exception ex){ }                                   
                                    }
                                }
                            }
                        }
                        else {
                            //ищем json файлы
                            boolean ok2 = false;                    
                            File folder = new File("jsonFiles");
                            File[] listOfFiles = folder.listFiles();
                            for (File file_: listOfFiles) {
                                String s = file_.getName();
                                s = s.substring(0, (s.length()-5));
                                if ((file_.isFile()) && (s.equals(a.FuncName1))) {
                                    //System.out.println("Функция ссылается на json файл " + file_.getName());
                                    ok2 = true;
                                    //запоминаем в список название файла
                                    if(!filesName.contains(a.FuncName1+".json")) {
                                        filesName.add(a.FuncName1+".json");
                                    }
                                }
                            }  
                    
                        }
                    }
                    if(listOfFuncForResult.isEmpty()){
                        listOfFuncForResult.add(fArr);
                    }
                    if(!filesName.isEmpty()){
                        System.out.println("Вывод файлов, на которые ссылаются: ");
                        for(String as:filesName){
                            System.out.println(as);
                        }
                        for(int i = 0; i<filesName.size(); i++){
                            String fn = filesName.get(i);
                            Base(fn);
                        }
                    } 
                }
                else{
                     System.out.println("function_call/procedure_call не найдены в этом файле!"); 
                }
                //добавляем в список вложенных функций по general_element
                if(!listOfGenElem.isEmpty()){
                    System.out.println("general_element найдены!");
                    //PrintJsonArray(listOfGenElem);
                    for (JSONArray a: listOfGenElem){
                        try{
                            String s = a.getJSONObject(0).getJSONArray("general_element_part").getJSONObject(0).getJSONArray
                                        ("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text");
                            for(FuncInfo f: listNameOfFunc){                                
                                if(s.equals(f.FuncName)){
                                    FuncCallInfo f3 = new FuncCallInfo();
                                    f3.FuncLine = f.FuncLine;
                                    f3.FuncName1 = f.FuncName;
                                    NameOfInsideFunc.add(f3);                                    
                                    list.forEach((i) -> { 
                                        FuncInfo f2 = new FuncInfo();
                                        f2.FuncName = i.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                                             getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text");
                                        f2.FuncLine = i.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                                        getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getInt("line");
                                        if((Objects.equals(f2.FuncLine, f.FuncLine))&&(f2.FuncName==f.FuncName)){
                                            listOfInsideFunc.add(i);
                                        }                                     
                                    });
                                }
                            }
                        }
                        catch(Exception ex){}
                    }                    
                    //если вложенные функции или процедуры есть, то получаем эти функции
                    if(!NameOfInsideFunc.isEmpty()){
                        //проверяем есть ли вложенные функции в этом же файле
                        for(FuncInfo i: listNameOfFunc) {
                            for (FuncCallInfo j: NameOfInsideFunc){
                                if((Objects.equals(i.FuncLine, j.FuncLine)) && (i.FuncName.equals(j.FuncName1))) {
                                    System.out.println("Body вложенной функции " + j + " найдено в этом же файле");
                                    //добавляем функцию/процедуру в список для будующей проверки на работу с данными                                
                                    for(JSONArray i2: listOfInsideFunc) { 
                                        FuncInfo f2 = new FuncInfo();
                                        try{
                                            f2.FuncName = i2.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                                                           getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text");
                                            f2.FuncLine = i2.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                                                                getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getInt("line");
                                            if((Objects.equals(f2.FuncLine, j.FuncLine))&&(f2.FuncName==j.FuncName1)){                                                
                                                listOfFuncForResult.add(i2);
                                            }  
                                        }
                                        catch (Exception e){  }                                                                      
                                    }
                                }
                            }
                        }
                    }
                    else {
                        System.out.println("Вложенные функции, полученные по ключу general_element, не найдены в этом файле!"); 
                    }
                }
                else {
                    System.out.println("geteral_element не найдены в этом файле!"); 
                }
                if(listOfFuncForResult.isEmpty()) {
                    System.out.println("Отправляем главную функцию на проверку!");
                    //проверяем тогда главную функцию на работу с данными/таблицами
                    getResult(fArr); //поиск операций с данными/таблицами  
                    if(!resultList.isEmpty()) {
                        resultList.forEach((i) -> {  
                            i.FuncName = funcName;
                            i.StrLine = funcLine;
                            System.out.println(i.toString());
                        });
                    }
                    else {
                        System.out.println("В выбранной процедуре/функции нет работы с таблицами/данными!"); 
                    }
                }
                else {
                    System.out.println();
                    for(JSONArray fb: listOfFuncForResult) {
                        getResult(fb); //поиск операций с данными/таблицами  
                        if(!resultList.isEmpty()) {
                            resultList.forEach((i) -> {  
                                i.FuncName = fb.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                                            getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text");
                                i.StrLine = fb.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                                            getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getInt("line");;
                                System.out.println(i.toString());
                            });
                        }                            
                        else {
                            //System.out.println("В выбранной процедуре/функции нет работы с таблицами/данными!"); 
                        }
                        resultList = new ArrayList<Structure>();
                    }
                }
            }
            else{
                System.out.println("Функция " + funcName + " отсутствует в выбранном файле!"); 
            }
        }
        else{
            System.out.println("В данном файле нет функций и процедур!"); 
        }
    }
    //если найдены вложенные функции в другом файле
    public static void Base(String nameFile) throws IOException{
        System.out.println("Проверка файла!");
        //чтение из файла построчно и построение json для передачи в jsonObject
        String json = "";           
        try {
            FileReader read = new FileReader("jsonFiles\\" + nameFile);
            BufferedReader BR = new BufferedReader(read);
            String l = BR.readLine(); 
            while (l != null) {
                json = json + l;
                l = BR.readLine();
            }
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        //создание массива из json файла
        JSONArray arr = new JSONArray();
        JSONObject obj = new JSONObject();
        try {
            obj = new JSONObject(json);
            arr = obj.getJSONArray("parserRule");
            //System.out.println(arr);
        } 
        catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        Integer elemCount = list.size();
        //обход по массиву и получение всех функций
        getJSONObjectFunc(arr);    
        if(!list.isEmpty())//если в файле есть функции и процедуры
        {
//            System.out.println("Вывод всех функций/процедур: ");
//            PrintJsonArray(list);
            JSONArray fArr = new JSONArray();
            for(int w = elemCount; w < list.size();w++) { 
                JSONArray i = list.get(w);
                String s = i.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                        getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text");                
                for(int q = 0; q < NameOfInsideFunc.size();q++){
                    FuncCallInfo f = NameOfInsideFunc.get(q);
                    if(f.FuncName2.equals(s)){
                        if(!i.equals(fArr)){
                            fArr = new JSONArray();
                            fArr = i;
                        if(fArr.length() != 0) {                
                            System.out.println("Будем работать с этим массивом: " + fArr);
                            Integer count = listOfInsideFunc.size();                
                            //ищем func_call или proc_call
                            getJSONObjectFuncCall(fArr);    
                            //ищем general_element
                            getJSONObjectGeneralElem(fArr);
                            //добавляем название и строку вложенных функций по func_call, proc_call
                            if(listOfInsideFunc.size()!=count){
                                System.out.println("Вложенные функции найдены!"); 
//                                System.out.println("Вложенные функции: ");
//                                for(int i5 = count; i5<listOfInsideFunc.size(); i5++){
//                                    System.out.println(listOfInsideFunc.get(i5));
//                                }
                                //идем по массиву с вложенными функциями, запоминаем название и строку
                                for(int j = count; j<listOfInsideFunc.size(); j++){    
                                    JSONArray ifunc = listOfInsideFunc.get(j);
                                    FuncCallInfo fCall = new FuncCallInfo(); 
                                    fCall.FuncName1 = ifunc.getJSONObject(0).getJSONArray("routine_name").getJSONObject(0).getJSONArray("identifier")
                                        .getJSONObject(0).getJSONArray("id_expression").getJSONObject(0).
                                        getJSONArray("regular_id").getJSONObject(0).getString("text");
                                    fCall.FuncLine = ifunc.getJSONObject(0).getJSONArray("routine_name").getJSONObject(0).getJSONArray("identifier")
                                        .getJSONObject(0).getJSONArray("id_expression").getJSONObject(0).
                                        getJSONArray("regular_id").getJSONObject(0).getInt("line");
                                    try{
                                        String a2 = ifunc.getJSONObject(0).getJSONArray("routine_name").getJSONObject(1).getString("text");                
                                        if(".".equals(a2)){
                                            fCall.FuncName2 = ifunc.getJSONObject(0).getJSONArray("routine_name").getJSONObject(2).getJSONArray("id_expression").
                                                getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text");                    
                                            //System.out.println(f);
                                        }   
                                    }
                                    catch(Exception ex){}
//                                    for(int j2 = count; j2<NameOfInsideFunc.size();j2++){
//                                        FuncCallInfo c = NameOfInsideFunc.get(j2);
//                                        if(!NameOfInsideFunc.contains(j2)){
//                                            NameOfInsideFunc.add(fCall);
//                                        }
//                                    }
                                    NameOfInsideFunc.add(fCall);
                                }                  
                                System.out.println("Названия вложенных функций: ");
                                for(int j = count; j<NameOfInsideFunc.size(); j++){
                                    FuncCallInfo a = NameOfInsideFunc.get(j);
                                    System.out.println(a);
                                }
                                //Integer countFiles = filesName.size();
                                Integer countRes = listOfFuncForResult.size();
                                //ищем body
                                for(int j = count; j<NameOfInsideFunc.size(); j++){
                                    FuncCallInfo a = NameOfInsideFunc.get(j);
                                    if(a.FuncName2=="null"){
                                    //то ищем body в этом файле
                                        for(int ik = count; ik<list.size(); ik++){
                                            JSONArray i2 = list.get(ik);
                                            String o1 = i2.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                                                getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0)
                                                    .getString("text");
                                            Integer o2 = i2.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                                                getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0)
                                                    .getInt("line");
                                            if((a.FuncName1==o1) && (Objects.equals(a.FuncLine, o2))) {
                                                System.out.println("Тело вложенной функции " + ik + " найдено в этом же файле");
                                                //добавляем функцию/процедуру в список для будующей проверки на работу с данными 
                                                for(JSONArray arrInFunc: listOfInsideFunc) { 
                                                    FuncInfo f2 = new FuncInfo();
                                                    try{//если из func_call
                                                        f2.FuncName = arrInFunc.getJSONObject(0).getJSONArray("routine_name").getJSONObject(0).getJSONArray("identifier")
                                                                        .getJSONObject(0).getJSONArray("id_expression").getJSONObject(0).
                                                                            getJSONArray("regular_id").getJSONObject(0).getString("text");
                                                        f2.FuncLine = arrInFunc.getJSONObject(0).getJSONArray("routine_name").getJSONObject(0).getJSONArray("identifier")
                                                                        .getJSONObject(0).getJSONArray("id_expression").getJSONObject(0).
                                                                            getJSONArray("regular_id").getJSONObject(0).getInt("line");
                                                        if((Objects.equals(f2.FuncLine, a.FuncLine))&&(f2.FuncName==a.FuncName1)){
                                                            listOfFuncForResult.add(arrInFunc);
                                                        }  
                                                    }
                                                    catch(Exception ex){ }                                   
                                                }
                                            }
                                        }
                                    }
                                    else {
                                        //ищем json файлы
                                        boolean ok2 = false;                    
                                        File folder = new File("jsonFiles");
                                        File[] listOfFiles = folder.listFiles();
                                        for (File file_: listOfFiles) {
                                            String s2 = file_.getName();
                                            s2 = s2.substring(0, (s2.length()-5));
                                            if ((file_.isFile()) && (s2.equals(a.FuncName1))) {
                                                //System.out.println("Функция ссылается на json файл " + file_.getName());
                                                ok2 = true;
                                                //запоминаем в список название файла
                                                if(!filesName.contains(a.FuncName1+".json")) {
                                                    filesName.add(a.FuncName1+".json");
                                                }
                                                //filesName.add(a.FuncName1+".json");
                                            }
                                        } 
                                    }
                                }                   
                                if(listOfFuncForResult.size() == countRes){
                                    System.out.println("Функция, на которую ссылаются в файле, найдена!");
                                    System.out.println("Тела вложенных в нее функции не найдены!");
                                    System.out.println("Тогда добавляем саму функцию!");                    
                                    listOfFuncForResult.add(fArr);
            //                        for(JSONArray a:listOfFuncForResult){
            //                            System.out.println(a);
            //                        }
                                }
                            }
                            else{
                                System.out.println("Функция, на которую ссылаются в файле, найдена!");
                                System.out.println("Вложенные в нее функции не найдены!");
                                System.out.println("Тогда добавляем саму функцию!");                    
                                listOfFuncForResult.add(fArr);
                            }
                            Integer countOfInside = NameOfInsideFunc.size();
                            //добавляем в список вложенных функций по general_element
                            if(!listOfGenElem.isEmpty()){
                                //System.out.println("general_element найдены!");
                                for (JSONArray a: listOfGenElem){
                                    try{
                                        String s2 = a.getJSONObject(0).getJSONArray("general_element_part").getJSONObject(0).getJSONArray
                                                    ("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text");
                                        for(int i3 = elemCount; i3<list.size(); i3++){
                                            JSONArray ja = list.get(i3);
                                            String s3 = ja.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                                                getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text");
                                            if(s2.equals(s3)){
                                                FuncCallInfo f3 = new FuncCallInfo();
                                                f3.FuncLine = ja.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                                                getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getInt("line");
                                                f3.FuncName1 = s3;
                                                NameOfInsideFunc.add(f3); 
                                                listOfInsideFunc.add(ja);
                                            }
                                        }
                                    }
                                    catch(Exception ex){}
                                }                    
                                //если вложенные функции или процедуры есть, то получаем эти функции
                                if(NameOfInsideFunc.size() != countOfInside){
                                    //проверяем есть ли вложенные функции в этом же файле
                                    for (int y = elemCount; y<list.size(); y++){
                                        JSONArray jsarr = list.get(y);
                                        String s3 = jsarr.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                                                getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text");;
                                        Integer s4 = jsarr.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                                                getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getInt("line");
                                        for (int a = countOfInside; a<NameOfInsideFunc.size(); a++){
                                            FuncCallInfo j = NameOfInsideFunc.get(a);
                                            if((Objects.equals(s4, j.FuncLine)) && (s3.equals(j.FuncName1))) {
                                                //System.out.println("Body вложенной функции " + j + " найдено в этом же файле");
                                                //добавляем функцию/процедуру в список для будующей проверки на работу с данными
                                                for (int r = countOfInside; r<listOfInsideFunc.size(); r++){
                                                    JSONArray i2 = listOfInsideFunc.get(r);
                                                    FuncInfo f2 = new FuncInfo();
                                                    try{
                                                        f2.FuncName = i2.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                                                                       getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text");
                                                        f2.FuncLine = i2.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                                                                            getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getInt("line");
                                                        if((Objects.equals(f2.FuncLine, j.FuncLine))&&(f2.FuncName==j.FuncName1)){                                                
                                                            listOfFuncForResult.add(i2);
                                                        }  
                                                    }
                                                    catch (Exception e){  }                                                                      
                                                }
                                            }
                                        }
                                    }
                                }
//                                else {
//                                    System.out.println("Вложенные функции, полученные по ключу general_element, не найдены в этом файле!"); 
//                                }
                            }
                        }
                        }
                    }
                }
            }
        }
    }
    
    //Вывод списка с json массивами
    public static void PrintJsonArray(ArrayList<JSONArray> a){
         for(JSONArray i: a){
             System.out.println(i);
         }
     }
     
    //Вывод списка названий и строк
    public static void PrintFuncInfo(ArrayList<FuncInfo> a){
         for(FuncInfo i: a){
             System.out.println(i);
         }
     }
     
    //Получение названия и строки из всех функций файла для выбора пользователем
    public static void GetNameAndLineFunc(ArrayList<JSONArray> a, ArrayList<FuncInfo> b){
         a.forEach((i) -> { 
                FuncInfo f = new FuncInfo();
                f.FuncName = i.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                        getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text");
                f.FuncLine = i.getJSONObject(1).getJSONArray("identifier").getJSONObject(0).
                        getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getInt("line");
                b.add(f);
            });
     }
     
    //Получение названия и строки из внутренних функций
    public static void GetNameAndLineInsideFunc(ArrayList<JSONArray> a, ArrayList<FuncCallInfo> b){
        a.forEach((i) -> {             
            FuncCallInfo f = new FuncCallInfo(); 
            f.FuncName1 = i.getJSONObject(0).getJSONArray("routine_name").getJSONObject(0).getJSONArray("identifier")
                            .getJSONObject(0).getJSONArray("id_expression").getJSONObject(0).
                            getJSONArray("regular_id").getJSONObject(0).getString("text");
            f.FuncLine = i.getJSONObject(0).getJSONArray("routine_name").getJSONObject(0).getJSONArray("identifier")
                            .getJSONObject(0).getJSONArray("id_expression").getJSONObject(0).
                            getJSONArray("regular_id").getJSONObject(0).getInt("line");
            try{
                String a2 = i.getJSONObject(0).getJSONArray("routine_name").getJSONObject(1).getString("text");                
                if(".".equals(a2)){
                    f.FuncName2 = i.getJSONObject(0).getJSONArray("routine_name").getJSONObject(2).getJSONArray("id_expression").
                            getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text"); 
                }
            }
            catch(Exception ex){
                
            }
//            for(int j = 0; j<b.size();j++){
//                FuncCallInfo c = b.get(j);
//                if(!b.contains(j)){
//                    b.add(f);
//                }
//            }
            b.add(f);
        });
     }
    
    //Выбор файла
    public static void FileSelection() {
        JFileChooser fileChoose = new JFileChooser(); 
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Json files", "json");
        
        fileChoose.setFileFilter(filter);
        int ret = fileChoose.showDialog(null, "Выберите файл");                
        try {
            ret = JFileChooser.APPROVE_OPTION;
            file = fileChoose.getSelectedFile();            
        }
        catch (Exception ex)
        {
            System.out.print("Ошибка при выборе файла");
            System.exit(0);
        }
    }
    
    //Получение всех функций по ключу - function_body, procedure_body
    private static void getJSONObjectFunc(JSONArray jsonArray) {    
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject tmpObj = jsonArray.getJSONObject(i);        
            Iterator key = tmpObj.keys();
            while (key.hasNext()) {
                String k = key.next().toString();
                //System.out.println("Key : " + k);
                try{
                    JSONArray arr3 = tmpObj.getJSONArray(k);
                    getJSONObjectFunc(arr3); 
                    if(("function_body".equals(k)) || ("procedure_body".equals(k))){
                        list.add(arr3);
                    }
                }
                catch(Exception ex){
                    if("line".equals(k)){                            
                        //System.out.println(tmpObj.getInt(k));
                    }
                    if("text".equals(k)){                            
                        //System.out.println(tmpObj.getString(k));
                    }
                    if("type".equals(k)){                            
                        //System.out.println(tmpObj.getInt(k));
                    }
                }
            }   
        }
    }
   
    //Получение всех вызовов функции по ключу - function_call, procedure_call
    private static void getJSONObjectFuncCall(JSONArray jsonArray) {    
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject tmpObj = jsonArray.getJSONObject(i);        
            Iterator key = tmpObj.keys();
            while (key.hasNext()) {
                String k = key.next().toString();
                //System.out.println("Key : " + k);
                try{
                    JSONArray arr3 = tmpObj.getJSONArray(k);
                    getJSONObjectFuncCall(arr3); 
                    if("function_call".equals(k)){
                         //System.out.println(funcName +" содержит функцию");
                         //System.out.println("Добавили: " + arr3);
                         listOfInsideFunc.add(arr3);
                    }
                    if("procedure_call".equals(k)){
                         //System.out.println(funcName +" содержит процедуру");
                         //System.out.println(arr3);
                         listOfInsideFunc.add(arr3);
                    }
                }
                catch(Exception ex){
                    if("line".equals(k)){                            
                        //System.out.println(tmpObj.getInt(k));
                    }
                    if("text".equals(k)){                            
                        //System.out.println(tmpObj.getString(k));
                    }
                    if("type".equals(k)){                            
                        //System.out.println(tmpObj.getInt(k));
                    }
                }
            }   
        }
    }
        
     //Получение вызовов функции/процедуры по ключу - general_element
    private static void getJSONObjectGeneralElem(JSONArray jsonArray) {    
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject tmpObj = jsonArray.getJSONObject(i);        
            Iterator key = tmpObj.keys();
            while (key.hasNext()) {
                String k = key.next().toString();
                //System.out.println("Key : " + k);
                try{
                    JSONArray arr3 = tmpObj.getJSONArray(k);
                    getJSONObjectGeneralElem(arr3); 
                    if("general_element".equals(k)){
                        //System.out.println("Содержится general_element");
                        listOfGenElem.add(arr3);
                    }
                }
                catch(Exception ex){
                    if("line".equals(k)){                            
                        //System.out.println(tmpObj.getInt(k));
                    }
                    if("text".equals(k)){                            
                        //System.out.println(tmpObj.getString(k));
                    }
                    if("type".equals(k)){                            
                        //System.out.println(tmpObj.getInt(k));
                    }
                }
            }   
        }
    }
    
    public static JSONArray arr2 = null;    
    //Поиск операции (insert, delete, update, select) в массиве
    private static void getResult(JSONArray jsonArray) {    
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject tmpObj = jsonArray.getJSONObject(i);        
            Iterator key = tmpObj.keys();
            while (key.hasNext()) {
                String k = key.next().toString();
                //System.out.println("Key : " + k);
                try{
                    arr2 = tmpObj.getJSONArray(k);  
                    if("select_statement".equals(k)){
                        //System.out.println("Найдена операция select");
                        //System.out.println(arr2);
                    }
                    if("insert_statement".equals(k)){
                        //System.out.println("Найдена операция insert");
                        //System.out.println(arr2);
                    }
                    if("delete_statement".equals(k)){
                        //System.out.println("Найдена операция delete");
                        //System.out.println(arr2);
                    }
                    if("update_statement".equals(k)){
    //                    System.out.println("Найдена операция update");
    //                    System.out.println(arr2);
                    }
                    getResult(arr2); 
                }
                catch(Exception ex){
                    if("line".equals(k)){                                  
                        //System.out.println(tmpObj.getInt(k));
                    }
                    if("text".equals(k)){  
                        if("delete".equals(tmpObj.getString(k))){
                            Structure s = new Structure();
                            s.TransactionType = tmpObj.getString(k);
                            //System.out.println("Тип операции: " + s.TransactionType);

                            s.TableName = arr2.getJSONObject(2).getJSONArray("general_table_ref").getJSONObject(0).getJSONArray("dml_table_expression"
                                    + "_clause").getJSONObject(0).getJSONArray("tableview_name").getJSONObject(0).getJSONArray("identif"
                                            + "ier").getJSONObject(0).getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular"
                                                    + "_id").getJSONObject(0).getString("text");

                            //System.out.println("Название таблицы: " + s.TableName);                        
                            //s.toString();
                            resultList.add(s);
                        }      
                        if("insert".equals(tmpObj.getString(k))){
                            Structure s = new Structure();
                            s.TransactionType = tmpObj.getString(k);
                            //System.out.println("Тип операции: " + s.TransactionType);

                            s.TableName = arr2.getJSONObject(1).getJSONArray("single_table_insert").getJSONObject(0).
                                    getJSONArray("insert_into_clause").getJSONObject(1).getJSONArray("general_table_ref").
                                    getJSONObject(0).getJSONArray("dml_table_expression_clause").getJSONObject(0).
                                    getJSONArray("tableview_name").getJSONObject(0).getJSONArray("identifier").getJSONObject(0).
                                    getJSONArray("id_expression").getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text");

    //                        System.out.println("Название таблицы: " + s.TableName);                        
    //                        s.toString();
                            resultList.add(s);
                        }    
                        if("update".equals(tmpObj.getString(k))){
                            Structure s = new Structure();
                            s.TransactionType = tmpObj.getString(k);
                            //System.out.println("Тип операции: " + s.TransactionType);

                            s.TableName = arr2.getJSONObject(1).getJSONArray("general_table_ref").getJSONObject(0)
                                    .getJSONArray("dml_table_expression_clause").getJSONObject(0).getJSONArray("tableview_name")
                                    .getJSONObject(0).getJSONArray("identifier").getJSONObject(0).getJSONArray("id_expression").
                                    getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text");
    //                      System.out.println("Название таблицы: " + s.TableName);                        
    //                      s.toString();
                            resultList.add(s);
                        }     
                        if("select".equals(tmpObj.getString(k))){
                            //System.out.println("!!!");
                            //System.out.println(arr2);
                            arrOfStr = new ArrayList<String>();
                            getJSONObjectTableName(arr2);
    //                        for (String i3: arrOfStr){
    //                            System.out.println(i3);
    //                        }
                            for (String i2: arrOfStr){
                                Structure s = new Structure();
                                s.TransactionType = tmpObj.getString(k);
                                //System.out.println("Тип операции: " + s.TransactionType);
                                s.TableName = i2;
                                resultList.add(s);
                            }
                        }  
                    }
                    if("type".equals(k)){                            
                        //System.out.println(tmpObj.getInt(k));
                    }
                }
            }   
        }
    }    
    
    //Получение названий таблиц
    private static void getJSONObjectTableName(JSONArray jsonArray2) {                
        String s = "";
        for (int i = 0; i < jsonArray2.length(); i++) {
            JSONObject tmpObj = jsonArray2.getJSONObject(i);        
            Iterator key = tmpObj.keys();
            while (key.hasNext()) {
                String k = key.next().toString();
                //System.out.println("Key : " + k);
                try{
                    JSONArray arr3 = tmpObj.getJSONArray(k);            
                    if("tableview_name".equals(k)){
                        s = arr3.getJSONObject(0).getJSONArray("identifier").getJSONObject(0).getJSONArray("id_expression")
                               .getJSONObject(0).getJSONArray("regular_id").getJSONObject(0).getString("text");
                        //System.out.println(s);
                        arrOfStr.add(s);                         
                    }
                    getJSONObjectTableName(arr3); 
                }
                catch(Exception ex){ }
                }   
            }
       }  
    
    static class FuncCallInfo {
        String FuncName1;//название пакета
        String FuncName2;//название функции
        Integer FuncLine;//номер строки функции

        //блок инициализатора
        {
            FuncName1 = "null";
            FuncName2 = "null";
            FuncLine = 0;
        }
        //пустой конструктор
        FuncCallInfo()    {
        }
        //конструктор
        FuncCallInfo(String FuncName1, int l, String FuncName2){
            this.FuncName1 = FuncName1;
            this.FuncName2 = FuncName2;
            this.FuncLine = l;
        }
        //вывод информации    
        @Override
        public String toString(){
            return "Package Name: " + FuncName1 + ", Function Name: " + FuncName2 + ", Line number: " + FuncLine;
        }
    }
    
    static class FuncInfo{
        String FuncName;//название функции
        Integer FuncLine;//номер строки функции

        //блок инициализатора
        {
            FuncName = "null";
            FuncLine = 0;
        }
        //пустой конструктор
        FuncInfo()    {
        }
        //конструктор
        FuncInfo(String FuncName, int l){
            this.FuncName = FuncName;
            this.FuncLine = l;
        }
        //вывод информации    
        @Override
        public String toString(){
            return "Name: " + FuncName + ", Line number: " + FuncLine;
        }
    }
}
