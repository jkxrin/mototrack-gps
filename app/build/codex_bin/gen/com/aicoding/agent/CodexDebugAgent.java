package com.aicoding.agent;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class CodexDebugAgent {
    private static final String TAG = "CodexDebugAgent";
    private static final String ACTION_DEBUG_LOG = "com.aicoding.ide.DEBUG_LOG";
    private static final String IDE_PACKAGE = "com.aicoding.ide";
    private static boolean installed = false;

    public static synchronized void install(final Context context) {
        if (installed) return;
        installed = true;

        final Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        final String pkgName = appContext.getPackageName();

        // 1. Notifica a IDE que o app iniciou
        sendLog(appContext, pkgName, "LIFECYCLE", "App inicializado com sucesso (PID: " + android.os.Process.myPid() + ")", null);

        // 2. Intercepta exceções não tratadas (crashes)
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    throwable.printStackTrace(pw);
                    String stackTrace = sw.toString();

                    String crashMsg = throwable.getClass().getName() + ": " + throwable.getMessage();
                    Log.e(TAG, "CRASH DETECTADO NO APP: " + crashMsg + "\n" + stackTrace);

                    // Transmite broadcast direto para o CODEX-IDE
                    sendLog(appContext, pkgName, "CRASH", crashMsg, stackTrace);

                    // Salva arquivo local para redundância
                    saveCrashToFile(appContext, pkgName, crashMsg, stackTrace);

                    // Pausa breve para o Binder do Android enviar o broadcast antes de encerrar
                    Thread.sleep(350);
                } catch (Throwable ignored) {
                } finally {
                    if (defaultHandler != null) {
                        defaultHandler.uncaughtException(thread, throwable);
                    } else {
                        System.exit(1);
                    }
                }
            }
        });

        // 3. Captura logs de erro e warning do próprio processo em segundo plano
        startLogcatStreamer(appContext, pkgName);
    }

    private static void sendLog(Context context, String pkgName, String type, String message, String stackTrace) {
        try {
            Intent intent = new Intent(ACTION_DEBUG_LOG);
            intent.setPackage(IDE_PACKAGE);
            intent.putExtra("package_name", pkgName);
            intent.putExtra("type", type);
            intent.putExtra("message", message != null ? message : "");
            intent.putExtra("stack_trace", stackTrace != null ? stackTrace : "");
            intent.putExtra("timestamp", System.currentTimeMillis());
            intent.putExtra("pid", android.os.Process.myPid());
            context.sendBroadcast(intent);
        } catch (Throwable e) {
            Log.w(TAG, "Falha ao enviar broadcast para a IDE: " + e.getMessage());
        }
    }

    private static void saveCrashToFile(Context context, String pkgName, String message, String stackTrace) {
        try {
            File dir = context.getExternalFilesDir(null);
            if (dir == null) dir = context.getFilesDir();
            File crashFile = new File(dir, "last_crash.txt");
            FileOutputStream fos = new FileOutputStream(crashFile);
            String content = "PACKAGE: " + pkgName + "\nTIMESTAMP: " + System.currentTimeMillis() + "\nMESSAGE: " + message + "\n\nSTACKTRACE:\n" + stackTrace;
            fos.write(content.getBytes("UTF-8"));
            fos.flush();
            fos.close();
        } catch (Throwable ignored) {}
    }

    private static void startLogcatStreamer(final Context context, final String pkgName) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int myPid = android.os.Process.myPid();
                    String pidStr = String.valueOf(myPid);
                    Process process = new ProcessBuilder("logcat", "-v", "time").start();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(pidStr)) {
                            if (line.contains(" E ") || line.contains(" W ") || line.contains("System.err") || line.contains("AndroidRuntime")) {
                                sendLog(context, pkgName, "LOG", line, null);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        });
        thread.setDaemon(true);
        thread.setName("CodexLogcatStreamer");
        thread.start();
    }
}