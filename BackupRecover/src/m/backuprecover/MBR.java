package m.backuprecover;

import android.content.Context;

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
	
	public static void backup(Context context, String packageName) throws Throwable {
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
	
	public static void recover(String packageName, String copy) throws Throwable {
		Process p = Runtime.getRuntime().exec("su");
		OutputStream output = p.getOutputStream();
		command("adb install -r -d " + packageName, output);
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
		
		File backupFolder = new File(BASE_DIR + packageName, copy);
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
	}
	
}
