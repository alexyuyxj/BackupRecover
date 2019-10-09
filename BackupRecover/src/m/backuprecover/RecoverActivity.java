package m.backuprecover;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class RecoverActivity extends BaseActivity {
	private ArrayList<PackageInfo> apps;
	private HashMap<PackageInfo, Drawable> icons;
	private BaseAdapter adapter;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		apps = new ArrayList<PackageInfo>();
		icons = new HashMap<PackageInfo, Drawable>();
		ListView lv = new ListView(this);
		lv.setBackgroundColor(0xffffffff);
		lv.setCacheColorHint(0);
		lv.setDivider(new ColorDrawable(0xffe8e8e8));
		lv.setDividerHeight((int) (getResources().getDisplayMetrics().density + 0.5f));
		setContentView(lv);
		adapter = new BaseAdapter() {
			public int getCount() {
				return apps == null ? 0 : apps.size();
			}
			
			public Object getItem(int position) {
				return null;
			}
			
			public long getItemId(int position) {
				return position;
			}
			
			public View getView(int position, View convertView, ViewGroup viewGroup) {
				if (convertView == null) {
					convertView = genItemView().llItem;
				}
				BackupDetailViewHolder holder = (BackupDetailViewHolder) convertView.getTag();
				final PackageInfo pi = apps.get(position);
				holder.ivIcon.setImageDrawable(getIcon(pi));
				String name = String.valueOf(pi.applicationInfo.loadLabel(getPackageManager()));
				holder.tvTitle.setText(TextUtils.isEmpty(name) ? pi.packageName : name);
				holder.tvSubTitle.setText(pi.packageName);
				holder.llItem.setOnClickListener(new OnClickListener() {
					public void onClick(View view) {
						Intent i = new Intent(RecoverActivity.this, DetailActivity.class);
						i.putExtra("package", pi.packageName);
						startActivity(i);
					}
				});
				return convertView;
			}
		};
		lv.setAdapter(adapter);
		genList();
	}
	
	private void genList() {
		new Thread() {
			public void run() {
				final ArrayList<PackageInfo> pis = fillLists();
				runOnUiThread(new Runnable() {
					public void run() {
						apps = pis;
						adapter.notifyDataSetChanged();
					}
				});
			}
		}.start();
	}
	
	private ArrayList<PackageInfo> fillLists() {
		ArrayList<PackageInfo> pis = new ArrayList<PackageInfo>();
		String[] backups = MBR.listBackups();
		final PackageManager pm = getPackageManager();
		for (String b : backups) {
			PackageInfo pi = null;
			try {
				pi = pm.getPackageInfo(b, 0);
			} catch (Throwable t) {}
			if (pi == null) {
				File dir = new File(MBR.BASE_DIR, b);
				String[] versions = dir.list();
				Arrays.sort(versions);
				File apk = null;
				for (int i = versions.length - 1; i >= 0; i--) {
					File[] apks = new File(dir, versions[i]).listFiles(new FileFilter() {
						public boolean accept(File file) {
							return file.getName().endsWith(".apk");
						}
					});
					if (apks != null && apks.length > 0) {
						apk = apks[0];
						break;
					}
				}
				if (apk != null) {
					pi = pm.getPackageArchiveInfo(apk.getPath(), 0);
					pi.applicationInfo.sourceDir = apk.getPath();
					pi.applicationInfo.publicSourceDir = apk.getPath();
				}
			}
			if (pi != null) {
				pis.add(pi);
			}
		}
		
		Collections.sort(pis, new Comparator<PackageInfo>() {
			public int compare(PackageInfo lpi, PackageInfo rpi) {
				String ln = null;
				ApplicationInfo lai = lpi.applicationInfo;
				if (lai.labelRes > 0) {
					Object label = pm.getText(lpi.packageName, lai.labelRes, lai);
					ln = label == null ? null : label.toString().trim();
				} else if (!TextUtils.isEmpty(lai.nonLocalizedLabel)) {
					ln = lai.nonLocalizedLabel.toString().trim();
				}
				String rn = null;
				ApplicationInfo rai = rpi.applicationInfo;
				if (rai.labelRes > 0) {
					Object label = pm.getText(rpi.packageName, rai.labelRes, rai);
					rn = label == null ? null : label.toString().trim();
				} else if (!TextUtils.isEmpty(rai.nonLocalizedLabel)) {
					rn = rai.nonLocalizedLabel.toString().trim();
				}
				if (TextUtils.isEmpty(ln)) {
					if (TextUtils.isEmpty(rn)) {
						ln = lpi.packageName;
						rn = rpi.packageName;
					} else {
						return 1;
					}
				} else if (TextUtils.isEmpty(rn)) {
					return -1;
				}
				return ln.compareTo(rn);
			}
		});
		
		ArrayList<PackageInfo> res = new ArrayList<PackageInfo>();
		String myPkg = getPackageName();
		for (PackageInfo pi : pis) {
			if (!pi.applicationInfo.packageName.equals(myPkg)) {
				if (new File(MBR.BASE_DIR, pi.packageName).exists()) {
					res.add(pi);
				}
			}
		}
		return res;
	}
	
	private Drawable getIcon(PackageInfo pi) {
		Drawable icon = icons.get(pi);
		if (icon == null) {
			icon = pi.applicationInfo.loadIcon(getPackageManager());
			icons.put(pi, icon);
		}
		return icon;
	}
	
	protected ViewItemHolder genItemView() {
		BackupDetailViewHolder holder = new BackupDetailViewHolder();
		holder.copy(super.genItemView());
		holder.llItem.setTag(holder);
		
		holder.flDetail = new FrameLayout(this);
		holder.flDetail.setBackgroundColor(0xff33a6e8);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp2px(72), dp2px(30));
		lp.rightMargin = dp2px(10);
		lp.gravity = Gravity.CENTER_VERTICAL;
		holder.llItem.addView(holder.flDetail, lp);
		
		holder.tvDetail = new TextView(this);
		holder.tvDetail.setGravity(Gravity.CENTER);
		holder.tvDetail.setTextColor(0xffffffff);
		holder.tvDetail.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
		holder.tvDetail.setText(R.string.detail);
		holder.tvDetail.setBackgroundColor(0);
		FrameLayout.LayoutParams lpfl = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		int dp_1 = dp2px(1);
		lpfl.setMargins(dp_1, dp_1, dp_1, dp_1);
		holder.flDetail.addView(holder.tvDetail, lpfl);
		
		return holder;
	}
	
	private class BackupDetailViewHolder extends ViewItemHolder {
		public FrameLayout flDetail;
		public TextView tvDetail;
	}
	
}
