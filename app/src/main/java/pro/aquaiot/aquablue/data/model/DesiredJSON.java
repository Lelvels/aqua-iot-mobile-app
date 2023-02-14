package pro.aquaiot.aquablue.data.model;

import com.google.gson.Gson;

public class DesiredJSON {
    private String desired;

    public DesiredJSON() {
    }

    public DesiredJSON(Desired desiredObj){
        Gson gson = new Gson();
        this.desired = gson.toJson(desiredObj);
    }

    public String getDesired() {
        return desired;
    }

    public void setDesired(Desired desiredObj) {
        Gson gson = new Gson();
        this.desired = gson.toJson(desiredObj);
    }
}
