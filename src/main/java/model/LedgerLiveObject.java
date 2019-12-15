package model;

import org.redisson.api.annotation.REntity;
import org.redisson.api.annotation.RId;

/**
 * Created on 3/9/18.
 */
@REntity
public class LedgerLiveObject {
    @RId
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}