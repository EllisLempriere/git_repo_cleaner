package TestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class TestUtils {

    /***
     * Finds the file specified by parameter and returns the absolute file path, as long as the file is under the
     * src directory of the project
     * @param fileName The file name to search for
     * @return The absolute path of the specified file
     */
    public static String getFullFilePath(String fileName) {
        try (Stream<Path> paths = Files.find(Paths.get("src"), 50,
                (p, attr) -> p.getFileName().toString().matches(fileName))) {

            return paths.toList().get(0).toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /***
     * Gets the absolute directory path of the repo the project is under.
     * @return The absolute path of the repo directory
     */
    public static String getProjectRepoDir() {
        File dir = new File("");
        String[] pathComponents = dir.getAbsolutePath().split("\\\\");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pathComponents.length - 1; i++) {
            sb.append(pathComponents[i]);
            sb.append("\\");
        }

        return sb.toString();
    }
}
