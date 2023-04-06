package pro.aquaiot.aquablue.data.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FishData {
    @SerializedName("data")
    @Expose
    private List<Fish> data;

    public List<Fish> getData() {
        return data;
    }

    public void setData(List<Fish> data) {
        this.data = data;
    }
}
