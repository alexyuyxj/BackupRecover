package m.backuprecover;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends BaseActivity {
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListView lv = new ListView(this);
		lv.setBackgroundColor(0xffffffff);
		lv.setCacheColorHint(0);
		lv.setDivider(new ColorDrawable(0xffe8e8e8));
		lv.setDividerHeight((int) (getResources().getDisplayMetrics().density + 0.5f));
		setContentView(lv);
		final int[] titles = {R.string.title_backup, R.string.title_recover};
		final int[] subtitles = {R.string.subtitle_backup, R.string.subtitle_recover};
		final int[] icons = {R.drawable.backup, R.drawable.restore};
		lv.setAdapter(new BaseAdapter() {
			public int getCount() {
				return titles.length;
			}
			
			public Object getItem(int position) {
				return null;
			}
			
			public long getItemId(int position) {
				return position;
			}
			
			public View getView(final int position, View convertView, ViewGroup viewGroup) {
				if (convertView == null) {
					ViewItemHolder holder = genItemView();
					convertView = holder.llItem;
				}
				ViewItemHolder holder = (ViewItemHolder) convertView.getTag();
				holder.ivIcon.setImageDrawable(getResources().getDrawable(icons[position]));
				holder.tvTitle.setText(titles[position]);
				holder.tvSubTitle.setText(subtitles[position]);
				holder.llItem.setOnClickListener(new OnClickListener() {
					public void onClick(View view) {
						Class<? extends BaseActivity> page = null;
						switch (titles[position]) {
							case R.string.title_backup: page = BackupActivity.class; break;
							case R.string.title_recover: page = RecoverActivity.class; break;
						}
						if (page != null) {
							startActivity(new Intent(view.getContext(), page));
						}
					}
				});
				return convertView;
			}
		});
		checkPermissions();
	}
	
	private void checkPermissions() {
		if (Build.VERSION.SDK_INT >= 23) {
			try {
				PackageManager pm = getPackageManager();
				PackageInfo pi = pm.getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
				ArrayList<String> list = new ArrayList<String>();
				for (String p : pi.requestedPermissions) {
					if (!checkPermission(p)) {
						list.add(p);
					}
				}
				
				if (list.size() > 0) {
					String[] permissions = list.toArray(new String[list.size()]);
					if (permissions != null) {
						requestPermissions(permissions, 1);
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	private boolean checkPermission(String permission) throws Throwable {
		int res;
		if (Build.VERSION.SDK_INT >= 23) {
			res = checkSelfPermission(permission);
		} else {
			res = getPackageManager().checkPermission(permission, getPackageName());
		}
		return res == PackageManager.PERMISSION_GRANTED;
	}
	
}
