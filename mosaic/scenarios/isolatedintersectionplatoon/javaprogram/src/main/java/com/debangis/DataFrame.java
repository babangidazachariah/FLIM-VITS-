import java.util.ArrayList;
import java.util.List;

public class DataFrame {
    private List<String> columnNames;
    private List<List<Object>> data;

    public DataFrame(List<String> columnNames) {
        this.columnNames = columnNames;
        this.data = new ArrayList<>();
    }

    public void addRow(List<Object> rowData) {
        if (rowData.size() != columnNames.size()) {
            throw new IllegalArgumentException("Row data size must match the number of columns.");
        }
        data.add(rowData);
    }

    public List<Object> getRow(int rowIndex) {
        return data.get(rowIndex);
    }

    public Object getValue(int rowIndex, int columnIndex) {
        return data.get(rowIndex).get(columnIndex);
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public int getRowCount() {
        return data.size();
    }

    public int getColumnCount() {
        return columnNames.size();
    }

    public static void main(String[] args) {
        List<String> columnNames = List.of("Name", "Age", "City");
        DataFrame df = new DataFrame(columnNames);

        df.addRow(List.of("John", 30, "New York"));
        df.addRow(List.of("Alice", 25, "London"));
        df.addRow(List.of("Bob", 40, "Paris"));

        System.out.println("DataFrame:");
        System.out.println("Column Names: " + df.getColumnNames());
        for (int i = 0; i < df.getRowCount(); i++) {
            System.out.println("Row " + i + ": " + df.getRow(i));
        }
    }
}
