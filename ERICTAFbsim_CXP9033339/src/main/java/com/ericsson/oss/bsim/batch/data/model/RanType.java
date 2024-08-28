package com.ericsson.oss.bsim.batch.data.model;

public enum RanType {
    WRAN("WCDMA"),
    LRAN("LTE");

    private String ranType;

    RanType(final String ranType) {
        this.ranType = ranType;
    }

    /**
     * @return the ranType
     */
    public String getRanType() {
        return ranType;
    }
}
