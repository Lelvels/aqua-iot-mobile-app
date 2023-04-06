package pro.aquaiot.aquablue.data.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SyncFishAquaRequest {
    @SerializedName("aquariumId")
    @Expose
    private Integer aquariumId;
    @SerializedName("fishIds")
    @Expose
    private List<Integer> fishIds;

    public Integer getAquariumId() {
        return aquariumId;
    }

    public SyncFishAquaRequest(Integer aquariumId, List<Integer> fishIds) {
        this.aquariumId = aquariumId;
        this.fishIds = fishIds;
    }

    public void setAquariumId(Integer aquariumId) {
        this.aquariumId = aquariumId;
    }

    public List<Integer> getFishIds() {
        return fishIds;
    }

    public void setFishIds(List<Integer> fishIds) {
        this.fishIds = fishIds;
    }

}
