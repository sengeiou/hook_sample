package com.hook.startservice;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author 张全
 */

public class HookUtil {
       static final String targetServiceName="com.hook.startservice.TargetService";
   static ComponentName componentName=new ComponentName("com.hook.startservice", "com.hook.startservice.TargetService");
//    static final String targetServiceName = "com.plugin.PluginService";
//    static ComponentName componentName = new ComponentName("com.plugin", "com.plugin.PluginService");

    public static void hookHandlerMessage() {
        try {
            Class<?> cls = Class.forName("android.app.ActivityThread");
            Method method = cls.getDeclaredMethod("currentActivityThread");
            Object currentActivityThread = method.invoke(null);

            Field mHField = cls.getDeclaredField("mH");
            mHField.setAccessible(true);
            Handler handler = (Handler) mHField.get(currentActivityThread);
            Class handlerClass = Class.forName("android.os.Handler");
            Field mCallbackField = handlerClass.getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);
            mCallbackField.set(handler, new MyHandlerCallback(handler));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class MyHandlerCallback implements Handler.Callback {
        private Handler handler;

        public MyHandlerCallback(Handler handler) {
            this.handler = handler;
        }

        @Override
        public boolean handleMessage(Message msg) {
            Log.e("HookUtil", "msg=" + msg);
            hookStartService(msg);
            Log.e("HookUtil", "--------------msg=" + msg);
            handler.handleMessage(msg);
            return true;
        }
    }

    private static void hook(Message msg) {

    }

    private static void hookStartService(Message msg) {
        if (msg.what == 114) {  //create Service
            try {
                //替换intent
                Class<?> cls = Class.forName("android.app.ActivityThread$CreateServiceData");
                Object serviceData = msg.obj;

                Field intentField = cls.getDeclaredField("intent");
                intentField.setAccessible(true);
                Intent intent = new Intent();
                intent.setComponent(componentName);
                intentField.set(serviceData, intent);

                try {
                    Field infoField = cls.getDeclaredField("info");
                    infoField.setAccessible(true);
                    Object info = infoField.get(serviceData);
                    Class<?> infoClass = Class.forName("android.content.pm.PackageItemInfo");
                    Field nameField = infoClass.getDeclaredField("name");
                    nameField.setAccessible(true);
                    nameField.set(info, targetServiceName);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (msg.what == 115) { //SERVICE_ARGS
            try {
                Class<?> cls = Class.forName("android.app.ActivityThread$ServiceArgsData");
                Object serviceData = msg.obj;
                Field argsField = cls.getDeclaredField("args");
                argsField.setAccessible(true);
                Object oldIntent = argsField.get(serviceData); //Intent

                replaceIntent(oldIntent, componentName);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (msg.what == 121) { //BIND_SERVICE
            try {
                Class<?> cls = Class.forName("android.app.ActivityThread$BindServiceData");
                Object serviceData = msg.obj;
                Field argsField = cls.getDeclaredField("intent");
                argsField.setAccessible(true);
                Object oldIntent = argsField.get(serviceData); //Intent

                replaceIntent(oldIntent, componentName);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static void replaceIntent(Object oldIntent, ComponentName componentName) throws Exception {
        Class<?> intentClass = Class.forName("android.content.Intent");
        Field mComponentField = intentClass.getDeclaredField("mComponent");
        mComponentField.setAccessible(true);
        mComponentField.set(oldIntent, componentName);
    }
}

