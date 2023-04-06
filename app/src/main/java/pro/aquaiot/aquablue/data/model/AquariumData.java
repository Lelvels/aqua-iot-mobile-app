package pro.aquaiot.aquablue.data.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AquariumData {
    @SerializedName("data")
    @Expose
    private Aquarium aquarium;

    public Aquarium getAquarium() {
        return aquarium;
    }

    public void setAquarium(Aquarium data) {
        this.aquarium = data;
    }
}
