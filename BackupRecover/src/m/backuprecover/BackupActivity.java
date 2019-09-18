package m.backuprecover;

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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class BackupActivity extends BaseActivity {
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
				BackupAppViewHolder holder = (BackupAppViewHolder) convertView.getTag();
				final PackageInfo pi = apps.get(position);
				holder.ivIcon.setImageDrawable(getIcon(pi));
				String name = String.valueOf(pi.applicationInfo.loadLabel(getPackageManager()));
				holder.tvTitle.setText(TextUtils.isEmpty(name) ? pi.packageName : name);
				holder.tvSubTitle.setText(pi.packageName);
				holder.tvBackup.setOnClickListener(new OnClickListener() {
					public void onClick(View view) {
						backup(pi.packageName);
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
		ArrayList<PackageInfo> res = new ArrayList<PackageInfo>();
		final PackageManager pm = getPackageManager();
		List<PackageInfo> pis = pm.getInstalledPackages(0);
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
		
		String myPkg = getPackageName();
		for (PackageInfo pi : pis) {
			if (!pi.applicationInfo.packageName.equals(myPkg)) {
				if (!isSystemApp(pi)) {
					res.add(pi);
				}
			}
		}
		
		return res;
	}
	
	private boolean isSystemApp(PackageInfo pi) {
		boolean isSysApp = (pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1;
		boolean isSysUpd = (pi.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 1;
		return isSysApp || isSysUpd;
	}
	
	private Drawable getIcon(PackageInfo pi) {
		Drawable icon = icons.get(pi);
		if (icon == null) {
			icon = pi.applicationInfo.loadIcon(getPackageManager());
			icons.put(pi, icon);
		}
		return icon;
	}
	
	private void backup(final String packageName) {
		Toast.makeText(BackupActivity.this, R.string.start_backup, Toast.LENGTH_SHORT).show();
		new Thread() {
			public void run() {
				try {
					MBR.backup(BackupActivity.this, packageName);
					runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(BackupActivity.this, R.string.operation_finished, Toast.LENGTH_SHORT).show();
						}
					});
				} catch (final Throwable t) {
					runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(BackupActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
						}
					});
				}
			}
		}.start();
	}
	
	protected ViewItemHolder genItemView() {
		BackupAppViewHolder holder = new BackupAppViewHolder();
		holder.copy(super.genItemView());
		holder.llItem.setTag(holder);
		
		holder.flBackup = new FrameLayout(this);
		holder.flBackup.setBackgroundColor(0xff33a6e8);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp2px(72), dp2px(30));
		lp.rightMargin = dp2px(10);
		lp.gravity = Gravity.CENTER_VERTICAL;
		holder.llItem.addView(holder.flBackup, lp);
		
		holder.tvBackup = new TextView(this);
		holder.tvBackup.setGravity(Gravity.CENTER);
		holder.tvBackup.setTextColor(0xffffffff);
		holder.tvBackup.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
		holder.tvBackup.setText(R.string.title_backup);
		holder.tvBackup.setBackgroundColor(0);
		FrameLayout.LayoutParams lpfl = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		int dp_1 = dp2px(1);
		lpfl.setMargins(dp_1, dp_1, dp_1, dp_1);
		holder.flBackup.addView(holder.tvBackup, lpfl);
		
		return holder;
	}
	
	private class BackupAppViewHolder extends ViewItemHolder {
		public FrameLayout flBackup;
		public TextView tvBackup;
	}
}
