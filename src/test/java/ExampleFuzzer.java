import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExampleFuzzer {
    private static final int STRING_MAX_LENGTH = 100;

    @SuppressWarnings({ "checkstyle:noSystemOut", "PMD.SystemPrintln" })
    public static void fuzzerTestOneInput(FuzzedDataProvider dataProvider) {
        String s = dataProvider.consumeAsciiString(STRING_MAX_LENGTH);
        if (s.contains("foo")) {
            // throw new IllegalStateException("'foo' found in string: " + s);
            System.out.println(s);
        }
    }
}
