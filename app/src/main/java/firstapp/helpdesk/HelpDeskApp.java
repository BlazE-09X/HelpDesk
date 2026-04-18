package firstapp.helpdesk;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import firstapp.helpdesk.BuildConfig;

import java.util.HashMap;
import java.util.Map;

public class HelpDeskApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Используем данные из BuildConfig, которые берутся из local.properties
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
        config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
        config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
        
        // Инициализация работает только если ключи прописаны в local.properties
        if (!BuildConfig.CLOUDINARY_CLOUD_NAME.isEmpty()) {
            MediaManager.init(this, config);
        }
    }
}