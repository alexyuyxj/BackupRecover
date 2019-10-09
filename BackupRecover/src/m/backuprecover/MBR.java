package m.backuprecover;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MBR {
	private static final String DATA_P = "/data/data/";
	private static final String DATA_S = "/sdcard/Android/data/";
	public static final String BASE_DIR = "/sdcard/MBR/";
	
	public static void backup(final Context context, final String[] packageNames, final Callback result) {
		new Thread() {
			public void run() {
				Handler handler = new Handler(Looper.getMainLooper(), result);
				Message msg = new Message();
				try {
					for (String packageName : packageNames) {
						backup(context, packageName);
					}
				} catch (Throwable t) {
					msg.obj = t;
				}
				handler.sendMessage(msg);
			}
		}.start();
	}
	
	private static void backup(final Context context, final String packageName) throws Throwable {
		String time = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date());
		File backupFolder = new File(BASE_DIR + packageName, time);
		backupFolder.mkdirs();
		String tpd = new File(backupFolder, packageName + ".tpd").getAbsolutePath();
		String tsd = new File(backupFolder, packageName + ".tsd").getAbsolutePath();
		String apk = new File(backupFolder, packageName + ".apk").getAbsolutePath();
		
		Process p = Runtime.getRuntime().exec("su");
		OutputStream output = p.getOutputStream();
		command("am force-stop " + packageName, output);
		command("tar -cf " + tpd + " " + DATA_P + packageName, output);
		command("tar -cf " + tsd + " " + DATA_S + packageName, output);
		command("exit", output);
		p.waitFor();
		p.destroy();
		
		String baseAPK = context.getPackageManager().getApplicationInfo(packageName, 0).sourceDir;
		FileInputStream fis = new FileInputStream(baseAPK);
		FileOutputStream fos = new FileOutputStream(apk);
		fis.getChannel().transferTo(0, fis.getChannel().size(), fos.getChannel());
		fos.flush();
		fos.close();
		fis.close();
	}
	
	private static void command(String command, OutputStream os) throws Throwable {
		os.write((command + "\n").getBytes("utf-8"));
		os.flush();
	}
	
	public static void recover(final Context context, final String packageName, final String copy, final Callback result) {
		new Thread() {
			public void run() {
				final Handler handler = new Handler(Looper.getMainLooper(), result);
				final Message msg = new Message();
				final File backupFolder = new File(BASE_DIR + packageName, copy);
				String apk = new File(backupFolder, packageName + ".apk").getAbsolutePath();
				IntentFilter filter = new IntentFilter();
				filter.addAction(Intent.ACTION_PACKAGE_ADDED);
				filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
				filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
				filter.addDataScheme("package");
				BroadcastReceiver b = new BroadcastReceiver() {
					public void onReceive(Context context, Intent intent) {
						context.getApplicationContext().unregisterReceiver(this);
						new Thread() {
							public void run() {
								try {
									Process p = Runtime.getRuntime().exec("su");
									OutputStream output = p.getOutputStream();
									command("ls -lad " + DATA_P + packageName, output);
									InputStream is = p.getInputStream();
									BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
									String line = br.readLine();
									command("exit", output);
									p.waitFor();
									p.destroy();
									String uid = null;
									for (String part : line.split(" ")) {
										if (part.startsWith("u0_a")) {
											uid = "10" + part.substring(4);
											break;
										}
									}
									if (uid == null) {
										throw new Throwable("uid not found");
									}
									
									String tpd = new File(backupFolder, packageName + ".tpd").getAbsolutePath();
									String tsd = new File(backupFolder, packageName + ".tsd").getAbsolutePath();
									p = Runtime.getRuntime().exec("su");
									output = p.getOutputStream();
									command("mv " + DATA_P + packageName + " " + DATA_P + "." + packageName, output);
									command("mv " + DATA_S + packageName + " " + DATA_S + "." + packageName, output);
									command("tar -xf " + tpd + " -C /", output);
									command("tar -xf " + tsd + " -C /", output);
									command("chown -R media_rw:media_rw " + DATA_S + packageName, output);
									command("chown -hR " + uid + ":" + uid + " " + DATA_P + packageName, output);
									command("chmod -R u+rwx " + DATA_P + packageName, output);
									command("restorecon -R " + DATA_P + packageName, output);
									command("exit", output);
									p.waitFor();
									p.destroy();
									
									p = Runtime.getRuntime().exec("su");
									output = p.getOutputStream();
									command("rm -r " + DATA_P + "." + packageName, output);
									command("rm -r " + DATA_S + "." + packageName, output);
									command("exit", output);
									p.waitFor();
									p.destroy();
								} catch (Throwable t) {
									msg.obj = t;
								}
								handler.sendMessage(msg);
							}
						}.start();
					}
				};
				context.getApplicationContext().registerReceiver(b, filter);
				try {
					Process p = Runtime.getRuntime().exec("su");
					OutputStream output = p.getOutputStream();
					try {
						context.getPackageManager().getPackageInfo(packageName, 0);
						command("pm install -r -d " + apk, output);
					} catch (Throwable t) {
						command("pm install " + apk, output);
					}
					command("exit", output);
					p.waitFor();
					p.destroy();
				} catch (Throwable t) {
					context.getApplicationContext().unregisterReceiver(b);
					msg.obj = t;
					handler.sendMessage(msg);
				}
			}
		}.start();
	}
	
	public static String[] listBackups() {
		return new File(BASE_DIR).list();
	}
	
}
