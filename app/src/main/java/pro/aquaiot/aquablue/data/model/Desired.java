package pro.aquaiot.aquablue.data.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Desired {

    @SerializedName("autoMode")
    @Expose
    private Boolean autoMode;
    @SerializedName("heater")
    @Expose
    private Boolean heater;
    @SerializedName("chiller")
    @Expose
    private Boolean chiller;
    @SerializedName("setTemper")
    @Expose
    private Integer setTemper;
    public Desired(){
    }
    public Desired(Boolean autoMode, Boolean heater, Boolean chiller, Integer setTemper) {
        this.autoMode = autoMode;
        this.heater = heater;
        this.chiller = chiller;
        this.setTemper = setTemper;
    }
    public Boolean getAutoMode() {
        return autoMode;
    }

    public void setAutoMode(Boolean autoMode) {
        this.autoMode = autoMode;
    }

    public Boolean getHeater() {
        return heater;
    }

    public void setHeater(Boolean heater) {
        this.heater = heater;
    }

    public Boolean getChiller() {
        return chiller;
    }

    public void setChiller(Boolean chiller) {
        this.chiller = chiller;
    }

    public Integer getSetTemper() {
        return setTemper;
    }

    public void setSetTemper(Integer setTemper) {
        this.setTemper = setTemper;
    }

}
