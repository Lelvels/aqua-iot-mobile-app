package pro.aquaiot.aquablue.data.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitInstance {
    private static Retrofit retrofit = null;
    public static final String AQUA_APPLICATION_URL = "https://aqua-iot.xyz/api/v1/";

    //Singleton Pattern
    public static AquaApplicationService getService(){
        if(retrofit == null){
            retrofit = new Retrofit.Builder().baseUrl(AQUA_APPLICATION_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(AquaApplicationService.class);
    }
}
