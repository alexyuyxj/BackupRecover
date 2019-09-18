package m.backuprecover;

import android.content.pm.PackageInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class DetailActivity extends BaseActivity {
	private ArrayList<File> files;
	private HashMap<File, Long> backups;
	private BaseAdapter adapter;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		files = new ArrayList<File>();
		backups = new HashMap<File, Long>();
		
		final String packageName = getIntent().getStringExtra("package");
		String name = null;
		Drawable icon = null;
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(packageName, 0);
			name = String.valueOf(pi.applicationInfo.loadLabel(getPackageManager()));
			icon = pi.applicationInfo.loadIcon(getPackageManager());
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (name == null) {
			name = getString(R.string.title_recover);
		}
		setTitle(name);
		if (icon == null) {
			icon = getResources().getDrawable(R.drawable.restore);
		}
		getActionBar().setIcon(icon);
		
		ListView lv = new ListView(this);
		lv.setBackgroundColor(0xffffffff);
		lv.setCacheColorHint(0);
		lv.setDivider(new ColorDrawable(0xffe8e8e8));
		lv.setDividerHeight((int) (getResources().getDisplayMetrics().density + 0.5f));
		setContentView(lv);
		adapter = new BaseAdapter() {
			public int getCount() {
				return files == null ? 0 : files.size();
			}
			
			public Object getItem(int position) {
				return null;
			}
			
			public long getItemId(int position) {
				return position;
			}
			
			public View getView(final int position, View convertView, ViewGroup viewGroup) {
				if (convertView == null) {
					convertView = genItemView().llItem;
				}
				RecoverViewHolder holder = (RecoverViewHolder) convertView.getTag();
				holder.ivIcon.setImageResource(R.drawable.history);
				holder.tvTitle.setText(toDate(files.get(position).getName()));
				holder.tvSubTitle.setText(toSize(backups.get(files.get(position))));
				holder.tvRecover.setOnClickListener(new OnClickListener() {
					public void onClick(View view) {
						recover(packageName, files.get(position).getName());
					}
				});
				return convertView;
			}
		};
		lv.setAdapter(adapter);
		genList(packageName);
	}
	
	private void genList(final String packageName) {
		new Thread() {
			public void run() {
				final ArrayList<File> list = new ArrayList<File>();
				final HashMap<File, Long> map = new HashMap<File, Long>();
				File backupDir = new File(MBR.BASE_DIR, packageName);
				for (File file : backupDir.listFiles()) {
					list.add(file);
					long size = 0;
					for (File f : file.listFiles()) {
						size += f.length();
					}
					map.put(file, size);
				}
				runOnUiThread(new Runnable() {
					public void run() {
						files.clear();
						files.addAll(list);
						backups.clear();
						backups.putAll(map);
						adapter.notifyDataSetChanged();
					}
				});
			}
		}.start();
	}
	
	private String toDate(String name) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss");
			Date d = sdf.parse(name);
			sdf = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
			return sdf.format(d);
		} catch (Throwable t) {
			t.printStackTrace();
			return name;
		}
	}
	
	private String toSize(long size) {
		if (size < 1000) {
			return "1 KB";
		} else {
			size /= 1000;
			if (size < 1000) {
				return size + " KB";
			} else {
				size /= 1000;
				return size + " MB";
			}
		}
	}
	
	private void recover(final String packageName, final String backup) {
		Toast.makeText(this, R.string.start_recover, Toast.LENGTH_SHORT).show();
		new Thread() {
			public void run() {
				try {
					MBR.recover(packageName, backup);
					runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(DetailActivity.this, R.string.operation_finished, Toast.LENGTH_SHORT).show();
						}
					});
				} catch (final Throwable t) {
					runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(DetailActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
						}
					});
				}
			}
		}.start();
	}
	
	protected ViewItemHolder genItemView() {
		RecoverViewHolder holder = new RecoverViewHolder();
		holder.copy(super.genItemView());
		holder.llItem.setTag(holder);
		
		holder.flRecover = new FrameLayout(this);
		holder.flRecover.setBackgroundColor(0xff33a6e8);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp2px(72), dp2px(30));
		lp.rightMargin = dp2px(10);
		lp.gravity = Gravity.CENTER_VERTICAL;
		holder.llItem.addView(holder.flRecover, lp);
		
		holder.tvRecover = new TextView(this);
		holder.tvRecover.setGravity(Gravity.CENTER);
		holder.tvRecover.setTextColor(0xffffffff);
		holder.tvRecover.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
		holder.tvRecover.setText(R.string.title_recover);
		holder.tvRecover.setBackgroundColor(0);
		FrameLayout.LayoutParams lpfl = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		int dp_1 = dp2px(1);
		lpfl.setMargins(dp_1, dp_1, dp_1, dp_1);
		holder.flRecover.addView(holder.tvRecover, lpfl);
		
		return holder;
	}
	
	private class RecoverViewHolder extends ViewItemHolder {
		public FrameLayout flRecover;
		public TextView tvRecover;
	}
	
}
