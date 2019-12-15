package model;

import java.util.List;

/**
 * Created on 3/9/18.
 */
public interface LedgerServiceInterface {
    List<String> getEntries(int count);
}