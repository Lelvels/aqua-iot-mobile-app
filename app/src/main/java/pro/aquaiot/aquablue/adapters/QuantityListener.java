package pro.aquaiot.aquablue.adapters;

import java.util.ArrayList;

import pro.aquaiot.aquablue.data.model.Fish;

public interface QuantityListener {
    void onQuantityChange(ArrayList<Fish> fishArrayList);
}
