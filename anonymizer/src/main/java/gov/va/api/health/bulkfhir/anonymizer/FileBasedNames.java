package gov.va.api.health.bulkfhir.anonymizer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor
public class FileBasedNames implements Names {

  private static int NAMES_COUNT = 32549;
  private static final String FILE_NAME = "/yob2005.txt";
  private static List<String> NAMES = loadList();

  FileBasedNames(List<String> names) {
    NAMES = names;
    NAMES_COUNT = names.size();
  }

  @SneakyThrows
  private static List<String> loadList() {
    List<String> list = new ArrayList<>(NAMES_COUNT);
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(FILE_NAME)));
    while (reader.ready()) {
      list.add(reader.readLine().split(",")[0]);
    }
    return Collections.unmodifiableList(list);
  }

  /** Safely handle wrapping any value into an array index. */
  @Override
  public String getName(long index) {
    return NAMES.get((int) (index % (NAMES_COUNT - 1)));
  }
}
