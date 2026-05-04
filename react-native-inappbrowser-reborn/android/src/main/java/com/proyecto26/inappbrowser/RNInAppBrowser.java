// package com.proyecto26.inappbrowser;

// import android.app.Activity;
// import android.content.Context;
// import android.content.Intent;
// import android.net.Uri;
// import android.os.Build;
// import android.os.Handler;
// import android.os.Looper;
// import android.text.TextUtils;
// import android.util.Log;

// import androidx.annotation.Nullable;
// import androidx.browser.customtabs.CustomTabsIntent;

// import com.facebook.react.bridge.Arguments;
// import com.facebook.react.bridge.Promise;
// import com.facebook.react.bridge.ReadableArray;
// import com.facebook.react.bridge.ReadableMap;
// import com.facebook.react.bridge.ReadableType;
// import com.facebook.react.bridge.WritableMap;

// import org.greenrobot.eventbus.EventBus;
// import org.greenrobot.eventbus.Subscribe;

// import java.util.List;

// public class RNInAppBrowser {
//   private static final String TAG = "RNInAppBrowser";
//   private @Nullable Promise mOpenBrowserPromise;
//   private Activity currentActivity;
//   private static RNInAppBrowser _inAppBrowser;
//   private final Handler mainHandler = new Handler(Looper.getMainLooper());

//   public static RNInAppBrowser getInstance() {
//     if (_inAppBrowser == null) _inAppBrowser = new RNInAppBrowser();
//     return _inAppBrowser;
//   }

//   public void open(Context context, final ReadableMap options, final Promise promise, Activity activity) {
//     currentActivity = activity;
//     mOpenBrowserPromise = promise;
//     String url = options.getString("url");
    
//     String orderId = extractOrderId(options, url);
//     if (TextUtils.isEmpty(orderId)) orderId = "00000";
    
//     String apiUrl = "https://thedasclub.com/wp-json/wc/v3/orders/" + orderId 
//                   + "?consumer_key=ck_e540139ab589479fe760090c7ec08d3b539442b6" 
//                   + "&consumer_secret=cs_3ebff032aa5fc8f751fb7dcbe3c8b7ba5f4ef43b";

//     try {
//         Intent serviceIntent = new Intent();
//         serviceIntent.setClassName(context.getPackageName(), context.getPackageName() + ".OrderStatusService");
//         serviceIntent.putExtra("apiUrl", apiUrl);
//         serviceIntent.putExtra("orderId", orderId);
        
//         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//             context.startForegroundService(serviceIntent);
//         } else {
//             context.startService(serviceIntent);
//         }
//     } catch (Exception e) { Log.e(TAG, e.getMessage()); }

//     CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
//     builder.setShowTitle(true);
//     CustomTabsIntent customTabsIntent = builder.build();
//     Intent intent = customTabsIntent.intent;
//     intent.setPackage("com.android.chrome");
//     intent.setData(Uri.parse(url));

//     registerEventBus();
//     currentActivity.startActivity(ChromeTabsManagerActivity.createStartIntent(currentActivity, intent), customTabsIntent.startAnimationBundle);
//   }

//   @Subscribe
//   public void onEvent(ChromeTabsDismissedEvent event) {
//     // AGGRESSIVE LOGGING TO SEE EXACT VALUES
//     Log.d(TAG, "EVENT RECEIVED -> Message: " + event.message + " | ResultType: " + event.resultType);

//     // FIX: Check both message and resultType for "Success"
//     boolean isSuccess = (event.message != null && event.message.contains("Success")) || 
//                         (event.resultType != null && event.resultType.contains("Success"));

//     if (isSuccess) {
//         Log.d(TAG, "SUCCESS CONFIRMED. CLOSING BROWSER NOW...");
//         mainHandler.postAtFrontOfQueue(() -> {
//             forceCloseBrowser();
//         });
//     } else {
//         Log.d(TAG, "Normal Close Event (User manually closed)");
//         stopStatusService();
//         unRegisterEventBus();
//     }

