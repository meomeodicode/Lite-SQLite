public class DebugInsert {
    public static void main(String[] args) {
        String sql = "INSERT INTO users (name, age) VALUES ('John', 30)";
        System.out.println("Original SQL: " + sql);
        lite.sqlite.server.cmdinterface.domain.parserImpl.SQLParser parser = new lite.sqlite.server.cmdinterface.domain.parserImpl.SQLParser(sql);
        Object result = parser.updateCmd();
        if (result instanceof lite.sqlite.server.cmdinterface.domain.commands.InsertData) {
            lite.sqlite.server.cmdinterface.domain.commands.InsertData insertData = (lite.sqlite.server.cmdinterface.domain.commands.InsertData) result;
            System.out.println("Table: " + insertData.getTblName());
            System.out.println("Fields: " + insertData.getFields());
            System.out.println("Fields size: " + insertData.getFields().size());
            System.out.println("Values: " + insertData.getVal());
            System.out.println("Values size: " + insertData.getVal().size());
        }
    }
}
