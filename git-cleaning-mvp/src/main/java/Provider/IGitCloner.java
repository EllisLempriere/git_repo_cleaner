package Provider;

import Business.Models.GitCloningException;

public interface IGitCloner {

    void cloneRepo() throws GitCloningException;
}
