package com.smile.edittagsview;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * TODO 基于Edittext的标签输入控件，输入“，”、“,”或者回车确认添加标签
 * 
 * @author Smile<lijianhy1990@gmail.com>
 * @date 2015年5月16日
 */
public class EditTagsView extends ViewGroup implements OnEditorActionListener,
		TextWatcher, OnKeyListener {
	private static final int DEFAULT_HORIZONTAL_SPACING = 5;
	private static final int DEFAULT_VERTICAL_SPACING = 5;
	private static final int DEFAULT_MAX_COUNT = 10;
	private static final int DEFAULT_MAX_LENGH = 10;
	// 行间距
	private int mVerticalSpacing;
	// 列间距
	private int mHorizontalSpacing;
	// tag最大容量
	private int mMaxCount;
	// tag最大长度
	private int mMaxLength;
	// 内置输入框，用于接收输入
	private EditText mEdtView;
	// 确定的tag数量
	private List<String> mTags;
	// tag 背景
	private Drawable tagBg;

	private onTagAddListener mListener;
	private int dp10;

	public EditTagsView(Context context) {
		super(context);
		initView(context);
	}

	public EditTagsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.EditTags);
		try {
			mMaxCount = a.getInteger(R.styleable.EditTags_tag_max_count,
					DEFAULT_MAX_COUNT);
			mMaxLength = a.getInteger(R.styleable.EditTags_tag_max_length,
					DEFAULT_MAX_LENGH);
			mHorizontalSpacing = a.getDimensionPixelSize(
					R.styleable.EditTags_horizontal_spacing,
					DEFAULT_HORIZONTAL_SPACING);
			mVerticalSpacing = a.getDimensionPixelSize(
					R.styleable.EditTags_vertical_spacing,
					DEFAULT_VERTICAL_SPACING);
			tagBg = a.getDrawable(R.styleable.EditTags_tag_background);
		} finally {
			a.recycle();
		}
		initView(context);
	}

	public void setHorizontalSpacing(int pixelSize) {
		mHorizontalSpacing = pixelSize;
	}

	public void setVerticalSpacing(int pixelSize) {
		mVerticalSpacing = pixelSize;
	}

	public void setTagListener(onTagAddListener listener) {
		this.mListener = listener;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int myWidth = resolveSize(0, widthMeasureSpec);

		int paddingLeft = getPaddingLeft();
		int paddingTop = getPaddingTop();
		int paddingRight = getPaddingRight();
		int paddingBottom = getPaddingBottom();

		int childLeft = paddingLeft;
		int childTop = paddingTop;

		int lineHeight = 0;

		// Measure each child and put the child to the right of previous child
		// if there's enough room for it, otherwise, wrap the line and put the
		// child to next line.
		for (int i = 0, childCount = getChildCount(); i < childCount; ++i) {
			View childView = getChildAt(i);
			LayoutParams childLayoutParams = childView.getLayoutParams();
			childView.measure(
					getChildMeasureSpec(widthMeasureSpec, paddingLeft
							+ paddingRight, childLayoutParams.width),
					getChildMeasureSpec(heightMeasureSpec, paddingTop
							+ paddingBottom, childLayoutParams.height));
			int childWidth = childView.getMeasuredWidth();
			int childHeight = childView.getMeasuredHeight();

			lineHeight = Math.max(childHeight, lineHeight);

			if (childLeft + childWidth + paddingRight > myWidth) {
				childLeft = paddingLeft;
				childTop += mVerticalSpacing + lineHeight;
				lineHeight = childHeight;
			} else {
				childLeft += childWidth + mHorizontalSpacing;
			}
		}

		int wantedHeight = childTop + lineHeight + paddingBottom;
		setMeasuredDimension(myWidth,
				resolveSize(wantedHeight, heightMeasureSpec));
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int myWidth = r - l;

		int paddingLeft = getPaddingLeft();
		int paddingTop = getPaddingTop();
		int paddingRight = getPaddingRight();

		int childLeft = paddingLeft;
		int childTop = paddingTop;

		int lineHeight = 0;

		for (int i = 0, childCount = getChildCount(); i < childCount; ++i) {
			View childView = getChildAt(i);

			if (childView.getVisibility() == View.GONE) {
				continue;
			}

			int childWidth = childView.getMeasuredWidth();
			int childHeight = childView.getMeasuredHeight();

			lineHeight = Math.max(childHeight, lineHeight);

			if (childLeft + childWidth + paddingRight > myWidth) {
				childLeft = paddingLeft;
				childTop += mVerticalSpacing + lineHeight;
				lineHeight = childHeight;
			}

			childView.layout(childLeft, childTop, childLeft + childWidth,
					childTop + childHeight);
			childLeft += childWidth + mHorizontalSpacing;
		}
	}

	private void initView(Context context) {
		float scale = context.getResources().getDisplayMetrics().density;
		dp10 = (int) (10 * scale + 0.5f);
		mTags = new ArrayList<String>();
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		mEdtView = new EditText(getContext());
		mEdtView.setLayoutParams(lp);
		mEdtView.setMinWidth(dp10 * 8);
		mEdtView.setBackgroundColor(Color.argb(0, 0, 0, 0));
		mEdtView.setImeOptions(EditorInfo.IME_ACTION_DONE);
		mEdtView.setSingleLine(true);
		mEdtView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
		mEdtView.setTextColor(Color.BLACK);
		mEdtView.setPadding(dp10 / 2, dp10 / 2, dp10 / 2, dp10 / 2);
		mEdtView.setOnEditorActionListener(this);
		mEdtView.addTextChangedListener(this);
		mEdtView.setOnKeyListener(this);
		if (mMaxLength > 0) {
			InputFilter[] filters = { new LengthFilter(mMaxLength + 1) };
			mEdtView.setFilters(filters);
		}
		addView(mEdtView);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			mEdtView.requestFocus();
			InputMethodManager imm = (InputMethodManager) getContext()
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
		}
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_DONE
				|| (event != null && event.getAction() == KeyEvent.ACTION_MULTIPLE)) {
			addTag(v.getText());
			return true;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	private void addTag(CharSequence text) {
		if (text == null) {
			if (mListener != null) {
				mListener.onTagError(onTagAddListener.Type.STR_EMPTY);
			}
			return;
		}
		if (TextUtils.isEmpty(text.toString().trim())) {
			if (mListener != null) {
				mListener.onTagError(onTagAddListener.Type.STR_EMPTY);
			}
			return;
		}
		if (mTags.size() >= mMaxCount) {
			if (mListener != null) {
				mListener.onTagError(onTagAddListener.Type.COUNT_MAX);
			}
			return;
		}
		if (mListener != null) {
			mListener.onTagAdd(text.toString());
		}
		mTags.add(text.toString());
		TextView tagText = new TextView(getContext());
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		tagText.setLayoutParams(lp);
		tagText.setText(text);
		if (tagBg == null) {
			tagText.setBackgroundColor(Color.BLACK);
		} else {
			Drawable nBg = tagBg.getConstantState().newDrawable();
			tagText.setBackgroundDrawable(nBg);
		}
		tagText.setTextColor(Color.WHITE);
		tagText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
		tagText.setPadding(dp10, dp10 / 2, dp10, dp10 / 2);
		addView(tagText, getChildCount() - 1);
		mEdtView.setText("");
	}

	/**
	 * get tags list
	 * 
	 * @return
	 */
	public List<String> getTags() {
		return mTags;
	}
	
	/**
	 * get edtview
	 * 
	 * @return
	 */
	public EditText getEdtText(){
		return mEdtView;
	}

	/**
	 * init tags(reset tags list)
	 * 
	 * @param tags
	 */
	public void setTags(List<String> tags) {
		while (mTags.size() > 0) {
			deleteLastTag();
		}
		for (String item : tags) {
			addTag(item);
		}
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DEL
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			return deleteLastTag();
		}
		return false;
	}

	private boolean deleteLastTag() {
		if (mEdtView.getText().length() > 0) {
			return false;
		}
		if (mTags.isEmpty()) {
			return false;
		}
		mTags.remove(mTags.size() - 1);
		removeViewAt(getChildCount() - 2);
		return true;
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	@Override
	public void afterTextChanged(Editable s) {
		int index = s.toString().indexOf(",");
		String text = mEdtView.getText().toString();
		if (index >= 0) {
			text = text.substring(0, text.length() - 1);
			addTag(text);
			return;
		}
		index = s.toString().indexOf("，");
		if (index >= 0) {
			text = text.substring(0, text.length() - 1);
			addTag(text);
			return;
		}
		if (s.length() >= mMaxLength) {
			s.delete(mMaxLength - 1, s.length());
			return;
		}
		edtTag(text);
		return;
	}

	private void edtTag(String string) {
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	public interface onTagAddListener {
		public enum Type {
			STR_EMPTY, COUNT_MAX
		}

		void onTagAdd(String tag);

		void onTagError(Type errorType);
	}

}
