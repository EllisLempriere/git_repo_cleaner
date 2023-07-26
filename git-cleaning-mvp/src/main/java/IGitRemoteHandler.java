public interface IGitRemoteHandler {

    boolean cloneRepo(ILogWrapper log);

    boolean updateRepo(ILogWrapper log);

    boolean hasRemote(ILogWrapper log);

    void addRemote(ILogWrapper log);
}
