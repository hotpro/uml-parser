package edu.sjsu.cmpe.yutao;

/**
 * Created by yutao on 10/8/15.
 */
public enum UmlRelationShipType {
    EX("<|--"),
    IM("<|.."),
    AS("--"),
    DEP("<.."),
    LOLI("()--");

    private String s;
    UmlRelationShipType(String s) {
        this.s = s;
    }
    public String getS() {
        return s;
    }
}