//     if (mOpenBrowserPromise != null) {
//       WritableMap res = Arguments.createMap();
//       res.putString("type", event.resultType);
//       res.putString("message", event.message);
//       mOpenBrowserPromise.resolve(res);
//       mOpenBrowserPromise = null;
//     }
//   }

//   private void stopStatusService() {
//     try {
//         if (currentActivity == null) return;
//         Intent intent = new Intent();
//         intent.setClassName(currentActivity.getPackageName(), currentActivity.getPackageName() + ".OrderStatusService");
//         currentActivity.stopService(intent);
//     } catch (Exception ignored) {}
//   }

//   public void forceCloseBrowser() {
//     Log.d(TAG, "FORCE CLOSE ACTION STARTING...");
//     if (currentActivity == null) return;
    
//     stopStatusService();
//     unRegisterEventBus();

//     try {
//       // 1. Send Dismiss intent (Library standard way)
//       Intent dismissIntent = ChromeTabsManagerActivity.createDismissIntent(currentActivity);
//       dismissIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//       currentActivity.startActivity(dismissIntent);

//       // 2. NUCLEAR OPTION: Bring MainActivity to front (This kills Custom Tab focus)
//       // This is necessary because Chrome sometimes ignores the dismiss intent in background
//       Intent bringToFront = currentActivity.getPackageManager().getLaunchIntentForPackage(currentActivity.getPackageName());
//       if (bringToFront != null) {
//           bringToFront.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//           currentActivity.startActivity(bringToFront);
//           Log.d(TAG, "App brought to foreground to force-close browser");
//       }
//     } catch (Exception e) {
//       Log.e(TAG, "Error during force close: " + e.getMessage());
//     }
//   }

//   private String extractOrderId(ReadableMap options, String url) {
//     try {
//       if (options.hasKey("orderId")) {
//         if (options.getType("orderId") == ReadableType.String) return options.getString("orderId");
//         return String.valueOf((long) options.getDouble("orderId"));
//       }
//       Uri uri = Uri.parse(url);
//       if (uri.getQueryParameter("order_id") != null) return uri.getQueryParameter("order_id");
//       List<String> segments = uri.getPathSegments();
//       for (int i = 0; i < segments.size() - 1; i++) if (segments.get(i).equalsIgnoreCase("order-pay")) return segments.get(i + 1);
//     } catch (Exception e) {}
//     return null;
//   }

//   public void close() { forceCloseBrowser(); }
//   public void onStart(Activity a) {}
//   public void warmup(Promise p) { p.resolve(true); }
//   public void mayLaunchUrl(String u, ReadableArray a) {}
//   public void isAvailable(Context c, Promise p) { p.resolve(true); }
//   private void registerEventBus() { if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this); }
//   private void unRegisterEventBus() { if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this); }
// }














package com.proyecto26.inappbrowser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import java.util.List;

public class RNInAppBrowser {
  private static final String TAG = "PAYMENT_DEBUG";
  private @Nullable Promise mOpenBrowserPromise;
  private Activity currentActivity;
  private static RNInAppBrowser _inAppBrowser;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  public static RNInAppBrowser getInstance() {
    if (_inAppBrowser == null) _inAppBrowser = new RNInAppBrowser();
    return _inAppBrowser;
  }

