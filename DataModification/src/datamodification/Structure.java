package datamodification;

public class Structure {
     String TableName;//название таблицы
    String TransactionType;//тип операции (insert, delete, update, select)
    String FuncName;//название функции
    Integer StrLine;//номер строки функции
    
    //блок инициализатора
    {
        TableName = "null";
        TransactionType = "null";
        FuncName = "null";
        StrLine = 0;
    }
    //пустой конструктор
    Structure()    {
    }
    //конструктор
    Structure(String TableName,String TransactionType, String FuncName, int l){
        this.TableName = TableName;
        this.TransactionType = TransactionType;
        this.FuncName = FuncName;
        this.StrLine = l;
    }
    //вывод информации    
    @Override
    public String toString(){
        return "Table name: " + TableName + " | Transaction type: " + TransactionType + " | Function name: " + FuncName +
                " | Line number: " + StrLine;
    }
}
