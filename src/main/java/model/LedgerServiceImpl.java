package model;

import java.util.Arrays;
import java.util.List;

/**
 * Created on 3/9/18.
 */
public class LedgerServiceImpl implements LedgerServiceInterface {

    String[] returnArray = {"entry1","entry2","entry3"};

    @Override
    public List<String> getEntries(int count) {
        return Arrays.asList(returnArray);
    }
}