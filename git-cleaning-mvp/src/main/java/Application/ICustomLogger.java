package Application;

import java.util.logging.Level;

public interface ICustomLogger {

    void log(Level level, String message);

    void logRepoMsg(String message, int repoNum);

    void logBranchMsg(String message, int repoNum, int branchNum);

    void logTagMsg(String message, int repoNum, int tagNum);

    void logError(String message);

    void logRepoError(String message, int repoNum);

    void logBranchWarn(String message, int repoNum, int branchNum);

    void logTagWarn(String message, int repoNum, int tagNum);
}
