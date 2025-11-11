package com.dexter.little_smart_chat.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.HttpException;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ext.SdkExtensions;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dexter.little_smart_chat.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;


import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class OPUtils {
    public static class Logger {
        // 私有化构造函数，防止实例化
        private Logger() {
        }

        private static String getDebugInfo() {
            Throwable stack = new Throwable().fillInStackTrace();
            StackTraceElement[] trace = stack.getStackTrace();
            int n = 2;
            return trace[n].getClassName() + " meth name:" + trace[n].getMethodName() + "()" + ":" + trace[n].getLineNumber() +
                    " ";
        }

        // 提取公共的日志格式化方法
        @SuppressLint("DefaultLocale")
        private static String formatMessage(String msg) {
            return "|=============================|" + "\n" + String.format("%s: - %s", getDebugInfo(), msg) + "\n" + "|=============================|" + "\n";
        }

        // 各种日志级别的静态方法
        public static void d(String tag, String msg) {
            Log.d(tag, formatMessage(msg));
        }

        public static void e(String tag, String msg) {
            Log.e(tag, formatMessage(msg));
        }

        public static void i(String tag, String msg) {
            Log.i(tag, formatMessage(msg));
        }

        public static void v(String tag, String msg) {
            Log.v(tag, formatMessage(msg));
        }

        public static void w(String tag, String msg) {
            Log.w(tag, formatMessage(msg));
        }
        public static void dtf(String tag,String msg) {
            Log.d("D", formatMessage(tag+"        "+msg));
        }
        public static void wtf(String tag,String msg) {
            Log.d("W", formatMessage(tag+"        "+msg));
        }

        public static void etf(String tag, String msg, Throwable e) {
            Log.d("E", formatMessage(tag + "        " + msg));
         //todo   ThreadPoolManager.getInstance().execute(() -> {
//                if (e != null && CrashHandler.getInstance().isInit()) {
//                    CrashHandler.getInstance().handleException(msg, e);
//                }
//            });

        }
        public static void itf(String tag,String msg) {
            Log.d("I", formatMessage(tag+"        "+msg));
        }
        /**
         * 格式化日志消息
         */
        public static String formatLogMessage(String message) {
            // 如果是响应体且过大，则截断
            if (message.startsWith("<-- END HTTP")) {
                return "【Response body omitted】";
            }

            // 如果是请求体且过大，则截断
            if (message.startsWith("--> END")) {
                return "【Request body omitted】";
            }

            // 对于请求/响应头，只保留关键信息
            if (message.contains("Content-Type") ||
                    message.contains("Content-Length") ||
                    message.startsWith("-->") ||
                    message.startsWith("<--")) {
                return message;
            }

            // 如果消息过长，截断显示
            if (message.length() > 500) {
                return message.substring(0, 500) + "... 【Truncated】";
            }

            return message;
        }

    }
    public static class ThreadPoolManager {

        // 核心线程池大小
        private static final int CORE_POOL_SIZE = 10;
        // 最大线程池大小
        private static final int MAX_POOL_SIZE = 20;
        // 非核心线程空闲存活时间
        private static final long KEEP_ALIVE_TIME = 60L;
        // 线程池的队列容量
        private static final int QUEUE_CAPACITY = 100;

        // 单例实例
        private static volatile ThreadPoolManager instance = null;

        // 通用线程池
        private final ThreadPoolExecutor threadPoolExecutor;

        // 定时任务线程池
        private final ScheduledThreadPoolExecutor scheduledExecutor;

        // 私有构造函数，确保单例模式
        private ThreadPoolManager() {
            threadPoolExecutor = new ThreadPoolExecutor(
                    CORE_POOL_SIZE,
                    MAX_POOL_SIZE,
                    KEEP_ALIVE_TIME,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

            scheduledExecutor = new ScheduledThreadPoolExecutor(CORE_POOL_SIZE);
        }

        // 获取单例实例的方法，双重检查锁定
        public static ThreadPoolManager getInstance() {
            if (instance == null) {
                synchronized (ThreadPoolManager.class) {
                    if (instance == null) {
                        instance = new ThreadPoolManager();
                    }
                }
            }
            return instance;
        }

        // 提交任务到通用线程池
        public void execute(Runnable task) {
            threadPoolExecutor.execute(task);
        }

        // 提交带返回值的任务到通用线程池
        public <T> Future<T> submit(Callable<T> task) {
            return threadPoolExecutor.submit(task);
        }

        // 安排定时任务
        public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
            return scheduledExecutor.schedule(task, delay, unit);
        }

        // 安排周期性任务
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
            return scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit);
        }

        // 安排带固定延迟的周期性任务
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
            return scheduledExecutor.scheduleWithFixedDelay(task, initialDelay, delay, unit);
        }

        // 关闭线程池
        public void shutdown() {
            threadPoolExecutor.shutdown();
            scheduledExecutor.shutdown();
        }

        // 立即关闭线程池
        public void shutdownNow() {
            threadPoolExecutor.shutdownNow();
            scheduledExecutor.shutdownNow();
        }


        public <T> void submitNoWait(Callable<T> tCallable) {
            threadPoolExecutor.submit(tCallable);
        }

        //实现一个同一个接口批量请求带一个参数然后获得批量所有的返回结果后返回带一个key知道那些请求成功

    }

    public static class GeneratedValue {
        //获取一个uuid加一个时间戳
        public static String getUUIDAndTime() {
            return  UUID.randomUUID().toString().replace("-", "").toUpperCase()+"-"+System.currentTimeMillis() ;
        }
    }
    public static class KeyUtils {
        //单向Hash加密
        public static class HashUtil {
            private static final String MD5 = "MD5";//MD5加密可能存在Hash碰撞(32个字符)
            private static final String SHA256 = "SHA-256";//SHA256加密(64个字符)

            public static String getMD5(String key) {
                try {
                    MessageDigest md = MessageDigest.getInstance(MD5);
                    byte[] messageDigest = md.digest(key.getBytes());

                    StringBuilder hexString = new StringBuilder();
                    for (byte b : messageDigest) {
                        String hex = Integer.toHexString(0xFF & b);
                        if (hex.length() == 1) {
                            hexString.append('0');
                        }
                        hexString.append(hex);
                    }
                    return hexString.toString();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            public static String getSHA256(String key) {
                try {
                    MessageDigest md = MessageDigest.getInstance(SHA256);
                    byte[] messageDigest = md.digest(key.getBytes());

                    StringBuilder hexString = new StringBuilder();
                    for (byte b : messageDigest) {
                        String hex = Integer.toHexString(0xFF & b);
                        if (hex.length() == 1) {
                            hexString.append('0');
                        }
                        hexString.append(hex);
                    }
                    return hexString.toString();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        //AES加密解密
        public static class CryptoUtil {
            private static final String AES = "AES";

            // 加密方法
            public static String encrypt(String data, String key) throws Exception {
                SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES);
                Cipher cipher = Cipher.getInstance(AES);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(encryptedBytes);
            }

            // 解密方法
            public static String decrypt(String encryptedData, String key) throws Exception {
                SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES);
                Cipher cipher = Cipher.getInstance(AES);
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
                return new String(decryptedBytes, StandardCharsets.UTF_8);
            }
        }
    }

    public static class NetworkHandler {
        private static final int MAX_RETRIES = 1000;

        private static final long RETRY_DELAY_MS = 10000; // 10 seconds

        public static final int retryCount = 5;//默认最大重试次数

        public interface NetworkTask {
            void execute() throws Exception;
        }

        private NetworkHandler() {

        }
        public static class Inner{
            private static final NetworkHandler instance = new NetworkHandler();
        }
        public static NetworkHandler getInstance() {
            return Inner.instance;
        }


        public void executeWithRetry(NetworkTask task) throws Exception {
            AtomicInteger retryCount = new AtomicInteger(0);

            while (retryCount.get() <= MAX_RETRIES) {
                try {
                    Future<?> future = ThreadPoolManager.getInstance().submit(() -> {
                        try {
                            task.execute();
                        } catch (Exception e) {
                            throw e; // 直接抛出原始异常，不要包装在RuntimeException中
                        }
                        return null;
                    });

                    future.get(); // 等待任务完成
                    return; // 如果成功执行，直接返回
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof SocketTimeoutException || cause instanceof ConnectException) {
                        Logger.e("executeWithRetry", "Network exception occurred: " + cause.getMessage()+ " task:"+task.toString());
                        handleNetworkException(cause, retryCount);
                    } else {
                        // 对于非网络异常，直接抛出
                        Logger.e("executeWithRetry", "Non-network exception occurred: " + cause.getMessage()+ " task:"+task.toString());
                        throw new Exception("Non-network exception occurred", cause);
                    }
                } catch (InterruptedException e) {
                    Logger.e("workDao", "Task was interrupted");
                    Thread.currentThread().interrupt();
                    throw new Exception("Task was interrupted", e);
                }
            }
            throw new Exception("Max retries reached for network exception");
        }
        public void infiniteExecuteWithRetry_odl(NetworkTask task,Runnable runnable) {
            ThreadPoolManager.getInstance().execute(()->{
                AtomicInteger retryCount = new AtomicInteger(0);
                try {
                    while (retryCount.get() <= MAX_RETRIES) {
                        try {
                            CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                                try {
                                    task.execute();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return null;
                            });
                            future.get(); // 等待任务完成
                            return; // 如果成功执行，直接返回
                        } catch (ExecutionException e) {
                            Throwable cause = e.getCause();
                            if (cause instanceof SocketTimeoutException || cause instanceof ConnectException) {
                                Logger.e("executeWithRetry", "Network exception occurred: " + cause.getMessage() + " task:" + task.toString());
                                infiniteHandleNetworkException(cause, retryCount);
                            } else {
                                // 对于非网络异常，直接抛出
                                Logger.e("executeWithRetry", "Non-network exception occurred: " + cause.getMessage() + " task:" + task.toString());
                                throw new Exception("Non-network exception occurred", cause);
                            }
                        } catch (InterruptedException e) {
                            Logger.e("workDao", "Task was interrupted");
                            Thread.currentThread().interrupt();
                            throw new Exception("Task was interrupted", e);
                        }
                    }
                    runnable.run();
                } catch (Exception e) {
                    e.printStackTrace();
                    Logger.etf(task.getClass().getName(), "executeWithRetry Thread interrupted" + e.getMessage(),e);
                }
            });
        }


        public void infiniteExecuteWithRetry(NetworkTask task, Runnable onTimeout, RequestCallback callback, int retryCount) {
            ThreadPoolManager.getInstance().execute(() -> {
                try {
                    task.execute(); // 尝试执行任务
                    return;
                } catch (Exception e) {
                    Throwable cause = e instanceof ExecutionException ? e.getCause() : e;

                    if (cause instanceof SocketTimeoutException || cause instanceof ConnectException) {
                        Logger.e("RetryExecutor", "Network exception: " + cause.getMessage() + " task: " + task);

                        if (retryCount < MAX_RETRIES) {
                            int nextRetry = retryCount + 1;
                            Logger.i("RetryExecutor", "Retrying... attempt " + nextRetry);

                            ThreadPoolManager.getInstance().schedule(() ->
                                            infiniteExecuteWithRetry(task, onTimeout,callback, nextRetry),
                                    RETRY_DELAY_MS,
                                    TimeUnit.MILLISECONDS
                            );
                        } else {
                            Optional.ofNullable(onTimeout).ifPresent(Runnable::run);// 网络异常后回调
                            Logger.e("RetryExecutor", "Max retry attempts reached. Task failed: " + task);
                        }
                    } else {
                        // 非网络异常，直接记录并结束
                        Logger.etf("RetryExecutor", "Non-network exception: " + cause,  cause);
                        Optional.ofNullable(callback).ifPresent(a->a.onResult(false, "发生异常："+cause.getMessage()));// 网络异常后回调

                    }
                }
            });
        }



        private void handleException(Exception e, AtomicInteger retryCount) {
            e.printStackTrace();
            Logger.e("workDao", "Network request failed: " + e.getMessage());

            if ((e instanceof SocketTimeoutException || e instanceof ConnectException) && retryCount.get() < MAX_RETRIES) {
                retryCount.incrementAndGet();
                Logger.e("workDao", "Retrying in 10 seconds... (Attempt " + retryCount.get() + " of " + MAX_RETRIES + ")");
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    Logger.e("workDao", "Sleep interrupted: " + ex.getMessage());
                    Thread.currentThread().interrupt();
                }
            } else {
                Logger.e("workDao", "Max retries reached or non-retriable exception. Giving up.");
            }
        }

        private void handleNetworkException(Throwable e, AtomicInteger retryCount) {
            e.printStackTrace();
            Logger.e("workDao", "Network request failed: " + e.getMessage());

            if (retryCount.get() < MAX_RETRIES) {
                retryCount.incrementAndGet();
                Logger.e("workDao", "Retrying in 10 seconds... (Attempt " + retryCount.get() + " of " + MAX_RETRIES + ")");
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    Logger.e("workDao", "Retry was interrupted");
                }
            } else {
                Logger.e("workDao", "Max retries reached for network exception. Giving up.");
            }
        }

        private void infiniteHandleNetworkException(Throwable e, AtomicInteger retryCount) {
            e.printStackTrace();
            Logger.e("workDao", "Network request failed: " + e.getMessage());

            retryCount.incrementAndGet();
            Logger.e("workDao", "Retrying in 10 seconds... (Attempt " + retryCount.get() + ")");

            if (retryCount.get() > MAX_RETRIES) {
                retryCount.set(1); // 重置为1而不是0，因为这已经是新的第一次尝试
                Logger.e("workDao", "Max retries reached. Resetting count and continuing: Attempt " + retryCount.get());
            }

            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                Logger.e("workDao", "Retry was interrupted");
            }
        }}

    public static class TaskManager {

        private final ExecutorService executorService;
        private final ConcurrentHashMap<String, Boolean> taskMap;

        private TaskManager() {
            this.executorService = Executors.newFixedThreadPool(5);
            this.taskMap = new ConcurrentHashMap<>();
        }
        private static class Inner {
            private static final TaskManager instance = new TaskManager();
        }
        public static TaskManager getInstance() {
            return Inner.instance;
        }


        /*
        * 提交一个任务，如果任务ID已经存在，则不执行任务，直接返回
         */
        public void submitTask(Runnable task, String taskId) {
            if (!taskMap.containsKey(taskId)) {
                taskMap.put(taskId, true);
                executorService.submit(() -> {
                    try {
                        task.run();
                    } finally {
                        Logger.d("dx", "submitTask  Task completed: " + taskId);
                        taskMap.remove(taskId);
                    }
                });
            }
        }

        public void shutdown() {
            executorService.shutdown();
        }
    }

    /**
     *  LRU缓存
     */
    public static class LRUCacheManager {

        /**
         *  持久化缓存
         * @param <K>
         * @param <V>
         */

        public static class PersistentLRUCache<K extends Serializable, V extends Serializable> implements Serializable {
            private static final long serialVersionUID = 1L;

            private static final String EXPORT_BASE_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS) + File.separator ;
            private final int maxSize;
            private final String alias;

            private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
            // 使用volatile确保多线程可见性
            // 使用transient确保对象不会被序列化
            private volatile transient LinkedHashMap<K, V> cache;
            // 添加一个版本号用于兼容性检查
            private static final int CACHE_VERSION = 1;

            // 优化的序列化方法
            private void writeObject(ObjectOutputStream oos) throws IOException {
                oos.defaultWriteObject();
                oos.writeInt(CACHE_VERSION);
                oos.writeInt(cache.size());
                for (Map.Entry<K, V> entry : cache.entrySet()) {
                    oos.writeObject(entry.getKey());
                    oos.writeObject(entry.getValue());
                }
            }

            public PersistentLRUCache(int maxSize, String alias) {
                this.maxSize = maxSize;
                this.alias = alias;
                initCache();
                loadFromFile();
            }

            private void initCache() {
                this.cache = new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Entry<K, V> eldest) {
                        return size() > PersistentLRUCache.this.maxSize;
                    }
                };
            }

            public V get(K key) {
                lock.readLock().lock();
                try {
                    return cache.get(key);
                } finally {
                    lock.readLock().unlock();
                }
            }

            public void clear() {
                lock.writeLock().lock();
                try {
                    cache.clear();
                    saveToFile();
                } finally {
                    lock.writeLock().unlock();
                }
            }

            public void remove(K key) {
                cache.remove(key);
                saveToFile();
            }

            public int size() {
                return cache.size();
            }


            public boolean containsKey(K key) {
                return cache.containsKey(key);
            }

            @Override
            public String toString() {
                return cache.toString();
            }
            public LinkedHashMap<K, V> getCache() {
                return cache;
            }

            // 优化的保存方法
            private void saveToFile() {
                String fileName = getFileName();
                lock.writeLock().lock();
                try {
                    ensureDirectoryExists(fileName);
                    Logger.w("saveToFile",fileName +":文件开始保存");
                    try (ObjectOutputStream oos = new ObjectOutputStream(
                            new BufferedOutputStream(
                                    Files.newOutputStream(Paths.get(fileName)), 8192))) {
                        oos.writeObject(this);
                    } catch (IOException e) {
                        Logger.e("workDao", "Failed to save cache to file: " + e.getMessage());
                        e.printStackTrace();
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }

            @SuppressWarnings("unchecked")
            private void loadFromFile() {
                String fileName = getFileName();
                File file = new File(fileName);
                if (!file.exists()) {
                    Logger.w("loadFromFile",fileName +":文件加载失败 file is no exit");
                    return;
                }
                Logger.w("loadFromFile",fileName +":文件加载成功");

                lock.writeLock().lock();
                try {
                    try (ObjectInputStream ois = new ObjectInputStream(
                            new BufferedInputStream(
                                    Files.newInputStream(Paths.get(fileName)), 8192))) {
                        PersistentLRUCache<K, V> loaded = (PersistentLRUCache<K, V>) ois.readObject();
                        this.cache = loaded.cache;
                    Logger.w("loadFromFile","load cache data :"+cache);
                    } catch (Exception e) {
                        Logger.e("workDao", "Failed to load cache from file: " + e.getMessage());
                        e.printStackTrace();
                        initCache(); // 加载失败时初始化新的缓存
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }

            private void ensureDirectoryExists(String fileName) {
                try {
                    Path path = Paths.get(fileName);
                    Path parent = path.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                        Logger.w("ensureDirectoryExists",fileName +":file is no exit");
                    } else {
                        Files.createDirectories(Paths.get(EXPORT_BASE_DIR));
                        Logger.w("ensureDirectoryExists",fileName +":parent is no exit");
                    }
                } catch (IOException e) {
                    Logger.e("workDao", "Failed to create directories: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            private void deleteFile() {
                String fileName = getFileName();
                File file = new File(fileName);
                if (file.exists()) {
                    file.delete();
                }
            }

            // 添加自动保存功能
            private static final long SAVE_INTERVAL = 5 * 60 * 1000; // 5分钟
            private volatile long lastSaveTime;

            private void checkAutoSave() {
                long now = System.currentTimeMillis();
                if (now - lastSaveTime > SAVE_INTERVAL) {
                    saveToFile();
                    lastSaveTime = now;
                }
            }

            public void put(K key, V value) {
                lock.writeLock().lock();
                try {
                    cache.put(key, value);
                    checkAutoSave(); // 使用自动保存替代每次都保存
                } finally {
                    lock.writeLock().unlock();
                }
            }

            // 添加批量操作方法
            public void putAll(Map<? extends K, ? extends V> m) {
                lock.writeLock().lock();
                try {
                    cache.putAll(m);
                    saveToFile();
                } finally {
                    lock.writeLock().unlock();
                }
            }

            // 添加异常处理方法
            private static class CacheException extends RuntimeException {
                public CacheException(String message, Throwable cause) {
                    super(message, cause);
                }
            }

            // 添加文件路径检查
            private void validateFilePath() {
                File baseDir = new File(EXPORT_BASE_DIR);
                if (!baseDir.exists() && !baseDir.mkdirs()) {
                    throw new CacheException("Unable to create cache directory", null);
                }
                if (!baseDir.canWrite()) {
                    throw new CacheException("Cache directory is not writable", null);
                }
            }

            private String getFileName() {

                return EXPORT_BASE_DIR +alias + "_lru_cache.ser";
            }

            private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
                ois.defaultReadObject();
                int version = ois.readInt();
                if (version != CACHE_VERSION) {
                    throw new InvalidClassException("Incompatible cache version");
                }
                initCache();
                int size = ois.readInt();
                for (int i = 0; i < size; i++) {
                    K key = (K) ois.readObject();
                    V value = (V) ois.readObject();
                    cache.put(key, value);
                }


            }
        }


    }

    public static class BufferSizes {
        // 文件操作的推荐缓冲区大小
        private static final int FILE_BUFFER_SIZE = 8192;          // 8KB
        private static final int NETWORK_BUFFER_SIZE = 16384;      // 16KB
        private static final int LARGE_FILE_BUFFER_SIZE = 32768;   // 32KB

        // 不同场景的缓冲区使用
        public void fileOperations() {
            // 普通文件读取
            try (BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream("file.txt"), FILE_BUFFER_SIZE)) {
                // 处理文件
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 网络流处理
            try (BufferedInputStream bis = new BufferedInputStream(
                    new URL("http://example.com").openStream(), NETWORK_BUFFER_SIZE)) {
                // 处理网络数据
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 大文件处理
            try (BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream("large.file"), LARGE_FILE_BUFFER_SIZE)) {
                // 处理大文件
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static class PreFool{
        public static class UiInteraction{
            private long lastClickTime = 0;
            private final String CLICK_TIPS = "请勿频繁点击";
            private final long CLICK_INTERVAL_THRESHOLD = 1000;
            private UiInteraction(){}

            private static class Inner{
                private static final UiInteraction INSTANCE = new UiInteraction();
            }

            public static UiInteraction getInstance(){
                return Inner.INSTANCE;
            }
            public void proofBtn(Button btn, @NonNull Runnable runnable, Runnable errorRun){
                try {
                    btn.setOnClickListener((v) -> {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastClickTime > CLICK_INTERVAL_THRESHOLD) {
                            lastClickTime = currentTime;
                            runnable.run();
                        } else {
                            Logger.e("proofBtn", "点击过快触发防呆");
                            errorRun.run();
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                    Logger.e("proofBtn","proofBtn has a error msg:"+e.getMessage());
                }

            }

        }
    }

    public static class JsonParser {
        private static final String TAG = "JsonParser";
        private final Gson gson;

        private JsonParser() {
            this.gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd HH:mm:ss")
                    .serializeNulls()
                    .create();
        }

        public static JsonParser getInstance() {
            return Inner.INSTANCE;
        }
        private static class Inner {
            private static final JsonParser INSTANCE = new JsonParser();
        }
        /**
         * 将JSONArray解析为List<T>
         * @param json JSONArray数据
         * @param clazz 目标类型Class
         * @param <T> 泛型类型
         * @return 解析后的List<T>
         */
        public <T> List<T> parseJsonArrayToList(JSONArray json, Class<T> clazz) {
            List<T> list = new ArrayList<>();
            if (json == null) {
                return list;
            }

            try {
                Type listType = TypeToken.getParameterized(List.class, clazz).getType();
                list = gson.fromJson(json.toString(), listType);
            } catch (JsonSyntaxException e) {
                Log.e(TAG, "JSON syntax error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Parse error: " + e.getMessage());
            }

            return list;
        }

        /**
         * 将JSON字符串解析为List<T>
         * @param jsonString JSON字符串
         * @param clazz 目标类型Class
         * @param <T> 泛型类型
         * @return 解析后的List<T>
         */
        public <T> List<T> parseJsonStringToList(String jsonString, Class<T> clazz) {
            try {
                JSONArray jsonArray = new JSONArray(jsonString);
                return parseJsonArrayToList(jsonArray, clazz);
            } catch (JSONException e) {
                Log.e(TAG, "Invalid JSON string: " + e.getMessage());
                return new ArrayList<>();
            }
        }

        /**
         * 将对象转换为JSON字符串
         * @param object 要转换的对象
         * @return JSON字符串
         */
        public String toJsonString(Object object) {
            try {
                return gson.toJson(object);
            } catch (Exception e) {
                Log.e(TAG, "Convert to JSON error: " + e.getMessage());
                return "";
            }
        }

        /**
         * 将JSON字符串转换为对象
         * @param jsonString JSON字符串
         * @param clazz 目标类型Class
         * @param <T> 泛型类型
         * @return 转换后的对象
         */
        public <T> T parseJsonToObject(String jsonString, Class<T> clazz) {
            try {
                Type type = TypeToken.get(clazz).getType();
                return gson.fromJson(jsonString, type);
            } catch (Exception e) {
                Log.e(TAG, "Parse JSON to object error: " + e.getMessage());
                return null;
            }
        }
    }

    public static class FileUtils{
        public static String getFilePathFromUri(Context context, Uri uri) {
            String filePath = null;

            try {
                // 针对 Android 10 (Q) 及以上版本
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
                        ContentResolver contentResolver = context.getContentResolver();
                        Cursor cursor = contentResolver.query(uri, null, null, null, null);

                        if (cursor != null && cursor.moveToFirst()) {
                            // 获取文件名
                            @SuppressLint("Range") String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            cursor.close();

                            // 将文件复制到应用私有目录
                            File destFile = new File(context.getFilesDir(), displayName);
                            try (InputStream is = contentResolver.openInputStream(uri);
                                 OutputStream os = Files.newOutputStream(destFile.toPath())) {
                                if (is != null) {
                                    byte[] buffer = new byte[4096];
                                    int read;
                                    while ((read = is.read(buffer)) != -1) {
                                        os.write(buffer, 0, read);
                                    }
                                    os.flush();
                                    filePath = destFile.getAbsolutePath();
                                }
                            }
                        }
                    }
                }
                // 针对 Android 9 (P) 及以下版本
                else {
                    if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
                        String[] projection = {MediaStore.Images.Media.DATA};
                        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

                        if (cursor != null) {
                            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                            if (cursor.moveToFirst()) {
                                filePath = cursor.getString(column_index);
                            }
                            cursor.close();
                        }
                    }
                    // 如果是 file:// URI
                    else if (uri.getScheme().equals("file")) {
                        filePath = uri.getPath();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return filePath;
        }
        /**
         * 保存图片到本地
         * @param context 上下文
         * @param bitmap 要保存的图片
         * @param fileName 文件名(可选)
         * @return 返回保存的文件路径，保存失败返回null
         */
        public static String saveImageToLocal(Context context, Bitmap bitmap, @Nullable String fileName) {
            if (bitmap == null || context == null) {
                return null;
            }

            try {
                // 生成文件名
                if (fileName == null || fileName.isEmpty()) {
                    fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
                }
                if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg")) {
                    fileName += ".jpg";
                }

                // Android 10及以上使用MediaStore
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    return saveImageWithMediaStore(context, bitmap, fileName);
                } else {
                    return saveImageLegacy(context, bitmap, fileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * Android 10+ 使用 MediaStore 保存图片
         */
        @RequiresApi(api = Build.VERSION_CODES.Q)
        private static String saveImageWithMediaStore(Context context, Bitmap bitmap, String fileName) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/kc_store");

            ContentResolver resolver = context.getContentResolver();
            Uri uri = null;
            String filePath = null;

            try {
                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = resolver.openOutputStream(uri)) {
                        if (os != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                            os.flush();
                            filePath = getPathFromUri(context, uri);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (uri != null) {
                    resolver.delete(uri, null, null);
                }
                return null;
            }

            return filePath;
        }

        /**
         * Android 10以下版本保存图片
         */
        private static String saveImageLegacy(Context context, Bitmap bitmap, String fileName) {
            // 检查存储权限
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }

            File directory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "AppName");

            if (!directory.exists() && !directory.mkdirs()) {
                return null;
            }

            File file = new File(directory, fileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                return file.getAbsolutePath();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * 通用的文件保存方法
         * @param context 上下文
         * @param inputStream 输入流
         * @param fileName 文件名
         * @param mimeType MIME类型
         * @return 保存的文件路径
         */
        public static String saveFile(Context context, InputStream inputStream,
                                      String fileName, String mimeType) {
            if (inputStream == null || context == null || fileName == null) {
                return null;
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    return saveFileWithMediaStore(context, inputStream, fileName, mimeType);
                } else {
                    return saveFileLegacy(context, inputStream, fileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //删除文件的方法
        public static boolean deleteFile(String filePath) {
            try {
                File file = new File(filePath);
                if (file.exists() && file.delete()) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.Q)
        private static String saveFileWithMediaStore(Context context, InputStream inputStream,
                                                     String fileName, String mimeType) throws Exception {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AppName");

            ContentResolver resolver = context.getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return null;

            try (OutputStream os = resolver.openOutputStream(uri)) {
                if (os != null) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    os.flush();
                    return getPathFromUri(context, uri);
                }
            } catch (Exception e) {
                resolver.delete(uri, null, null);
                throw e;
            }
            return null;
        }

        private static String saveFileLegacy(Context context, InputStream inputStream,
                                             String fileName) throws IOException {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }

            File directory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "AppName");

            if (!directory.exists() && !directory.mkdirs()) {
                return null;
            }

            File file = new File(directory, fileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.flush();
                return file.getAbsolutePath();
            }
        }

        private static String getPathFromUri(Context context, Uri uri) {
            String[] projection = {MediaStore.MediaColumns.DATA};
            Cursor cursor = context.getContentResolver().query(uri, projection,
                    null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        return cursor.getString(0);
                    }
                } finally {
                    cursor.close();
                }
            }
            return null;
        }

        //实现目录创建的方法
        public static void createDir(final String dirPath) {
            ThreadPoolManager.getInstance().execute(() -> {
                try {

                    File dir = new File(dirPath);
                    if (!dir.exists()) {
                        File parentDir = dir.getParentFile();
                        if (parentDir != null) {
                            if (!parentDir.exists()) {
                                parentDir.mkdirs();
                            }
                        }
                        dir.mkdir();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    Log.e("dxlxx","createDir has a error:"+e.getMessage() +" |file Name"+dirPath);
                }
            });
        }

        //采用子目录英文转化功能
        public static void createDirEnConverse(final String pDirPath,String subDirName) {
            ThreadPoolManager.getInstance().execute(() -> {
                try {
                    //提取
                    String converseDir = pDirPath ;
                    File dir = new File(converseDir);
                    if (!dir.exists()) {
                        File parentDir = dir.getParentFile();
                        if (parentDir != null) {
                            if (!parentDir.exists()) {
                                parentDir.mkdirs();
                            }
                        }
                        dir.mkdir();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    Log.e("dxlxx","createDir has a error:"+e.getMessage() +" |file Name"+subDirName);
                }
            });
        }
        
        public static class ZipUtils {
            private static final int BUFFER_SIZE = 1024 * 2024; // 8KB buffer size
            private static final int COMPRESSION_LEVEL_SPEED = Deflater.BEST_SPEED;
            private static final int COMPRESSION_LEVEL_COMPRESSION = Deflater.BEST_COMPRESSION;


            /**
             * 异步压缩文件
             */
            public static CompletableFuture<File> zipFoldersAsync(List<File> folders, String zipPath) {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        return zipFolders(folders, zipPath);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                });
            }

            /**
             * 使用进度监听器的压缩方法
             */
            public interface ZipProgressListener {
                void onProgress(long bytesProcessed, long totalBytes);
                void onComplete(File zipFile);
                void onError(Exception e);
            }

            public static void zipFoldersWithProgress(List<File> folders, String zipPath, ZipProgressListener listener) {
                new Thread(() -> {
                    try {
                        // 计算总大小
                        long totalSize = calculateTotalSize(folders);
                        AtomicLong processedSize = new AtomicLong(0);

                        try (FileOutputStream fos = new FileOutputStream(zipPath);
                             BufferedOutputStream bos = new BufferedOutputStream(fos);
                             ZipOutputStream zos = new ZipOutputStream(bos)) {

                            zos.setLevel(Deflater.BEST_COMPRESSION);
                            byte[] buffer = new byte[BUFFER_SIZE];

                            for (File folder : folders) {
                                addToZipWithProgress(folder, "", zos, buffer, processedSize,
                                        totalSize, listener);
                            }

                            File zipFile = new File(zipPath);
                            listener.onComplete(zipFile);
                        }
                    } catch (Exception e) {
                        listener.onError(e);
                    }
                }).start();
            }

            private static void addToZipWithProgress(File file, String parentPath,
                                                     ZipOutputStream zos, byte[] buffer, AtomicLong processedSize,
                                                     long totalSize, ZipProgressListener listener) throws IOException {

                String entryPath = parentPath.isEmpty() ?
                        file.getName() :
                        parentPath + File.separator + file.getName();

                if (file.isDirectory()) {
                    File[] files = file.listFiles();
                    if (files != null) {
                        for (File child : files) {
                            addToZipWithProgress(child, entryPath, zos, buffer,
                                    processedSize, totalSize, listener);
                        }
                    }
                } else {
                    try (FileInputStream fis = new FileInputStream(file);
                         BufferedInputStream bis = new BufferedInputStream(fis)) {

                        ZipEntry entry = new ZipEntry(entryPath);
                        zos.putNextEntry(entry);

                        int count;
                        while ((count = bis.read(buffer)) != -1) {
                            zos.write(buffer, 0, count);
                            processedSize.addAndGet(count);
                            listener.onProgress(processedSize.get(), totalSize);
                        }
                        zos.closeEntry();
                    }
                }
            }

            public static File zipFolders(List<File> folders, String zipPath) throws IOException {
                if (folders == null || folders.isEmpty() || zipPath == null) {
                    throw new IllegalArgumentException("Invalid input parameters");
                }

                File zipFile = new File(zipPath);
                Objects.requireNonNull(zipFile.getParentFile()).mkdirs();

                try (FileOutputStream fos = new FileOutputStream(zipFile);
                     BufferedOutputStream bos = new BufferedOutputStream(fos);
                     ZipOutputStream zos = new ZipOutputStream(bos)) {

                    // 计算总文件大小
                    long totalSize = calculateTotalSize(folders);

                    // 动态设置压缩级别
                    int compressionLevel = determineCompressionLevel(totalSize);
                    zos.setLevel(compressionLevel);

                    Logger.d("ZipUtils", "Total size: " + totalSize + " bytes, using compression level: " + compressionLevel);

                    byte[] buffer = new byte[8192];
                    for (File folder : folders) {
                        addToZip(folder, "", zos, buffer);
                    }
                }

                return zipFile;
            }

            /**
             * 计算所有文件夹的总大小
             */
            private static long calculateTotalSize(List<File> folders) {
                return folders.stream()
                        .mapToLong(folder -> calculateFolderSize(folder))
                        .sum();
            }

            /**
             * 计算单个文件夹大小
             */
            private static long calculateFolderSize(File folder) {
                if (!folder.isDirectory()) {
                    return folder.length();
                }

                File[] files = folder.listFiles();
                if (files == null) return 0;

                return Arrays.stream(files)
                        .mapToLong(file -> file.isDirectory() ? calculateFolderSize(file) : file.length())
                        .sum();
            }

            /**
             * 根据总大小确定压缩级别
             */
            private static int determineCompressionLevel(long totalSize) {
                // 定义大小阈值（单位：字节）
                final long SMALL_FILE_THRESHOLD = 10 * 1024 * 1024;    // 10MB
                final long MEDIUM_FILE_THRESHOLD = 50 * 1024 * 1024;   // 50MB
                final long LARGE_FILE_THRESHOLD = 100 * 1024 * 1024;   // 100MB

                if (totalSize <= SMALL_FILE_THRESHOLD) {
                    // 小文件使用最佳压缩
                    return Deflater.BEST_COMPRESSION; // 9
                } else if (totalSize <= MEDIUM_FILE_THRESHOLD) {
                    // 中等文件使用默认压缩级别
                    return 6;
                } else if (totalSize <= LARGE_FILE_THRESHOLD) {
                    // 较大文件使用较快压缩
                    return 3;
                } else {
                    // 超大文件使用最快压缩
                    return Deflater.BEST_SPEED; // 1
                }
            }

            /**
             * 添加文件到ZIP
             */
            private static void addToZip(File file, String parentPath, ZipOutputStream zos, byte[] buffer)
                    throws IOException {
                String entryPath = parentPath.isEmpty() ?
                        file.getName() :
                        parentPath + File.separator + file.getName();

                if (file.isDirectory()) {
                    File[] files = file.listFiles();
                    if (files != null) {
                        for (File child : files) {
                            addToZip(child, entryPath, zos, buffer);
                        }
                    }
                } else {
                    // 跳过过大的单个文件
                    if (file.length() > 200 * 1024 * 1024) { // 200MB
                        Logger.w("ZipUtils", "Skipping large file: " + file.getPath());
                        return;
                    }

                    try (FileInputStream fis = new FileInputStream(file);
                         BufferedInputStream bis = new BufferedInputStream(fis)) {

                        ZipEntry entry = new ZipEntry(entryPath);
                        entry.setTime(file.lastModified());
                        zos.putNextEntry(entry);

                        int count;
                        while ((count = bis.read(buffer)) != -1) {
                            zos.write(buffer, 0, count);
                        }
                        zos.closeEntry();
                    }
                }
            }


        }
    }

    public static class ViewResourceHelper {
        private static final String TAG = "ViewResourceHelper";

        private static final String RESOURCE_ID_CODE = "mTextId";
        //反射获取资源id
        public static int getTextResourceId(TextView view) {
            try {
                @SuppressLint("SoonBlockedPrivateApi") Field field = TextView.class.getDeclaredField(RESOURCE_ID_CODE);
                field.setAccessible(true);
                return field.getInt(view);
            } catch (Exception e) {
                Logger.w(TAG, "Failed to get resource id: " + e.getMessage());
                return 0;
            }
        }
    }

    public static class ViewUi {
        public static final int DEFAULT_DOWN_H = 5;//默认高度为屏幕高度的1/5

        /**
         * 获取屏幕高度
         *
         * @param context
         * @param denominator
         */
        public static int getParameterScreenHeight(Context context, int denominator) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int screenHeight = displayMetrics.heightPixels; // 获取屏幕高度
            return screenHeight / denominator; // 返回屏幕高度的 1/N
        }

        /**
         * 获取屏幕宽度
         *
         * @param context
         * @param denominator
         */
        public static int getParameterScreenWeight(Context context, int denominator) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int widthPixels = displayMetrics.widthPixels; // 获取屏幕高度
            return widthPixels / denominator; // 返回屏幕宽度的 1/N
        }



        /**
         * 获取屏幕宽度
         *
         * @param context
         * @param denominator
         */
        public static int getAdjustParameterScreen(Context context, int denominator) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int widthPixels = displayMetrics.widthPixels; // 获取屏幕高度
            int screenHeight = displayMetrics.heightPixels; // 获取屏幕高度
            int adjustValue = Math.min(widthPixels, screenHeight);
            return adjustValue / denominator; // 返回屏幕宽度的 1/N
        }


        /**
         * 获取设置自适应按钮字体大小
         *
         * @param button
         */
        public static void adjustButtonTextSize(Button button) {
            // 获取按钮的宽度和高度
            int width = button.getWidth();
            int height = button.getHeight();

            // 如果按钮的宽度或高度为 0，返回
            if (width == 0 || height == 0) {
                button.post(() -> adjustButtonTextSize(button)); // 在布局完成后再次调用
                return;
            }

            // 计算适合的字体大小
            float textSize = Math.min(width, height) / 4f; // 这里的 5f 是一个比例，可以根据需要调整
            button.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize); // 设置字体大小
        }

        public static class LayoutAdjust {
            public static void setThreeAndTwoDg(Dialog dg) {
                WindowManager windowManager = (WindowManager) dg.getContext().getSystemService(Context.WINDOW_SERVICE);
                DisplayMetrics displayMetrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                int dialogH = (int) (displayMetrics.heightPixels * 2.0f / 3.0f);
                Window window = dg.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams layoutParams = window.getAttributes();
                    layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
                    layoutParams.height = dialogH;
                    // 设置对话框显示在屏幕中央
                    window.setGravity(Gravity.CENTER);
                    // 应用参数
                    window.setAttributes(layoutParams);
                    // 设置背景半透明
                    window.setDimAmount(0.5f);
                }
            }

            public static void setCustomerScaleDg(Dialog dg, float scale) {
                WindowManager windowManager = (WindowManager) dg.getContext().getSystemService(Context.WINDOW_SERVICE);
                DisplayMetrics displayMetrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                int dialogH = (int) (displayMetrics.heightPixels * scale);
                Window window = dg.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams layoutParams = window.getAttributes();
                    layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
                    layoutParams.height = dialogH;
                    // 设置对话框显示在屏幕中央
                    window.setGravity(Gravity.CENTER);
                    // 应用参数
                    window.setAttributes(layoutParams);
                    // 设置背景半透明
                    window.setDimAmount(0.5f);
                }
            }
        }
    }

    public static class Data{
        /**
         * 获取当天的起始时间戳
         */
        public static long getStartTime() {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTimeInMillis();
        }

        /**
         * 获取当天的结束时间戳
         */
        public static long getEndTime() {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            return calendar.getTimeInMillis();
        }

    }

    public static class RandomUtils {
        /**
         * 获取随机字符串
         *
         * @param length 定义的字串长度
         * @return
         */
        public static String getRandomString(int length) {
            StringBuilder sb = new StringBuilder();
            Random random = new Random();
            for (int i = 0; i < length; i++) {
                int number = random.nextInt(10);
                sb.append(number);
            }
            return sb.toString();
        }
    }

    public static class HttpRequestException extends Exception {
        public static final int NETWORK_ERROR = 1;
        public static final int SERVER_ERROR = 2;
        public static final int CLIENT_ERROR = 3;

        private int errorType;

        public HttpRequestException(String message, int errorType) {
            super(message);
            this.errorType = errorType;
        }

        public int getErrorType() {
            return errorType;
        }
    }

    public static class InterCheck{
        public static boolean interCheck(Context context){
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            if (!isConnected) {
                Logger.e("NetworkError", "No internet connection");
            }
            return isConnected;
        }

    }


    /**
     * 异常处理工具类
     */
    public static class ExceptionHandler {
        private static final String TAG = "ExceptionHandler";

        /**
         * 异常类型枚举
         */
        public enum ErrorType {
            NETWORK,    // 网络错误
            HTTP,       // HTTP错误
            SERVER,     // 服务器错误
            PARSE,      // 解析错误
            BUSINESS,   // 业务错误
            UNKNOWN     // 未知错误
        }

        /**
         * 异常处理结果
         */
        public static class ExceptionResult {
            private final ErrorType type;
            private final int code;
            private final String message;
            private final Exception exception;
            private final boolean needRetry;

            public ExceptionResult(ErrorType type, int code, String message,
                                   Exception exception, boolean needRetry) {
                this.type = type;
                this.code = code;
                this.message = message;
                this.exception = exception;
                this.needRetry = needRetry;
            }

            public ErrorType getType() {
                return type;
            }

            public int getCode() {
                return code;
            }

            public String getMessage() {
                return message;
            }

            public Exception getException() {
                return exception;
            }

            public boolean isNeedRetry() {
                return needRetry;
            }
        }

        /**
         * 异常处理接口
         */
        public interface ExceptionHandlerCallback {
            void onError(ExceptionResult result);
            void onRetry();
        }

        /**
         * 处理异常
         */
        public static ExceptionResult handle(Exception e,
                                             ExceptionHandlerCallback callback
                                            ) {
            ExceptionResult result = null;

            if (e instanceof UnknownHostException) {
                result = new ExceptionResult(
                        ErrorType.NETWORK,
                        0,
                        "网络连接失败，请检查网络设置",
                        e,
                        true
                );
            } else if (e instanceof SocketTimeoutException) {
                result = new ExceptionResult(
                        ErrorType.NETWORK,
                        0,
                        "网络连接超时，请稍后重试",
                        e,
                        true
                );
            } else if (e instanceof ConnectException) {
                result = new ExceptionResult(
                        ErrorType.NETWORK,
                        0,
                        "服务器连接失败",
                        e,
                        true
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 7) {
                if (e instanceof HttpException) {
                    HttpException httpException = (HttpException) e;
                    int code = httpException.hashCode();
                    ErrorType type;
                    String message;
                    boolean needRetry;

                    switch (code) {
                        case 401:
                            type = ErrorType.HTTP;
                            message = "登录已过期，请重新登录";
                            needRetry = false;
                            break;
                        case 403:
                            type = ErrorType.HTTP;
                            message = "没有访问权限";
                            needRetry = false;
                            break;
                        case 404:
                            type = ErrorType.HTTP;
                            message = "请求的资源不存在";
                            needRetry = false;
                            break;
                        case 500:
                            type = ErrorType.SERVER;
                            message = "服务器内部错误";
                            needRetry = true;
                            break;
                        case 502:
                            type = ErrorType.SERVER;
                            message = "服务器维护中";
                            needRetry = true;
                            break;
                        case 503:
                            type = ErrorType.SERVER;
                            message = "服务暂时不可用";
                            needRetry = true;
                            break;
                        default:
                            type = ErrorType.HTTP;
                            message = "请求失败(" + code + ")";
                            needRetry = false;
                    }

                    result = new ExceptionResult(type, code, message, e, needRetry);
                } }else if (e instanceof JsonParseException || e instanceof JsonSyntaxException) {
                    result = new ExceptionResult(
                            ErrorType.PARSE,
                            0,
                            "数据解析错误",
                            e,
                            false
                    );
                } else if (e instanceof BusinessException) {
                    BusinessException businessException = (BusinessException) e;
                    result = new ExceptionResult(
                            ErrorType.BUSINESS,
                            businessException.getCode(),
                            businessException.getMessage(),
                            e,
                            false
                    );
                } else {
                    result = new ExceptionResult(
                            ErrorType.UNKNOWN,
                            0,
                            "系统异常，请稍后重试",
                            e,
                            true
                    );
                }
            // 处理异常结果
            handleResult(result, callback);
            return result;
        }

        /**
         * 处理异常结果
         */
        private static void handleResult(ExceptionResult result,
                                         ExceptionHandlerCallback callback
                                        ) {
            // 1. 记录日志
            logError(result);

            // 2. 显示错误提示
            showError(result);

            // 3. 回调通知
            if (callback != null) {
                callback.onError(result);
            }

            // 4. 错误上报
            reportError(result);

            // 5. 处理重试
            if (result.isNeedRetry() && callback != null) {
                callback.onRetry();
            }
        }

        /**
         * 记录错误日志
         */
        private static void logError(ExceptionResult result) {
            StringBuilder logMessage = new StringBuilder()
                    .append("【").append(result.getType()).append("】");
            if (result.getCode() != 0) {
                logMessage.append(" Code:").append(result.getCode());
            }
            logMessage.append(" Message:").append(result.getMessage());

            switch (result.getType()) {
                case BUSINESS:
                    Logger.i(TAG, logMessage.toString());
                    break;
                case NETWORK:
                    Logger.w(TAG, logMessage.toString());
                    break;
                default:
                    Logger.e(TAG, logMessage.toString()+"\n"+result.getException()); // 默认为异常
                    break;
            }
        }

        /**
         * 显示错误提示
         */
        private static void showError(ExceptionResult result) {

            switch (result.getType()) {
                case NETWORK:
                case HTTP:
                case SERVER:
                case BUSINESS:
                  Initializer.showToast(result.getMessage());
                    break;
                default:
                    if (BuildConfig.DEBUG) {
                        Initializer.showToast(result.getMessage() +
                                "(" + result.getException().getClass().getSimpleName() + ")");
                    } else {
//                        Initializer.showToast(result.getMessage());
                        //不提示
                    }
                    break;
            }
        }

        /**
         * 错误上报
         */
        private static void reportError(ExceptionResult result) {
            switch (result.getType()) {
                case UNKNOWN:
                    // 未知错误需要上报
                    // Crashlytics.logException(result.getException());
                    break;
                case SERVER:
                    // 服务器错误需要上报
                    // 可以上报到自己的服务器
                    break;
            }
        }


    }

    /**
     * 业务异常类
     */
    public class BusinessException extends Exception {
        private final int code;

        public BusinessException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * 全局初始化类
     */
    public static class Initializer {
        public static final Gson gson = new Gson();
        public static Context context;
        private final static Handler mainHandler = new Handler(Looper.getMainLooper());

        /**
         * 显示Toast
         */
        public static void showToast(String message) {
            if (Initializer.context == null) {
                return;
            }
            Initializer.mainHandler(()->Toast.makeText(Initializer.context, message, Toast.LENGTH_SHORT).show());
        }

        /**
         * 主线程处理器
         */

        public static void mainHandler(Executor executor) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                executor.execute();
            }else {
                Initializer.mainHandler.post(()->executor.execute());
            }
        }

        public static void mainHandler(Executor executor, long delay) {
            Initializer.mainHandler.postDelayed(()->executor.execute(), delay);

        }

        /**
         * 执行器
         */
        public interface Executor {
            void execute();
        }

    }

    public static class CrashHandler implements Thread.UncaughtExceptionHandler {
        private static final String TAG = "CrashHandler";
        private Context mContext;
        private Thread.UncaughtExceptionHandler mDefaultHandler;

        //创建一个单线程池，用于处理异常
        private final ExecutorService executorService = Executors.newSingleThreadExecutor();
        // 严重异常类型标记
        private final String CRITICAL_EXCEPTION_TYPE = "OutOfMemoryError";
        private final String CRITICAL_EXCEPTION_TYPE2 = "StackOverflowError";
        private final String CRITICAL_EXCEPTION_TYPE3 = "RuntimeException";
        private final String CRITICAL_EXCEPTION_TYPE4 = "NullPointerException";
        private final String CRITICAL_EXCEPTION_TYPE5 = "SecurityException";
        private final String CRITICAL_EXCEPTION_TYPE6 = "IllegalStateException";

        private final Set<String> criticalExceptions = new HashSet<>(Arrays.asList(
                CRITICAL_EXCEPTION_TYPE, CRITICAL_EXCEPTION_TYPE2, CRITICAL_EXCEPTION_TYPE3,
                CRITICAL_EXCEPTION_TYPE5, CRITICAL_EXCEPTION_TYPE6));

        private CrashHandler() {
        }

        private static class Inner {
            private static final CrashHandler INSTANCE = new CrashHandler();
        }
        public static CrashHandler getInstance() {
            return Inner.INSTANCE;
        }


        public void init(Context context) {
            mContext = context.getApplicationContext(); // 使用ApplicationContext避免内存泄漏
            mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(this);
        }

        public boolean isInit(){
            return mContext != null;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            executorService.execute(() -> {
//todo 不采用系统                if (mContext == null || ex == null) {
//                    if (mDefaultHandler != null ) {
//                        if (ex != null){
//                            mDefaultHandler.uncaughtException(thread, ex);
//                        }
//                    }
//                    return;
//                }

                boolean handled = handleException(ex);

                // 无论是否处理，都记录到系统日志
                Log.e(TAG, "Uncaught exception in thread " + thread.getName(), ex);

//  todo 不处理              if (!handled && mDefaultHandler != null) {
//                    mDefaultHandler.uncaughtException(thread, ex);
//                } else {
//                    try {
//                        Thread.sleep(1000); // 给日志写入时间，但不要太长
//                    } catch (InterruptedException e) {
//                        Log.e(TAG, "Error during crash handling: ", e);
//                    }
//
//                    // 考虑是否重启应用而不是直接退出
//                    // restartApp();
//                    // 判断异常等级如果不是危险异常则不杀死应用
//                    if (!isCriticalException(ex)) {
//                        return;
//                    }
//                    //释放 线程池
//                    executorService.shutdown();
//                    android.os.Process.killProcess(android.os.Process.myPid());
//                    System.exit(1);
//                }
            });
        }

        private boolean handleException(Throwable ex) {
            try {
                // 收集设备参数信息
                Map<String, String> deviceInfo = collectDeviceInfo(mContext);

                // 检查是否严重异常并标记
                boolean isCritical = isCriticalException(ex);

                // 立即在主线程记录关键信息，确保不会丢失
                String exceptionType = ex.getClass().getSimpleName();
                String exceptionMessage = ex.getMessage();
                Log.e(TAG, (isCritical ? "❌ CRITICAL EXCEPTION1: " : "⚠️ EXCEPTION: ") +
                        exceptionType + " - " + exceptionMessage);

                // 异步保存详细日志
                final Throwable finalEx = ex;
                final boolean finalIsCritical = isCritical;
                final Map<String, String> finalDeviceInfo = deviceInfo;
                String  path =  saveCrashInfo2File(finalEx, finalIsCritical, finalDeviceInfo,  null);
                    // 可以添加上传逻辑
//                    uploadCrashReport(fileName, finalIsCritical);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error in handleException", e);
                return false;
            }
        }

        public boolean handleException(Throwable ex,String className) {
            try {
                executorService.execute(() -> {
                // 收集设备参数信息
                Map<String, String> deviceInfo = collectDeviceInfo(mContext);

                // 检查是否严重异常并标记
                boolean isCritical = isCriticalException(ex);

                // 立即在主线程记录关键信息，确保不会丢失
                String exceptionType = ex.getClass().getSimpleName();
                String exceptionMessage = ex.getMessage();
                Log.e(TAG, (isCritical ? "❌ CRITICAL EXCEPTION2: " : "⚠️ EXCEPTION: ") +
                        exceptionType + " - " + exceptionMessage + "  className:"+className);

                // 异步保存详细日志
                final Throwable finalEx = ex;
                final boolean finalIsCritical = isCritical;
                final Map<String, String> finalDeviceInfo = deviceInfo;

                    String  path =  saveCrashInfo2File(finalEx, finalIsCritical, finalDeviceInfo,null);
                    // 可以添加上传逻辑
                    if (path != null) {
                    }
                });
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error in handleException", e);
                return false;
            }
        }

        public boolean handleException(String tips,Throwable ex) {
            try {
                // 收集设备参数信息
                Map<String, String> deviceInfo = collectDeviceInfo(mContext);

                // 检查是否严重异常并标记
                boolean isCritical = isCriticalException(ex);

                // 立即在主线程记录关键信息，确保不会丢失
                String exceptionType = ex.getClass().getSimpleName();
                String exceptionMessage = ex.getMessage();
                Log.e(TAG, (isCritical ? "❌ CRITICAL EXCEPTION3: " : "⚠️ EXCEPTION: ") +
                        exceptionType + " - " + exceptionMessage + "  Tps:"+tips);

                // 异步保存详细日志
                final Throwable finalEx = ex;
                final boolean finalIsCritical = isCritical;
                final Map<String, String> finalDeviceInfo = deviceInfo;
                executorService.execute(() -> {
                    String  path =  saveCrashInfo2File(finalEx, finalIsCritical, finalDeviceInfo,tips);
                    // 可以添加上传逻辑
//                    uploadCrashReport(fileName, finalIsCritical);
                });
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error in handleException", e);
                return false;
            }
        }

        private String saveCrashInfo2File(Throwable ex, boolean isCritical, Map<String, String> deviceInfo, @Nullable String tips) {
            StringBuilder sb = new StringBuilder();

            // 添加严重性标记
            sb.append(isCritical ? "❌ CRITICAL CRASH REPORT ❌\n\n" : "⚠️ CRASH REPORT ⚠️\n\n");

            // 添加设备信息
            for (Map.Entry<String, String> entry : deviceInfo.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            sb.append("\n");

            // 添加异常信息
            //添加对应的堆栈信息包括行
        // 获取完整堆栈并打印
            StackTraceElement[] stackTrace = ex.getStackTrace();
            StringBuilder stackTraceString = new StringBuilder();
            for (StackTraceElement element : stackTrace) {
                stackTraceString.append("\tat ").
                        append(element.getClassName()).
                        append(".").
                        append(element.getMethodName()).
                        append("(").
                        append(element.getMethodName()).
                        append("(").
                        append(element.getFileName()).
                        append(":").
                        append(element.getLineNumber()).
                        append(")\n");
            }

            sb.append("Exception: ").append(ex.getClass().getName()).append("\n");
            sb.append("Message: ").append(ex.getMessage()).append("\n");
            if (tips != null){
                sb.append("Tips: ").append(tips).append("\n");
            }
            sb.append("stackTraceString: ").append(stackTraceString).append("\n\n");


            // 添加堆栈跟踪
            sb.append("Stack Trace:\n");
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            ex.printStackTrace(printWriter);
            Throwable cause = ex.getCause();
            while (cause != null) {
                sb.append("\nCaused by: ");
                cause.printStackTrace(printWriter);
                cause = cause.getCause();
            }
            printWriter.close();
            sb.append(writer.toString());

            // 保存文件
            String string  = sb.toString();
            try {
                String severity = isCritical ? "CRITICAL_" : "";
                //名称按时间区分
                String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
                String fileName = severity + "crash_" + "esop_"+ time + ".log";

                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/esop_crash_logs";
                File dir = new File(path);
                if (!dir.exists()) {
                 boolean isCreate =  dir.mkdirs();
                 if (!isCreate){
                     Logger.e(TAG, "创建文件夹失败");
                     return null;
                 }
                 Logger.e(TAG, "创建文件夹成功");

                }
                //将string 采用缓冲流写入到文件中
                FileOutputStream fos = new FileOutputStream(path + File.separator + fileName,true);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

                // 添加分隔符和时间
                bw.write("============================================\n");
                bw.write("Crash Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n");
                bw.write("============================================\n\n");

                bw.write(string);
                bw.write("\n\n");

                bw.flush();
                bw.close();
                return path + fileName;
            }catch (Exception e){
                e.printStackTrace();
                Logger.etf(TAG, "创建文件失败2",  e);
                return null;
            }
        }

        private boolean isCriticalException(Throwable ex) {
            String exceptionName = ex.getClass().getSimpleName();
            if (criticalExceptions.contains(exceptionName)) {
                return true;
            }

            // 检查关键堆栈信息
// //todo 保护程序           StackTraceElement[] stackTrace = ex.getStackTrace();
//            if (stackTrace != null && stackTrace.length > 0) {
//                // 检查是否在主要组件中发生
//                for (StackTraceElement element : stackTrace) {
//                    if (element.getClassName().contains("Activity") ||
//                            element.getClassName().contains("Fragment") ||
//                            element.getClassName().contains("Application")) {
//                        return true;
//                    }
//                }
//            }

            return false;
        }

        private Map<String, String> collectDeviceInfo(Context ctx) {
            Map<String, String> info = new HashMap<>();
            try {
                PackageManager pm = ctx.getPackageManager();
                PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);

                info.put("App Version", pi.versionName + " (" + pi.versionCode + ")");
                info.put("OS Version", Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")");
                info.put("Device", Build.MANUFACTURER + " " + Build.MODEL);
                info.put("Memory", getAvailableMemory(ctx));
                info.put("Time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date()));

                // 添加更多设备信息...
            } catch (Exception e) {
                Log.e(TAG, "Error collecting device info", e);
            }
            return info;
        }

        private String getAvailableMemory(Context context) {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            return (mi.availMem / 1048576L) + "MB / " + (mi.totalMem / 1048576L) + "MB";
        }

    }
    public static void hintKeyboard(View v) {
        if (v == null || Initializer.context == null ){
            return;
        }
        InputMethodManager imm = (InputMethodManager) Initializer.context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }


    //dx add for en dir
    public static class DirectoryMapper {
        private static final String SP_KEY_DIR_MAPPING = "en_directory_mapping";
        private static final String EXPORT_PREFIX = "ch_to_en_";
        private  Map<String, String> dirMapping; // 原始名称 -> 映射名称
        private SharedPreferences sp;
        private  Gson gson;

        // 单例模式
        private DirectoryMapper(){}

        public static class DirectoryMapperHolder {
            private static final DirectoryMapper instance = new DirectoryMapper();
        }

        public static DirectoryMapper getInstance() {
            return DirectoryMapperHolder.instance;
        }


        private void initDirectoryMapper(Context context) {
            sp = context.getSharedPreferences("directory_mapper", Context.MODE_PRIVATE);
            gson = new Gson();
            dirMapping = loadMapping();
        }

        /**
         * 获取原始目录名
         * @param mappedName 映射名称
         * @return 原始目录名，如果不存在返回null
         */
        public String getOriginalDirectoryName(String mappedName) {
            // 如果是映射名称，查找原始名称
            for (Map.Entry<String, String> entry : dirMapping.entrySet()) {
                if (entry.getValue().equals(mappedName)) {
                    return entry.getKey();
                }
            }
            // 如果找不到映射，说明可能是英文目录名，直接返回
            return mappedName;
        }

        /**
         * 检查字符串是否只包含英文字符、数字和基本符号
         */
        private boolean isEnglishOnly(String text) {
            return text.matches("^[a-zA-Z0-9_.-]+$");
        }

        /**
         * 从SharedPreferences加载映射关系
         */
        private Map<String, String> loadMapping() {
            String json = sp.getString(SP_KEY_DIR_MAPPING, "{}");
            Type type = new TypeToken<HashMap<String, String>>(){}.getType();
            try {
                return gson.fromJson(json, type);
            } catch (JsonSyntaxException e) {
                return new HashMap<>();
            }
        }

        /**
         * 保存映射关系到SharedPreferences
         */
        private void saveMapping() {
            String json = gson.toJson(dirMapping);
            sp.edit().putString(SP_KEY_DIR_MAPPING, json).apply();
        }

        /**
         * 清理指定映射
         */
        public void removeMapping(String originalName) {
            dirMapping.remove(originalName);
            saveMapping();
        }

        /**
         * 清理所有映射
         */
        public void clearAllMappings() {
            dirMapping.clear();
            sp.edit().remove(SP_KEY_DIR_MAPPING).apply();
        }

    }

    public static class FileLRUCache {
        private final long maxTotalSizeBytes;
        private long currentSize = 0;
        private final LinkedHashMap<String, String> map = new LinkedHashMap<>(16, 0.75f, true);

        public FileLRUCache(long maxTotalSizeBytes) {
            this.maxTotalSizeBytes = maxTotalSizeBytes;
        }

        public synchronized void put(String url, String filePath) {
            File file = new File(filePath);
            long fileSize = file.exists() ? file.length() : 0;
            // 如果已存在，先移除旧文件
            if (map.containsKey(url)) {
                String oldPath = map.get(url);
                File oldFile = new File(oldPath);
                if (oldFile.exists()) currentSize -= oldFile.length();
                oldFile.delete();
            }
            map.put(url, filePath);
            currentSize += fileSize;
            trimToSize();

            // 打印剩余空间和当前缓存key
            long remain = maxTotalSizeBytes - currentSize;
            System.out.println("[FileLRUCache] 剩余空间: " + remain / 1024 / 1024 + "MB, 已用: " + currentSize / 1024 / 1024 + "MB, 最大: " + maxTotalSizeBytes / 1024 / 1024 + "MB");
            System.out.println("[FileLRUCache] 当前缓存URL: " + map.keySet());
        }

        public synchronized String get(String url) {
            return map.get(url);
        }

        public synchronized void remove(String url) {
            String path = map.remove(url);
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    currentSize -= file.length();
                    file.delete();
                }
            }
        }

        private void trimToSize() {
            Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
            while (currentSize > maxTotalSizeBytes && it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                String path = entry.getValue();
                File file = new File(path);
                if (file.exists()) currentSize -= file.length();
                file.delete();
                it.remove();
            }
        }
    }

    public static class PermissionChecker {

        public final static String RECORD_AUDIO  = "android.permission.RECORD_AUDIO"; //音频权限
        public final static int RECORD_AUDIO_CODE = 210;                        //音频权限请求码
        public static boolean checkPermission(Context context, String permission){
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
        }

        public static boolean checkPermissions(Context context, String[] permissions){
            for (String permission : permissions) {
                if (!checkPermission(context, permission)) return false;
            }
            return true;
        }

        //权限请求
        public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }
    }

    public static class AssetsUtils {
        private final static String TAG = "AssetsUtils";

        private AssetsUtils() {
        }

        public static void copyDirsToPath(Context context, String srcPath, String destPath) {
            Context ctx = context.getApplicationContext();
            File dir = new File(destPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try {
                String[] list = ctx.getAssets().list(srcPath);
                for (String fileName : list) {
                    copyFileToPath(ctx, srcPath + "/" + fileName, destPath + "/" + fileName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * 把assets下的某个文件copy到指定文件
         *
         * @param srcFile  原文件路径
         * @param destFile 目标文件路径
         */
        public static boolean copyFileToPath(Context context, String srcFile, String destFile) {
            Context ctx = context.getApplicationContext();
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                File dest = new File(destFile);
                // 目标文件存在且跟源文件一样,直接跳过
                if (dest.exists()) {
                } else {
                    inputStream = ctx.getAssets().open(srcFile);
                    File destDat = new File(destFile + ".dat");
                    outputStream = new FileOutputStream(destDat);
                    byte[] buffer = new byte[1024];
                    int read = inputStream.read(buffer);
                    while (read != -1) {
                        outputStream.write(buffer, 0, read);
                        read = inputStream.read(buffer);
                    }
                    outputStream.flush();
                    destDat.renameTo(dest);
                }
                return true;
            } catch (Exception e) {
                Log.w(TAG, "copyFileToPath: error " + e.getLocalizedMessage());
                return false;
            } finally {
                closeSafety(inputStream, outputStream);
            }
        }

        public static boolean copyAssetsFile(Context context, String oldPath, String newPath,
                                             boolean deleteOldFile) {
            OutputStream outputStream = null;
            InputStream inputStream = null;
            try {
                File fileNew = new File(newPath);
                File parentFile = fileNew.getParentFile();
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
                if (fileNew.exists()) {
                    if (fileNew.isFile()) {
                        if (deleteOldFile) {
                            fileNew.delete();
                        } else {
                            return true;
                        }
                    } else {
                        fileNew.delete();
                    }
                }
                inputStream = context.getAssets().open(oldPath);
                outputStream = new FileOutputStream(fileNew);
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                return true;
            } catch (Exception e) {
                return false;
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public static boolean copyAssetsFolder(Context context, String oldFolder, String newFolder,
                                               boolean deleteOldFile) {
            try {
                AssetManager assetManager = context.getAssets();
                String[] fileNames = assetManager.list(oldFolder);
                boolean ret = true;
                if (fileNames != null && fileNames.length > 0) {
                    for (String fileName : fileNames) {
                        ret = ret && copyAssetsFile(context, oldFolder + File.separator + fileName,
                                newFolder + File.separator + fileName, deleteOldFile);
                    }
                }
                return ret;
            } catch (Exception e) {
                return false;
            }
        }

        private static void closeSafety(Closeable... closeables) {
            for (Closeable closeable : closeables) {
                try {
                    if (closeable != null) {
                        closeable.close();
                    }
                } catch (IOException e) {
                    // ignore
                    e.printStackTrace();
                    Logger.etf(TAG, "close error:"+closeable.toString(),null);
                }
            }
        }
    }

    @SuppressLint("HardwareIds")
    public static String getSN(Context context) {
        String sn = "";
            //提前检查sp是否存在 存在则直接返回
             SharedPreferences sp2 = context.getSharedPreferences("device_info", Context.MODE_PRIVATE);
             sn = sp2.getString("device_sn", "");
             if (!TextUtils.isEmpty(sn)) {
                 return sn;
             }

        try {
            // Android 9.0 (API 28) 及以下版本的处理
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Android 8.0 (API 26) 及以上需要权限检查
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (ContextCompat.checkSelfPermission(context,
                            Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        sn = Build.getSerial();
                    } else {
                        // 没有权限，使用替代方案
                        sn = getAlternativeDeviceId(context);
                    }
                } else {
                    // Android 8.0以下直接使用Build.SERIAL
                    sn = Build.SERIAL;
                }
            } else {
                // Android 10 (API 29) 及以上版本
                // 普通应用无法获取真实序列号，使用替代方案
                //系统优化过
                try {
                    sn = Build.getSerial();//android 10以上需要是系统应用或系统权限才能获得（system.uid.）
                } catch (Exception e) {
                    //获取设备序列号
                    e.printStackTrace();
                }
            }

            // 检查获取的序列号是否有效
            if (sn == null || sn.isEmpty() || "unknown".equalsIgnoreCase(sn)) {
                sn = getAlternativeDeviceId(context);
            }

        } catch (SecurityException e) {
            // Android 10+可能抛出SecurityException
            Logger.etf("getySN", "SecurityException获取设备SN号: " + e.getMessage(), null);
            sn = getAlternativeDeviceId(context);
        } catch (Exception e) {
            Logger.etf("getySN", "获取设备SN号时捕捉异常: " + e.getMessage(), null);
            sn = getAlternativeDeviceId(context);
        }

        Logger.etf("getySN", "获取设备SN号: " + sn, null);
        //SN不为空则写入sp
        if (!TextUtils.isEmpty(sn)) {
            SharedPreferences sp = context.getSharedPreferences("device_info", Context.MODE_PRIVATE);
            sp.edit().putString("device_sn", sn).apply();
        }
        return sn;
    }

    /**
     * 获取替代的设备标识符
     * 适用于Android 10+或无法获取真实序列号的情况
     */
    @SuppressLint("HardwareIds")
    private static String getAlternativeDeviceId(Context context) {

        // 生成基于多个设备特征的唯一标识符
        StringBuilder deviceId = new StringBuilder();

        try {
            // 1. Android ID (在Android 8.0+对每个应用都是唯一的)
            String androidId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            if (androidId != null && !androidId.equals("9774d56d682e549c")) { // 避免模拟器默认值
                deviceId.append(androidId);
            }

            // 2. 设备型号和制造商
            deviceId.append(Build.MODEL).append(Build.MANUFACTURER);

            // 3. 设备指纹的部分信息
            deviceId.append(Build.FINGERPRINT.hashCode());

            // 4. 应用安装时间戳(首次安装时生成)
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            deviceId.append(pi.firstInstallTime);

        } catch (Exception e) {
            // 如果以上方法都失败，使用随机生成的ID
            Logger.etf("getAlternativeDeviceId", "生成替代设备ID时异常: " + e.getMessage(), null);
        }
        String finalDeviceId = hashString(deviceId.toString());

        return finalDeviceId;
    }

    /**
     * 生成字符串的SHA-256哈希值
     */
    private static String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // 如果SHA-256不可用，使用简单的hashCode
            return String.valueOf(Math.abs(input.hashCode()));
        }
    }

    /**
     * 检查是否为系统应用
     * 系统应用可以尝试获取真实的序列号
     */
    private static boolean isSystemApp(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            return (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    public interface RequestCallback {
        void onResult(boolean result,String msg);
    }

    /*
     **解析Mqtt的消息
     * @param msg
     */
    public static String getCommandIdByMqttMsg(MqttMessage msg) {
        try {
            // 将 MQTT 消息的 payload 转换为字符串
            String payload = new String(msg.getPayload());
            // 将 JSON 格式的字符串转换为 JSONObject
            JSONObject jsonObject = new JSONObject(payload);
            // 从 JSONObject 中获取 commandId 字段的值
            Log.d("oputils", "---------------------getCommandIdByMqttMsg: " + jsonObject.getString("commandId"));
            return jsonObject.getString("commandId");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("oputils", "解析Mqtt的消息时出错 " + e);
        }
        return null;
    }


}

