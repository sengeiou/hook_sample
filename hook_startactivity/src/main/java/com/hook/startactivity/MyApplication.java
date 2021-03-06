package com.hook.startactivity;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {

    List<String> dexPathList;

    {
        dexPathList = new ArrayList<>();
        dexPathList.add(new File(Environment.getExternalStorageDirectory(), "plugin-debug.apk").getAbsolutePath());
        dexPathList.add(new File(Environment.getExternalStorageDirectory(), "plugin2-debug.apk").getAbsolutePath());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            loadPluginResources();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ClassLoader classLoader = getClassLoader();
        while(classLoader!=null){
            System.out.println(classLoader);
            classLoader=classLoader.getParent();
        }

        //这个ProxyActivity在清单文件中注册过，以后所有的Activitiy都可以用ProxyActivity无需声明，绕过监测
        HookUtil hookUtil=new HookUtil(ProxyActivity.class, this);
        hookUtil.hookAms();
        hookUtil.hookSystemHandler();
    }

    @Override
    protected void attachBaseContext(Context base) {
        setUpClassLoader(base);
        super.attachBaseContext(base);
    }

    public void setUpClassLoader(Context context){
        ClassLoader classLoader =MyApplication.class.getClassLoader();
        String nativeLibraryPath=null;
        try
        {
            nativeLibraryPath = (String)classLoader.getClass().getMethod("getLdLibraryPath", new Class[0]).invoke(classLoader, new Object[0]);
        }
        catch (Throwable t)
        {
            Log.e("InstantRun", "Failed to determine native library path " + t.getMessage());
            nativeLibraryPath= new File("/data/data/"+getPackageName(), "lib").getPath();
        }

        String codeCacheDir=context.getCacheDir().getPath();
        String dexPath=new File(Environment.getExternalStorageDirectory(),"plugin-debug.apk").getAbsolutePath();
        MyIncrementalClassLoader.inject(classLoader, nativeLibraryPath, codeCacheDir, dexPath);
    }

    //--------------------------------------------------------------------------------
    private Resources.Theme mTheme;
    private Resources mResources;

    @Override
    public AssetManager getAssets() {
        return mResources == null ? super.getAssets() : mResources.getAssets();
    }

    @Override
    public Resources getResources() {
        return mResources == null ? super.getResources() : mResources;
    }

    @Override
    public Resources.Theme getTheme() {
        return mTheme == null ? super.getTheme() : mTheme;
    }


    /**
     * 加载插件资源
     */
    public void loadPluginResources() throws Exception {
        try {

            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, dexPathList.get(0));

            Resources superRes = super.getResources();
            mResources = new Resources(assetManager, superRes.getDisplayMetrics(), superRes.getConfiguration());
            mTheme = mResources.newTheme();
            mTheme.setTo(super.getTheme());

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("HookUtil", "##loadPluginResources出错");
            throw e;
        }
    }
}