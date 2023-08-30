package Application;

import java.util.logging.Level;

public interface ICustomLogger {

    void log(Level level, String message);

    void logRepoMsg(String message, int repoNum);

    void logBranchMsg(String message, int repoNum, int branchNum);

    void logTagMsg(String message, int repoNum, int tagNum);
}
