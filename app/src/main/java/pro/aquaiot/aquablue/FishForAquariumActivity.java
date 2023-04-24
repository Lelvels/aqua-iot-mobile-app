package pro.aquaiot.aquablue;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

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
    private List<Fish> fishInAquarium;
    private Button goBackButton;
    private Button confirmButton;
    private Aquarium aquarium = null;
    private FishAdapter fishAdapter;
    private RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fish_for_aquarium);
        Bundle bundle = getIntent().getExtras();
        aquariumName = bundle.getString("device_name");
        Log.i(TAG, "Starting FishForAquarium Activity with aquarium name: " + aquariumName);
        goBackButton = findViewById(R.id.af_go_back_button);
        goBackButton.setOnClickListener(v -> {
            finish();
        });
        fishList = new ArrayList<>();
        fishInAquarium = new ArrayList<>();
        recyclerView = findViewById(R.id.af_recycle_view);
        fishAdapter = new FishAdapter(fishList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FishForAquariumActivity.this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(fishAdapter);
        confirmButton = findViewById(R.id.af_confirm_buton);
        confirmButton.setOnClickListener(v -> {
            updateFishInAquarium();
        });
        getAquarium(aquariumName);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public ArrayList<Integer> findDuplicates(List<Fish> fishList1, List<Fish> fishList2){
        HashSet<Integer> map = new HashSet<>();
        ArrayList<Integer> dupIndex = new ArrayList<>();
        if(fishList2 != null && fishList1 != null){
            for(Fish fish2 : fishList2){
                map.add(fish2.getId());
            }
            int len = fishList1.size();
            for(int idx = 0; idx<len; idx++){
                if(map.contains(fishList1.get(idx).getId())){
                    dupIndex.add(idx);
                }
            }
        }
        return dupIndex;
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
                } else {
                    Toast.makeText(FishForAquariumActivity.this,
                            "Hiện tại chưa có loài cá nào trong cơ sở dữ liệu",
                            Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<FishData> call, Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        });
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
                    fishInAquarium.addAll(result);
                    ArrayList<Integer> dupIndex = findDuplicates(fishList, fishInAquarium);
                    if(recyclerView.getLayoutManager() != null){
                        for(Integer idx : dupIndex){
                            CheckBox checkBox = (CheckBox) recyclerView.getLayoutManager().findViewByPosition(idx).findViewById(R.id.item_fish_cb);
                            checkBox.setChecked(true);
                        }
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
                if(response.isSuccessful()){
                    if(response.body().getAquarium() != null){
                        aquarium = response.body().getAquarium();
                        Log.i(TAG, "Get aquarium: " + aquarium.toString());
                        getAllFishes();
                    } else {
                        createAquarium(aquariumName, "New aquarium created by AquaBlue");
                    }
                } else {
                    Toast.makeText(FishForAquariumActivity.this,
                            "Không thể nhận dữ liệu từ Server, hãy kiểm tra đường truyền và thử lại sau",
                            Toast.LENGTH_SHORT).show();
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
                if(response.isSuccessful()){
                    if(response.body() != null) {
                        aquarium = response.body().getAquarium();
                        Log.i(TAG, "Create new aquarium: " + aquarium.toString());
                        getFishInAquarium();
                        getAllFishes();
                    }
                    else {
                        Toast.makeText(FishForAquariumActivity.this,
                                "Không thể nhận dữ liệu từ Server, hãy kiểm tra đường truyền và thử lại sau",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(FishForAquariumActivity.this,
                            "Không thể nhận dữ liệu từ Server, hãy kiểm tra đường truyền và thử lại sau",
                            Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<AquariumData> call, Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        });
    }

    public void updateFishInAquarium(){
        Toast.makeText(this, "Đang cập nhật", Toast.LENGTH_SHORT).show();
        AquaApplicationService aquaApplicationService = RetrofitInstance.getService();
        List<Integer> fishIds = new ArrayList<>();
        if(fishList != null){
            for(int i = 0; i<fishList.size(); i++){
                Log.i(TAG, "Iter though i = " + i);
                CheckBox checkBox =  Objects.requireNonNull(recyclerView.getLayoutManager())
                                    .findViewByPosition(i)
                                    .findViewById(R.id.item_fish_cb);
                if(checkBox.isChecked()){
                    fishIds.add(fishList.get(i).getId());
                }
            }
        }
        SyncFishAquaRequest syncFishAquaRequest = new SyncFishAquaRequest(aquarium.getId(), fishIds);
        StringBuilder idString = new StringBuilder();
        idString.append("Fishes id update: ");
        for(Integer id : fishIds){
            idString.append(id);
            idString.append(",");
        }
        Log.i(TAG, idString.toString());
        aquaApplicationService.syncFishInAquarium(syncFishAquaRequest).enqueue(new Callback<FishData>() {
            @Override
            public void onResponse(Call<FishData> call, Response<FishData> response) {
                if(response.body() != null){
                    List<Fish> resultFishList = response.body().getData();
                    if(resultFishList != null){
                        for(Fish fish : resultFishList){
                            Log.i(TAG, "Updated fish: " + fish.toString());
                        }
                    }
                    Toast.makeText(FishForAquariumActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                } else{
                    Log.i(TAG, response.errorBody().toString());
                    Toast.makeText(FishForAquariumActivity.this, "Cập nhật thất bại, đã có lỗi xảy ra!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FishData> call, Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        });
    }
}