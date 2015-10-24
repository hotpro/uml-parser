package edu.sjsu.cmpe.yutao;

/**
 * Created by yutao on 10/8/15.
 */
public enum UmlRelationShipType {
    EX("<|--"),
    IM("<|.."),
    AS("--"),
    DEP("<.."),
    IM_LOLI("()--"),
    DEP_LOLI("-0)-");

    private String s;
    UmlRelationShipType(String s) {
        this.s = s;
    }
    public String getS() {
        return s;
    }
}
