package pro.aquaiot.aquablue.data.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SensorDataValues {
    @SerializedName("pH")
    @Expose
    private Double pH;
    @SerializedName("TDS")
    @Expose
    private Integer tds;
    @SerializedName("nhietdo")
    @Expose
    private Double nhietdo;

    public Double getpH() {
        return pH;
    }

    public void setpH(Double pH) {
        this.pH = pH;
    }

    public Integer getTds() {
        return tds;
    }

    public void setTds(Integer tds) {
        this.tds = tds;
    }

    public Double getNhietdo() {
        return nhietdo;
    }

    public void setNhietdo(Double nhietdo) {
        this.nhietdo = nhietdo;
    }
}