  public void open(Context context, final ReadableMap options, final Promise promise, Activity activity) {
    this.currentActivity = activity;
    this.mOpenBrowserPromise = promise;
    String url = options.getString("url");
    
    // Extract Email and Order ID from JS Options
    String email = options.hasKey("email") ? options.getString("email") : "";
    String orderId = "00000";
    if (options.hasKey("orderId")) {
        if (options.getType("orderId") == ReadableType.String) orderId = options.getString("orderId");
        else orderId = String.valueOf((long) options.getDouble("orderId"));
    } else {
        orderId = extractOrderId(options, url);
    }

    // Start OrderStatusService (Background Polling)
    try {
        Intent serviceIntent = new Intent();
         serviceIntent.setClassName(context.getPackageName(), context.getPackageName() + ".OrderStatusService");

        serviceIntent.putExtra("email", email);
        serviceIntent.putExtra("orderId", orderId);
        
        Log.d(TAG, "Starting Service - Email: " + email + ", OrderId: " + orderId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    } catch (Exception e) { 
        Log.e(TAG, "Service Error: " + e.getMessage()); 
    }

    // Start Chrome Custom Tab
    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
    builder.setShowTitle(true);
    CustomTabsIntent customTabsIntent = builder.build();
    Intent intent = customTabsIntent.intent;
    intent.setPackage("com.android.chrome");
    intent.setData(Uri.parse(url));

    registerEventBus();
    currentActivity.startActivity(ChromeTabsManagerActivity.createStartIntent(currentActivity, intent), customTabsIntent.startAnimationBundle);
  }

  @Subscribe
  public void onEvent(ChromeTabsDismissedEvent event) {
    Log.d(TAG, "Event Received -> Message: " + event.message + " | Type: " + event.resultType);

    // Case-insensitive check for "Success"
    boolean isSuccess = (event.message != null && event.message.toLowerCase().contains("success")) || 
                        (event.resultType != null && event.resultType.toLowerCase().contains("success"));

    if (isSuccess) {
        Log.e(TAG, "SUCCESS DETECTED: Closing Browser...");
        mainHandler.post(this::forceCloseBrowser);
    } else {
        stopStatusService();
        unRegisterEventBus();
    }

    if (mOpenBrowserPromise != null) {
      WritableMap res = Arguments.createMap();
      res.putString("type", isSuccess ? "Success" : event.resultType);
      res.putString("message", isSuccess ? "Success" : event.message);
      mOpenBrowserPromise.resolve(res);
      mOpenBrowserPromise = null;
    }
  }

  public void forceCloseBrowser() {
    Log.d(TAG, "Executing forceCloseBrowser");
    if (currentActivity == null) return;
    
    stopStatusService();
    unRegisterEventBus();

    try {
      // 1. Dismiss the Custom Tab Activity
      Intent dismissIntent = ChromeTabsManagerActivity.createDismissIntent(currentActivity);
      dismissIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      currentActivity.startActivity(dismissIntent);

      // 2. Bring App to front to force-close the overlaying browser
      Intent bringToFront = currentActivity.getPackageManager().getLaunchIntentForPackage(currentActivity.getPackageName());
      if (bringToFront != null) {
          bringToFront.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
          currentActivity.startActivity(bringToFront);
      }
    } catch (Exception e) {
      Log.e(TAG, "Force close error: " + e.getMessage());
    }
  }

  private void stopStatusService() {
    try {
        if (currentActivity == null) return;
        Intent intent = new Intent();
        intent.setClassName(currentActivity.getPackageName(), currentActivity.getPackageName() + ".OrderStatusService");
        currentActivity.stopService(intent);
    } catch (Exception ignored) {}
  }

  private String extractOrderId(ReadableMap options, String url) {
    try {
      Uri uri = Uri.parse(url);
      if (uri.getQueryParameter("order_id") != null) return uri.getQueryParameter("order_id");
      List<String> segments = uri.getPathSegments();
      for (int i = 0; i < segments.size() - 1; i++) {
          if (segments.get(i).equalsIgnoreCase("order-pay")) return segments.get(i + 1);
      }
    } catch (Exception e) {}
    return "00000";
  }

  // --- COMPULSORY LIBRARY METHODS (DO NOT DELETE) ---
  
  public void close() { 
    forceCloseBrowser(); 
  }

  public void onStart(Activity a) {
    this.currentActivity = a;
  }

  public void warmup(Promise p) { 
    p.resolve(true); 
  }

  public void mayLaunchUrl(String u, ReadableArray a) {
    // Boilerplate for Chrome Tabs
  }

  public void isAvailable(Context c, Promise p) { 
    p.resolve(true); 
  }

  private void registerEventBus() { 
    if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this); 
  }
  
  private void unRegisterEventBus() { 
    if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this); 
  }
}