package pro.aquaiot.aquablue;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import pro.aquaiot.aquablue.adapters.FishAdapter;
import pro.aquaiot.aquablue.data.model.Aquarium;
import pro.aquaiot.aquablue.data.model.AquariumData;
import pro.aquaiot.aquablue.data.model.CreateAquariumRequest;
import pro.aquaiot.aquablue.data.model.Fish;
import pro.aquaiot.aquablue.data.model.FishData;
import pro.aquaiot.aquablue.data.model.SyncFishAquaRequest;
import pro.aquaiot.aquablue.data.remote.AquaApplicationService;
import pro.aquaiot.aquablue.data.remote.RetrofitInstance;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FishForAquariumActivity extends AppCompatActivity {
    private final String TAG = "FishAquaLog";
    private String aquariumName = "";
    private List<Fish> fishList;
    private Button goBackButton;
    private Button confirmButton;
    private List<Fish> fishInAquarium;
    private Aquarium aquarium = null;
    private FishAdapter fishAdapter;
    private RecyclerView recyclerView;
    List<Integer> dupIndex = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fish_for_aquarium);
        Bundle bundle = getIntent().getExtras();
        aquariumName = bundle.getString("device_name");
        Log.i(TAG, "Starting FishForAquarium Activity with aquarium name: " + aquariumName);
        goBackButton = findViewById(R.id.af_go_back_button);
        goBackButton.setOnClickListener(v->{
            finish();
        });
        fishList = new ArrayList<>();
        recyclerView = findViewById(R.id.af_recycle_view);
        fishAdapter = new FishAdapter(fishList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FishForAquariumActivity.this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(fishAdapter);
        confirmButton = findViewById(R.id.af_confirm_buton);
        getAquarium(aquariumName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getAllFishes();
    }

    public void getAllFishes(){
        AquaApplicationService aquaApplicationService = RetrofitInstance.getService();
        Call<FishData> fishDataCall = aquaApplicationService.getAllFishes();
        fishDataCall.enqueue(new Callback<FishData>() {
            @Override
            public void onResponse(Call<FishData> call, Response<FishData> response) {
                FishData fishData = response.body();
                if(fishData != null && fishData.getData() != null){
                    List<Fish> result = fishData.getData();
                    Log.i(TAG, "All the fishes: ");
                    for(Fish fish : result){
                        Log.i(TAG, fish.toString());
                    }
                    fishList.addAll(result);
                    fishAdapter.notifyDataSetChanged();
                    getFishInAquarium();
                }
            }
            @Override
            public void onFailure(Call<FishData> call, Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        });
    }

    public ArrayList<Integer> findDuplicates(List<Fish> fishList1, List<Fish> fishList2){
        HashSet<Integer> map = new HashSet<Integer>();
        ArrayList<Integer> dupIndex = new ArrayList<>();
        for(Fish fish2 : fishList2){
            map.add(fish2.getId());
        }
        for(Fish fish1 : fishList1){
            if(map.contains(fish1.getId())){
                dupIndex.add(fish1.getId());
            }
        }
        return dupIndex;
    }

    public void getFishInAquarium(){
        AquaApplicationService aquaApplicationService = RetrofitInstance.getService();
        Call<FishData> call = aquaApplicationService.getFishesByAquariumId(aquarium.getId());
        call.enqueue(new Callback<FishData>() {
            @Override
            public void onResponse(Call<FishData> call, Response<FishData> response) {
                FishData fishData = response.body();
                if(fishData != null && fishData.getData() != null){
                    List<Fish> result = fishData.getData();
                    Log.i(TAG, "Fishes in aquarium: ");
                    for(Fish fish : result){
                        Log.i(TAG, fish.toString());
                    }
                    dupIndex = findDuplicates(fishList, result);
                    for(Integer i : dupIndex){
                        Log.i(TAG, "Dup index: " + i);
                        CheckBox checkBox = (CheckBox) recyclerView.getLayoutManager().findViewByPosition(i).findViewById(R.id.item_fish_cb);
                        checkBox.setChecked(true);
                    }
                }
            }

            @Override
            public void onFailure(Call<FishData> call, Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        });
    }

    public void getAquarium(String aquariumName) {
        AquaApplicationService aquaApplicationService = RetrofitInstance.getService();
        Call<AquariumData> call = aquaApplicationService.getAquariumByName(aquariumName);
        call.enqueue(new Callback<AquariumData>() {
            @Override
            public void onResponse(Call<AquariumData> call, Response<AquariumData> response) {
                Aquarium result = response.body().getAquarium();
                if(result != null){
                    aquarium = result;
                } else {
                    createAquarium(aquariumName, "New aquarium created by AquaBlue");
                }
            }
            @Override
            public void onFailure(Call<AquariumData> call, Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        });
    }

    public void createAquarium(String aquariumName, String description){
        AquaApplicationService aquaApplicationService = RetrofitInstance.getService();
        CreateAquariumRequest createAquariumRequest = new CreateAquariumRequest(aquariumName, description);
        Call<AquariumData> call = aquaApplicationService.createNewAquarium(createAquariumRequest);
        call.enqueue(new Callback<AquariumData>() {
            @Override
            public void onResponse(Call<AquariumData> call, Response<AquariumData> response) {
                AquariumData aquariumData = response.body();
                if(aquariumData != null && aquariumData.getAquarium() != null){
                    aquarium = aquariumData.getAquarium();
                    Log.i(TAG, "Create new aquarium: " + aquarium.toString());
                }
            }

            @Override
            public void onFailure(Call<AquariumData> call, Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        });
    }
}