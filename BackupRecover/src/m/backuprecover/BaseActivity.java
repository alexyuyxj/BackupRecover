package m.backuprecover;

import android.app.Activity;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class BaseActivity extends Activity {
	
	protected int dp2px(int dp) {
		float density = getResources().getDisplayMetrics().density;
		return (int) (dp * density + 0.5f);
	}
	
	protected ViewItemHolder genItemView() {
		ViewItemHolder holder = new ViewItemHolder();
		holder.llItem = new LinearLayout(this);
		int dp_5 = dp2px(5);
		holder.llItem.setPadding(0, dp_5, 0, dp_5);
		holder.llItem.setTag(holder);
		
		holder.ivIcon = new ImageView(this);
		holder.ivIcon.setScaleType(ScaleType.FIT_CENTER);
		int dp_48 = dp2px(48);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp_48, dp_48);
		lp.gravity = Gravity.CENTER_VERTICAL;
		int dp_10 = dp2px(10);
		lp.leftMargin = dp_10;
		holder.llItem.addView(holder.ivIcon, lp);
		
		LinearLayout llInfo = new LinearLayout(this);
		llInfo.setOrientation(LinearLayout.VERTICAL);
		lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER_VERTICAL;
		lp.leftMargin = lp.rightMargin = dp_10;
		lp.weight = 1;
		holder.llItem.addView(llInfo, lp);
		
		holder.tvTitle = new TextView(this);
		holder.tvTitle.setSingleLine();
		holder.tvTitle.setTextColor(0xff353535);
		holder.tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		llInfo.addView(holder.tvTitle, lp);
		
		holder.tvSubTitle = new TextView(this);
		holder.tvSubTitle.setSingleLine();
		holder.tvSubTitle.setTextColor(0xffafafaf);
		holder.tvSubTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
		lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		llInfo.addView(holder.tvSubTitle, lp);
		
		return holder;
	}
	
	protected class ViewItemHolder {
		public LinearLayout llItem;
		public ImageView ivIcon;
		public TextView tvTitle;
		public TextView tvSubTitle;
		
		protected void copy(ViewItemHolder dst) {
			llItem = dst.llItem;
			ivIcon = dst.ivIcon;
			tvTitle = dst.tvTitle;
			tvSubTitle = dst.tvSubTitle;
		}
	}
	
}
